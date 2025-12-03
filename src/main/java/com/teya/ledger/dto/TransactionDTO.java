package com.teya.ledger.dto;

import com.teya.ledger.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionDTO(
        UUID id,
        String accountId,
        BigDecimal amount,
        TransactionType type,
        Instant timestamp
) {}
