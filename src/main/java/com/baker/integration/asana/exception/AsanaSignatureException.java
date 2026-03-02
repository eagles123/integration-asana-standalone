package com.baker.integration.asana.exception;

public class AsanaSignatureException extends RuntimeException {
    public AsanaSignatureException(String message) { super(message); }
    public AsanaSignatureException(String message, Throwable cause) { super(message, cause); }
}
