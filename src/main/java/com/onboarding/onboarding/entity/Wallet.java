package com.onboarding.onboarding.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.xml.transform.Source;

/**
 * @author Divakar Verma
 * @created_at : 30/01/2024 - 5:15 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
@Document(collection = "wallets")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Wallet {

    @Id
    private String id;
    @Indexed(unique = true)
    private String walletId;
    @Indexed(unique = true)
    private String username;
    private String cardId;
    private String kycStatus;
    //private double balance;
    private String cardStatus;
    private String vpa;
    private String state;
    private String status;
}
