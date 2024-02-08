package com.onboarding.onboarding.exception;

import ch.qos.logback.core.model.INamedModel;

/**
 * @author Divakar Verma
 * @created_at : 31/01/2024 - 1:10 pm
 * @mail_to: vermadivakar2022@gmail.com
 */
public class InvalidTokenException extends RuntimeException{

    public InvalidTokenException(String message){
        super(message);
    }
}
