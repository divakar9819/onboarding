package com.onboarding.onboarding.exception;

/**
 * @author Divakar Verma
 * @created_at : 31/01/2024 - 1:11 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
public class ServerErrorException extends RuntimeException{
    public ServerErrorException(String message){
        super(message);
    }
}
