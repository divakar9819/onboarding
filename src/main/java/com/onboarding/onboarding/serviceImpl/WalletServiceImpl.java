package com.onboarding.onboarding.serviceImpl;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;
import com.onboarding.onboarding.entity.KycData;
import com.onboarding.onboarding.entity.Wallet;
import com.onboarding.onboarding.exception.*;
import com.onboarding.onboarding.payload.request.KycDataRequest;
import com.onboarding.onboarding.payload.request.WalletAccountRequest;
import com.onboarding.onboarding.payload.request.WalletRequest;
import com.onboarding.onboarding.payload.response.*;
import com.onboarding.onboarding.repository.KycDataRepository;
import com.onboarding.onboarding.repository.WalletRepository;
import com.onboarding.onboarding.service.WalletService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

import static com.onboarding.onboarding.interceptor.AuthenticationInterceptor.getAuthToken;
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
    @Qualifier("getAuthWebClient")
    private WebClient authWebClient;

    @Autowired
    @Qualifier("getTMWebClient")
    private WebClient tmWebClient;

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
        wallet.setCardStatus("pending");
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
    return getWalletByUsername(username)
        .flatMap(walletResponse -> {
            if (walletResponse.getKycStatus().equalsIgnoreCase("pending")) {
                KycData kycData = new KycData();
                kycData.setWalletId(walletResponse.getWalletId());
                kycData.setName(kycDataRequest.getName());
                kycData.setAddress(kycDataRequest.getAddress());

                return kycDataRepository.save(kycData)
                    .then(Mono.defer(() -> {
                        Wallet wallet = walletResponseToWallet(walletResponse);
                        wallet.setKycStatus("completed");
                        wallet.setState("vpa");
                        wallet.setUsername(username);
                        return walletRepository.save(wallet);
                    }))
                    .map(savedWallet -> new ApiResponse("Kyc done successfully", true))
                    .onErrorResume(throwable -> {
                        if (throwable instanceof DuplicateKeyException || throwable instanceof MongoWriteException) {
                            return Mono.error(new ValidationErrorException(throwable.getMessage()));
                        } else {
                            return Mono.error(throwable);
                        }
                    });
            } else if (walletResponse.getKycStatus().equalsIgnoreCase("completed")) {
                return Mono.just(new ApiResponse("User kyc already done", true));
            } else {
                return Mono.error(new GlobalException("Invalid kyc status"));
            }
        });
}

    @Override
    public Mono<ValidTokenResponse> validateToken(String token) {
        return authWebClient.post().uri("/validateToken")
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
        return authWebClient.get().uri("/getUserByUsername/"+username)
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

    public Mono<WalletAccountResponse> createWalletAccount(WalletAccountRequest walletAccountRequest){
        return tmWebClient.post().uri("/createWallet")
                .body(Mono.just(walletAccountRequest),WalletAccountRequest.class)
                .header(HttpHeaders.AUTHORIZATION, getAuthToken())
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
                .bodyToMono(WalletAccountResponse.class);
    }

    @Override
    public Mono<WalletResponse> createVpa() {
    String username = getUsername();
    return getWalletByUsername(username)
        .flatMap(walletResponse -> {
            if (walletResponse.getState().equalsIgnoreCase("vpa")) {
                return getKycData(walletResponse.getWalletId())
                    .flatMap(kycData -> {
                        WalletAccountRequest walletAccountRequest = new WalletAccountRequest();
                        walletAccountRequest.setWalletId(walletResponse.getWalletId());
                        walletAccountRequest.setName(kycData.getName());
                        return createWalletAccount(walletAccountRequest)
                            .flatMap(walletAccountResponse -> {
                                if (walletAccountResponse.getVpa() != null && walletAccountResponse.getBalance() == 0.0) {
                                    Wallet wallet = walletResponseToWallet(walletResponse);
                                    wallet.setVpa(walletAccountResponse.getVpa());
                                    wallet.setState("active");
                                    wallet.setStatus("completed");
                                    return walletRepository.save(wallet)
                                        .map(createdWallet -> {
                                            System.out.println(createdWallet);
                                            WalletResponse createdWalletResponse = new WalletResponse();
                                            createdWalletResponse.setId(createdWallet.getId());
                                            createdWalletResponse.setWalletId(createdWallet.getWalletId());
                                            createdWalletResponse.setVpa(createdWallet.getVpa());
                                            createdWalletResponse.setKycStatus(createdWallet.getKycStatus());
                                            createdWalletResponse.setName(kycData.getName());
                                            createdWalletResponse.setStatus(createdWallet.getStatus());
                                            createdWalletResponse.setState(createdWallet.getState());
                                            createdWalletResponse.setUsername(createdWallet.getUsername());
                                            return createdWalletResponse;
                                        })
                                        .onErrorResume(throwable -> {
                                            if (throwable instanceof DuplicateKeyException || throwable instanceof MongoWriteException) {
                                                return Mono.error(new ValidationErrorException(throwable.getMessage()));
                                            } else {
                                                return Mono.error(throwable);
                                            }
                                        });
                                } else {
                                    return Mono.error(new Exception("Invalid VPA or balance"));
                                }
                            })
                            .onErrorResume(throwable -> {
                                return Mono.error(throwable);
                            });
                    })
                    .onErrorResume(throwable -> {
                        return Mono.error(throwable);
                    });
            } else {
                return Mono.error(new WalletActivatedException("Wallet already activated"));
            }
        });
    }

    @Override
    public Mono<WalletResponse> activateCard(WalletRequest walletRequest) {

        return getWalletByUsername(walletRequest.getUsername())
                .flatMap(walletResponse -> {
                    if(walletResponse.getCardStatus().equalsIgnoreCase("pending")){
                        return getKycData(walletResponse.getWalletId())
                                .flatMap(kycData -> {
                                    WalletResponse response = new WalletResponse();
                                    response.setName(kycData.getName());
                                    Wallet wallet = walletResponseToWallet(walletResponse);
                                    wallet.setCardId(walletRequest.getCardId());
                                    wallet.setCardStatus("activated");
                                    return walletRepository.save(wallet)
                                            .flatMap(updatedWallet -> {
                                                response.setWalletId(updatedWallet.getWalletId());
                                                response.setCardId(updatedWallet.getCardId());
                                                response.setWalletId(updatedWallet.getWalletId());
                                                response.setCardStatus(updatedWallet.getCardStatus());
                                                return Mono.just(response);
                                            })
                                            .switchIfEmpty(Mono.error(new GlobalException("Getting error while saving data in wallet")));
                                })
                                .onErrorResume(Mono:: error );
                    }
                    else {
                        return Mono.error(new CardAlreadyActivatedException("User card already activated"));
                    }
                })
                .onErrorResume(Mono::error);
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
                        return Mono.error(throwable);
                    }
                });
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
