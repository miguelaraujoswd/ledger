package com.teya.ledger.service;

import com.teya.ledger.dto.AccountDTO;
import com.teya.ledger.dto.CreateTransactionRequest;
import com.teya.ledger.dto.TransactionDTO;
import com.teya.ledger.exception.AccountNotFoundException;
import com.teya.ledger.exception.InsufficientBalanceException;
import com.teya.ledger.exception.InvalidTransactionAmountException;
import com.teya.ledger.mapper.TransactionMapper;
import com.teya.ledger.model.Transaction;
import com.teya.ledger.model.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LedgerService {

    private final Map<String, List<Transaction>> transactionsByAccount = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> balanceByAccount = new ConcurrentHashMap<>();

    public BigDecimal getBalance(String accountId) throws AccountNotFoundException {
        if (balanceByAccount.containsKey(accountId)) {
            return balanceByAccount.getOrDefault(accountId, BigDecimal.ZERO);
        }
        else throw new AccountNotFoundException(accountId);
    }

    public List<TransactionDTO> getTransactions(String accountId) throws AccountNotFoundException {
        if (transactionsByAccount.containsKey(accountId)) {
            return transactionsByAccount.getOrDefault(accountId, new ArrayList<>()).stream()
                    .sorted(Comparator.comparing(Transaction::timestamp).reversed())
                    .map(TransactionMapper::toDto)
                    .toList();
        }
        else throw new AccountNotFoundException(accountId);
    }

    public TransactionDTO createTransaction(CreateTransactionRequest request, String accountId) throws AccountNotFoundException {
        if(!transactionsByAccount.containsKey(accountId) || !balanceByAccount.containsKey(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        BigDecimal currentBalance = balanceByAccount.getOrDefault(accountId, BigDecimal.ZERO);
        Transaction transaction = new Transaction(accountId, request.amount(), request.type());

        if (transaction.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionAmountException();
        }

        if (transaction.type() == TransactionType.WITHDRAWAL && currentBalance.compareTo(transaction.amount()) < 0) {
            throw new InsufficientBalanceException();
        }

        BigDecimal newBalance = switch (transaction.type()) {
            case DEPOSIT -> currentBalance.add(transaction.amount());
            case WITHDRAWAL -> currentBalance.subtract(transaction.amount());
        };

        balanceByAccount.put(accountId, newBalance);
        transactionsByAccount.computeIfAbsent(accountId, k -> new ArrayList<>()).add(transaction);

        return TransactionMapper.toDto(transaction);
    }

    public AccountDTO createAccount() {
        String accountId = UUID.randomUUID().toString();
            if(!this.transactionsByAccount.containsKey(accountId) && !this.balanceByAccount.containsKey(accountId)) {
                this.transactionsByAccount.put(accountId, new ArrayList<>());
                this.balanceByAccount.put(accountId, BigDecimal.ZERO);
            }
            else {
                return createAccount();
            }
        return new AccountDTO(accountId);
    }
}
