package com.onboarding.onboarding.repository;

import com.onboarding.onboarding.entity.KycData;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * @author Divakar Verma
 * @created_at : 05/02/2024 - 12:53 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
public interface KycDataRepository extends ReactiveCrudRepository<KycData,String> {

    public Mono<KycData> findByWalletId(String walletId);
}
