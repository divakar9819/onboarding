package com.onboarding.onboarding.exception;

/**
 * @author Divakar Verma
 * @created_at : 31/01/2024 - 1:12 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
public class ConnectionRefusedException extends RuntimeException{

    public ConnectionRefusedException(String message){
        super(message);
    }
}
