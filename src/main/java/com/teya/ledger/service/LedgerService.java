package com.teya.ledger.service;

import com.teya.ledger.dto.CreateTransactionRequest;
import com.teya.ledger.dto.TransactionDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LedgerService {
    public BigDecimal getBalance(String accountId) {
        return BigDecimal.ZERO;
    }

    public List<TransactionDTO> getTransactions(String accountId) {
        return null;
    }

    public TransactionDTO createTransaction(CreateTransactionRequest request, String accountId) {
        return null;
    }
}
