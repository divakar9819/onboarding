package com.onboarding.onboarding.utils;

import java.util.Random;

/**
 * @author Divakar Verma
 * @created_at : 02/02/2024 - 12:58 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
public class Util {

    public static String getRandomWalletId(){
        Random random = new Random();
        long id = random.nextLong(10000);
        return String.valueOf(id);
    }
}
