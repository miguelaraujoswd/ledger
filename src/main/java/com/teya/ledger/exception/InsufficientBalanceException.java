package com.teya.ledger.exception;

public class InsufficientBalanceException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "Account has insufficient balance for this transaction";

    public InsufficientBalanceException() {
        super(DEFAULT_MESSAGE);
    }

    public InsufficientBalanceException(String message) {
        super(message);
    }
}

