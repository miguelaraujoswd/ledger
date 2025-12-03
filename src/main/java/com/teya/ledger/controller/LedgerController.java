package com.teya.ledger.controller;

import com.teya.ledger.dto.CreateTransactionRequest;
import com.teya.ledger.dto.TransactionDTO;
import com.teya.ledger.service.LedgerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(final LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountId) {
        return ResponseEntity.ok().body(ledgerService.getBalance(accountId));
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionDTO>> getTransactions(@PathVariable String accountId) {
        List<TransactionDTO> transactions = ledgerService.getTransactions(accountId);
        return ResponseEntity.ok().body(transactions);
    }

    @PostMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<TransactionDTO> createTransaction(@PathVariable String accountId, @RequestBody CreateTransactionRequest request) {
        TransactionDTO createdTransaction = ledgerService.createTransaction(request, accountId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }
}
