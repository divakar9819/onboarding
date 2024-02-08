package com.onboarding.onboarding.exception;

/**
 * @author Divakar Verma
 * @created_at : 05/02/2024 - 3:39 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(String message){
        super(message);
    }
}
