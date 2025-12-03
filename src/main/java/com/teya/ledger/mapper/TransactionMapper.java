package com.teya.ledger.mapper;

import com.teya.ledger.dto.TransactionDTO;
import com.teya.ledger.model.Transaction;

public class TransactionMapper {

    public static TransactionDTO toDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new TransactionDTO(
                transaction.id(),
                transaction.accountId(),
                transaction.amount(),
                transaction.type(),
                transaction.timestamp()
        );
    }

    public static Transaction toModel(TransactionDTO transactionDTO) {
        if (transactionDTO == null) {
            return null;
        }
        return new Transaction(
                transactionDTO.id(),
                transactionDTO.accountId(),
                transactionDTO.amount(),
                transactionDTO.type(),
                transactionDTO.timestamp()
        );
    }
}