package com.urlshortener.exception;

public class AliasAlreadyTakenException extends RuntimeException {
    public AliasAlreadyTakenException(String alias) {
        super("Custom alias is already taken: " + alias);
    }
}
