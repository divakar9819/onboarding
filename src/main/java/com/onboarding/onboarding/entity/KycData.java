package com.onboarding.onboarding.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author Divakar Verma
 * @created_at : 05/02/2024 - 12:44 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
@Document
@NoArgsConstructor
@AllArgsConstructor
@Data
public class KycData {

    @Id
    private String id;
    @Indexed(unique = true)
    private String walletId;
    private String name;
    private String address;

}
