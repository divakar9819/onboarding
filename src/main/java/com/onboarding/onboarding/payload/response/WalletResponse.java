package com.onboarding.onboarding.payload.response;

import com.onboarding.onboarding.entity.KycData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Divakar Verma
 * @created_at : 31/01/2024 - 11:54 am
 * @mail_to: vermadivakar2022@gmail.com
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class WalletResponse {
    private String id;
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
