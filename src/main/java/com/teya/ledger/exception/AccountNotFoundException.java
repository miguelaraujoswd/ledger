package com.teya.ledger.exception;

public class AccountNotFoundException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Account not found with id: ";

    public AccountNotFoundException(String id) {
        super(DEFAULT_MESSAGE + id);
    }

    public AccountNotFoundException(String message, String id) {
        super(message + id);
    }
}