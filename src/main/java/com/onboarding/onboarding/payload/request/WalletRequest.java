package com.onboarding.onboarding.payload.request;

import com.onboarding.onboarding.entity.KycData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Divakar Verma
 * @created_at : 31/01/2024 - 11:53 am
 * @mail_to: vermadivakar2022@gmail.com
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class WalletRequest {

    private String walletId;
    private String username;
    private String cardId;
    private String kycStatus;
    private double balance;
    private String cardStatus;
    private String vpa;
    private String state;
    private String status;

}
