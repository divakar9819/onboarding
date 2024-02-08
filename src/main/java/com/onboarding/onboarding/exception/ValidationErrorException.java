package com.onboarding.onboarding.exception;

/**
 * @author Divakar Verma
 * @created_at : 02/02/2024 - 3:14 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
public class ValidationErrorException extends RuntimeException{

    public ValidationErrorException(String message){
        super(message);
    }
}
