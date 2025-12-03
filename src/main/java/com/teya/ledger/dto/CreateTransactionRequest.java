package com.teya.ledger.dto;

import com.teya.ledger.model.TransactionType;

import java.math.BigDecimal;

public record CreateTransactionRequest(
        BigDecimal amount,
        TransactionType type
) {}
