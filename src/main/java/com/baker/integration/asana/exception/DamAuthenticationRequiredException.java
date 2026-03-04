package com.baker.integration.asana.exception;

public class DamAuthenticationRequiredException extends RuntimeException {

    public DamAuthenticationRequiredException(String message) {
        super(message);
    }
}
