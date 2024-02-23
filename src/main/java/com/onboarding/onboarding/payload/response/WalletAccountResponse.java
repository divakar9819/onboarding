package com.onboarding.onboarding.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Divakar Verma
 * @created_at : 13/02/2024 - 6:08 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class WalletAccountResponse {
    private double balance;
    private String accountNumber;
    private String name;
    private String vpa;
}
