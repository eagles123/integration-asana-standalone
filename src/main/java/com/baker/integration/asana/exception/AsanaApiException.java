package com.baker.integration.asana.exception;

public class AsanaApiException extends RuntimeException {
    public AsanaApiException(String message) { super(message); }
    public AsanaApiException(String message, Throwable cause) { super(message, cause); }
}
