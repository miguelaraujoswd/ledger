package com.teya.ledger.controller;

import com.teya.ledger.dto.AccountDTO;
import com.teya.ledger.dto.CreateTransactionRequest;
import com.teya.ledger.dto.TransactionDTO;
import com.teya.ledger.exception.AccountNotFoundException;
import com.teya.ledger.service.LedgerService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
@Validated
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(final LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountDTO> createAccount() {
        AccountDTO createdAccount = ledgerService.createAccount();
        return ResponseEntity.status(HttpStatus.CREATED).body(createdAccount);
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountId) {
        try {
            return ResponseEntity.ok().body(ledgerService.getBalance(accountId));
        } catch (AccountNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<List<TransactionDTO>> getTransactions(@PathVariable String accountId) {
        List<TransactionDTO> transactions;
        try {
            transactions = ledgerService.getTransactions(accountId);
            return ResponseEntity.ok().body(transactions);

        } catch (AccountNotFoundException e) {
            return  ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<TransactionDTO> createTransaction(@NotBlank @PathVariable String accountId,
                                                            @RequestBody CreateTransactionRequest request) {
        TransactionDTO createdTransaction = null;
        try {
            createdTransaction = ledgerService.createTransaction(request, accountId);
        } catch (AccountNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTransaction);
    }
}
