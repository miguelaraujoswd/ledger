package com.teya.ledger.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record Transaction(
        UUID id,
        String accountId,
        BigDecimal amount,
        TransactionType type,
        Instant timestamp
) {
    public Transaction(String accountId, BigDecimal amount, TransactionType type) {
        this(UUID.randomUUID(), accountId, amount, type, Instant.now());
    }
}
