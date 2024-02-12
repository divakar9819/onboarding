package com.onboarding.onboarding.serviceImpl;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;
import com.onboarding.onboarding.entity.KycData;
import com.onboarding.onboarding.entity.Wallet;
import com.onboarding.onboarding.entity.WalletAccount;
import com.onboarding.onboarding.exception.*;
import com.onboarding.onboarding.payload.request.KycDataRequest;
import com.onboarding.onboarding.payload.response.ApiResponse;
import com.onboarding.onboarding.payload.response.UserResponse;
import com.onboarding.onboarding.payload.response.ValidTokenResponse;
import com.onboarding.onboarding.payload.response.WalletResponse;
import com.onboarding.onboarding.repository.KycDataRepository;
import com.onboarding.onboarding.repository.WalletAccountRepository;
import com.onboarding.onboarding.repository.WalletRepository;
import com.onboarding.onboarding.service.WalletService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolation;
import javax.xml.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;

import static com.onboarding.onboarding.interceptor.AuthenticationInterceptor.getUsername;
import static com.onboarding.onboarding.utils.Util.getRandomWalletId;

/**
 * @author Divakar Verma
 * @created_at : 30/01/2024 - 6:18 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
@Service
public class WalletServiceImpl implements WalletService {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private KycDataRepository kycDataRepository;

    @Autowired
    private WalletAccountRepository walletAccountRepository;

    @Autowired
    @Qualifier("getAuthWebClient")
    private WebClient webClient;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public Mono<WalletResponse> createWallet() {
        Wallet wallet = new Wallet();
        wallet.setWalletId(getRandomWalletId());
        wallet.setState("kyc");
        wallet.setStatus("pending");
        wallet.setKycStatus("pending");
        wallet.setUsername(getUsername());
       return  walletRepository.save(wallet)
               .flatMap(savedWallet -> walletToWalletResponse(Mono.just(savedWallet)))
               .onErrorResume(throwable -> {
                   if (throwable instanceof DuplicateKeyException || throwable instanceof MongoWriteException) {
                       return Mono.error(new ValidationErrorException(throwable.getMessage()));
                   } else {
                       return Mono.error(throwable);
                   }
               });
    }

    @Override
    public Mono<WalletResponse> getWalletByUsername(String username) {
        return walletRepository.findByUsername(username)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User wallet not found")))
                .map(wallet -> modelMapper.map(wallet, WalletResponse.class));
    }

    @Override
    public Mono<ApiResponse> doKyc(KycDataRequest kycDataRequest) {
        String username = getUsername();
        WalletResponse walletResponse = getWalletByUsername(username).block();
        if(walletResponse.getKycStatus().equalsIgnoreCase("pending")){
            KycData kycData = new KycData();
            kycData.setWalletId(walletResponse.getWalletId());
            kycData.setName(kycDataRequest.getName());
            kycData.setAddress(kycDataRequest.getAddress());
            kycDataRepository.save(kycData)
                    .onErrorResume(throwable -> {
                        if (throwable instanceof DuplicateKeyException || throwable instanceof MongoWriteException) {
                            return Mono.error(new ValidationErrorException(throwable.getMessage()));
                        } else {
                            return Mono.error(throwable);
                        }
                    })
                    .subscribe();
            Wallet wallet = walletResponseToWallet(walletResponse);
            wallet.setKycStatus("completed");
            wallet.setState("vpa");
            walletRepository.save(wallet)
                    .onErrorResume(throwable -> {
                        if (throwable instanceof DuplicateKeyException || throwable instanceof MongoWriteException) {
                            return Mono.error(new ValidationErrorException(throwable.getMessage()));
                        } else {
                            return Mono.error(throwable);
                        }
                    })
                    .subscribe();

            ApiResponse apiResponse = new ApiResponse("Kyc done successfully",true);
            return Mono.just(apiResponse);
        }
        if(walletResponse.getKycStatus().equalsIgnoreCase("completed")){
            ApiResponse apiResponse = new ApiResponse("User kyc already done",true);
            return Mono.just(apiResponse);
        }
        else {
            throw new GlobalException("Global exception");
        }
    }

    @Override
    public Mono<ValidTokenResponse> validateToken(String token) {
        return webClient.post().uri("/validateToken")
                .body(Mono.just(token),String.class)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                    Mono.error(new InvalidTokenException("Invalid token"))
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                    Mono.error(new ServerErrorException("Server error"))
                )
                .onStatus(HttpStatusCode :: isError, clientResponse -> {
                    if(clientResponse.statusCode()== HttpStatus.SERVICE_UNAVAILABLE){
                        return Mono.error(new ConnectionRefusedException("Connection refused"));
                    }
                    else {
                        return  clientResponse.createException();
                    }
                })
                .bodyToMono(ValidTokenResponse.class);

    }

    public Mono<UserResponse> getUserByUsername(String username){
        return webClient.get().uri("/getUserByUsername/"+username)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,clientResponse ->
                        Mono.error(new ResourceNotFoundException("User not found "))
                )
                .onStatus(HttpStatusCode::is5xxServerError,clientResponse ->
                        Mono.error(new ServerErrorException("Server error"))
                )
                .onStatus(HttpStatusCode :: isError, clientResponse -> {
                    if(clientResponse.statusCode()== HttpStatus.SERVICE_UNAVAILABLE){
                        return Mono.error(new ConnectionRefusedException("Connection refused"));
                    }
                    else {
                        return  clientResponse.createException();
                    }
                })
                .bodyToMono(UserResponse.class);
    }

    @Override
    public Mono<WalletResponse> createVpa() {
        String username = getUsername();
        Mono<UserResponse> userResponse = getUserByUsername(username)
                .onErrorResume(throwable -> {
                    if (throwable instanceof ResourceNotFoundException) {
                        return Mono.error(throwable);
                    } else if (throwable instanceof ServerErrorException || throwable instanceof ConnectionRefusedException) {
                        return Mono.error(throwable);
                    } else {
                        return Mono.error(new GlobalException("Global exception"));
                    }
                });

        UserResponse user = new UserResponse();
        userResponse
                .flatMap(user1 -> {
                    System.out.println(user1);
                    user.setEmail(user1.getEmail());
                    user.setMobileNumber(user1.getMobileNumber());
                    return Mono.just(user1);
                })
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User not found")))
                .onErrorResume(throwable -> {
                    if (throwable instanceof ResourceNotFoundException) {
                        return Mono.error(throwable);
                    } else if (throwable instanceof ServerErrorException || throwable instanceof ConnectionRefusedException) {
                        return Mono.error(throwable);
                    } else {
                        return Mono.error(new GlobalException("Global exception"));
                    }
                }).subscribe();

        Mono<WalletResponse> walletResponse = getWalletByUsername(username);
        return walletResponse
                .flatMap(walletResponse1 -> {
                    if(walletResponse1.getState().equalsIgnoreCase("vpa")){
                        Wallet wallet = walletResponseToWallet(walletResponse1);
                        wallet.setVpa(user.getMobileNumber()+"@mypsp");
                        wallet.setState("active");
                        wallet.setStatus("completed");
                        Mono<Wallet> createdWallet =  walletRepository.save(wallet)
                                .onErrorResume(throwable -> {
                                    if (throwable instanceof DuplicateKeyException || throwable instanceof MongoWriteException) {
                                        return Mono.error(new ValidationErrorException(throwable.getMessage()));
                                    } else {
                                        return Mono.error(throwable);
                                    }});
                        WalletAccount walletAccount = new WalletAccount();
                        Mono<KycData> kycData = getKycData(walletResponse1.getWalletId())
                                .onErrorResume(throwable -> {
                                    if(throwable instanceof ResourceNotFoundException){
                                        return Mono.error(throwable);
                                    }
                                    else {
                                        return Mono.error(throwable);
                                    }
                                });
                        kycData
                                .flatMap(kycData1 -> {
                                    System.out.println(kycData1);
                                    walletAccount.setName(kycData1.getName());
                                    walletAccount.setWalletId(walletResponse1.getWalletId());
                                    walletAccount.setAccountNumber(user.getMobileNumber()+walletResponse1.getWalletId());
                                    walletAccount.setVpa(wallet.getVpa());
                                    walletAccount.setBalance(0.0);
                                    LocalDateTime localDateTime = LocalDateTime.now();
                                    walletAccount.setCreatedAt(localDateTime);
                                    walletAccount.setUpdatedAt(localDateTime);
                                    walletAccountRepository.save(walletAccount);
                                    return Mono.just(kycData1);
                                })
                                .subscribe();
                        return walletToWalletResponse(createdWallet);
                    }
                    else {
                        return Mono.error(new WalletActivatedException("Wallet already activated"));
                    }
                })
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Wallet not found")));

    }

    public Mono<KycData> getKycData(String walletId){
        return kycDataRepository.findByWalletId(walletId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("User kyc data not found")))
                .flatMap(kycData1 -> {
                    KycData kycData = new KycData();
                    kycData.setName(kycData1.getName());
                    kycData.setAddress(kycData1.getAddress());
                    return Mono.just(kycData);
                })
                .onErrorResume(throwable -> {
                    if(throwable instanceof ResourceNotFoundException) {
                        return Mono.error(new ResourceNotFoundException("User kyc data not found"));
                    } else {
                        return Mono.error(new GlobalException("Global exception"));
                    }
                });//
    }

    public Mono<WalletResponse> walletToWalletResponse(Mono<Wallet> wallet){
        Mono<WalletResponse> walletResponseMono = wallet.map(w -> this.modelMapper.map(w,WalletResponse.class));
        return walletResponseMono;
    }

    public Mono<Wallet> walletResponseToWallet(Mono<WalletResponse> walletResponse){
        Mono<Wallet> wallet = walletResponse.map(w -> this.modelMapper.map(w,Wallet.class));
        return wallet;
    }

    public Wallet walletResponseToWallet(WalletResponse walletResponse){
        return this.modelMapper.map(walletResponse,Wallet.class);
    }
}
