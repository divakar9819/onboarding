package com.onboarding.onboarding.controller;

import com.onboarding.onboarding.entity.Wallet;
import com.onboarding.onboarding.exception.*;
import com.onboarding.onboarding.payload.request.KycDataRequest;
import com.onboarding.onboarding.payload.request.WalletRequest;
import com.onboarding.onboarding.payload.response.ApiResponse;
import com.onboarding.onboarding.payload.response.WalletResponse;
import com.onboarding.onboarding.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @author Divakar Verma
 * @created_at : 30/01/2024 - 6:21 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
@RestController
@RequestMapping("api/v1/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;
    @GetMapping("/home")
    public String home(){
        return "Wallet home";
    }

    @PostMapping("/createWallet")
    public Mono<ResponseEntity<WalletResponse>> createWallet(){
        return walletService.createWallet()
                .map(createdWallet -> ResponseEntity.status(HttpStatus.CREATED).body(createdWallet))
                .onErrorResume(throwable -> {
                    throw new ValidationErrorException("User wallet is already created");
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    @PostMapping("/kyc")
    public Mono<ResponseEntity<ApiResponse>> doKyc(@RequestBody KycDataRequest kycDataRequest){
        return walletService.doKyc(kycDataRequest)
                .map(createdKyc -> ResponseEntity.status(HttpStatus.CREATED).body(createdKyc))
                .onErrorResume(throwable -> {
                    throw new ValidationErrorException(throwable.getMessage());
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
    }

    @PostMapping("/createVpa")
    public Mono<? extends ResponseEntity<WalletResponse>> createVpa(){
        return walletService.createVpa()
                .map(walletResponse -> new ResponseEntity<>(walletResponse, HttpStatus.CREATED))
                .onErrorResume(error -> {
                    if(error instanceof WalletActivatedException){
                        throw new WalletActivatedException(error.getMessage());
                    }
                    else if(error instanceof ResourceNotFoundException){
                        throw new ResourceNotFoundException(error.getMessage());
                    }
                    else {
                        throw new GlobalException(error.getMessage());
                    }
                });
    }

    @PostMapping("/activateCard")
    public Mono<ResponseEntity<WalletResponse>> activateCard(@RequestBody WalletRequest walletRequest){
        return walletService.activateCard(walletRequest)
                .map(walletResponse -> ResponseEntity.status(HttpStatus.CREATED).body(walletResponse))
                .onErrorResume(throwable -> {
                    if (throwable instanceof CardAlreadyActivatedException) {
                        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
                    } else {
                        return Mono.error(throwable);
                    }
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
