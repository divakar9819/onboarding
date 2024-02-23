package com.onboarding.onboarding.service;

import com.onboarding.onboarding.entity.Wallet;
import com.onboarding.onboarding.payload.request.KycDataRequest;
import com.onboarding.onboarding.payload.request.WalletRequest;
import com.onboarding.onboarding.payload.response.ApiResponse;
import com.onboarding.onboarding.payload.response.ValidTokenResponse;
import com.onboarding.onboarding.payload.response.WalletResponse;
import reactor.core.publisher.Mono;

/**
 * @author Divakar Verma
 * @created_at : 30/01/2024 - 6:17 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
public interface WalletService {

    public Mono<WalletResponse> createWallet();

    public Mono<WalletResponse> getWalletByUsername(String username);

    public Mono<ApiResponse> doKyc(KycDataRequest kycDataRequest);
    public Mono<ValidTokenResponse> validateToken(String token);

    public Mono<WalletResponse> createVpa();

    public Mono<WalletResponse> activateCard(WalletRequest walletRequest);

}
