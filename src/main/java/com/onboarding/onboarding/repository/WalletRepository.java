package com.onboarding.onboarding.repository;

import com.onboarding.onboarding.entity.Wallet;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * @author Divakar Verma
 * @created_at : 30/01/2024 - 5:20 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
public interface WalletRepository extends ReactiveCrudRepository<Wallet, String> {

//    @Query(value = "{'username' : ?0}")
//    public Mono<Wallet> queryByUsername(@Param("username") String username);

    public Mono<Wallet> findByUsername(String username);
}
