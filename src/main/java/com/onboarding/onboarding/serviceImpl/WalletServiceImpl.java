package com.onboarding.onboarding.serviceImpl;

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoWriteException;
import com.onboarding.onboarding.entity.KycData;
import com.onboarding.onboarding.entity.Wallet;
import com.onboarding.onboarding.exception.*;
import com.onboarding.onboarding.payload.request.KycDataRequest;
import com.onboarding.onboarding.payload.response.ApiResponse;
import com.onboarding.onboarding.payload.response.ValidTokenResponse;
import com.onboarding.onboarding.payload.response.WalletResponse;
import com.onboarding.onboarding.repository.KycDataRepository;
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
