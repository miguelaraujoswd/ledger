package com.teya.ledger.exception;

public class InvalidTransactionAmountException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Transaction amount must be greater than zero";

    public InvalidTransactionAmountException() {
        super(DEFAULT_MESSAGE);
    }

    public InvalidTransactionAmountException(String message) {
        super(message);
    }
}

