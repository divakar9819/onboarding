package com.onboarding.onboarding.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Divakar Verma
 * @created_at : 05/02/2024 - 12:48 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class KycDataResponse {

    private String id;
    private String walletId;
    private String name;
    private String address;
}
