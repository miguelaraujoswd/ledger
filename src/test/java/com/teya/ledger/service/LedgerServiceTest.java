package com.teya.ledger.service;

import com.teya.ledger.dto.AccountDTO;
import com.teya.ledger.dto.CreateTransactionRequest;
import com.teya.ledger.dto.TransactionDTO;
import com.teya.ledger.exception.AccountNotFoundException;
import com.teya.ledger.exception.InsufficientBalanceException;
import com.teya.ledger.exception.InvalidTransactionAmountException;
import com.teya.ledger.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LedgerServiceTest {

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        ledgerService = new LedgerService();
    }

    @Test
    void createAccount_shouldCreateAccountWithUniqueIdAndZeroBalance() {
        AccountDTO account = ledgerService.createAccount();

        assertThat(account).isNotNull();
        assertThat(account.accountId()).isNotBlank();
        assertThat(ledgerService.getBalance(account.accountId())).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void createAccount_shouldCreateMultipleAccountsWithDifferentIds() {
        AccountDTO account1 = ledgerService.createAccount();
        AccountDTO account2 = ledgerService.createAccount();
        AccountDTO account3 = ledgerService.createAccount();

        assertThat(account1.accountId())
                .isNotEqualTo(account2.accountId())
                .isNotEqualTo(account3.accountId());
        assertThat(account2.accountId()).isNotEqualTo(account3.accountId());
    }

    @Test
    void getBalance_shouldReturnZeroForNewAccount() {
        AccountDTO account = ledgerService.createAccount();

        BigDecimal balance = ledgerService.getBalance(account.accountId());

        assertThat(balance).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void getBalance_shouldThrowExceptionForNonExistentAccount() {
        String nonExistentAccountId = "non-existent-id";

        assertThatThrownBy(() -> ledgerService.getBalance(nonExistentAccountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(nonExistentAccountId);
    }

    @Test
    void getBalance_shouldReturnCorrectBalanceAfterDeposit() {
        AccountDTO account = ledgerService.createAccount();
        CreateTransactionRequest depositRequest = new CreateTransactionRequest(
                new BigDecimal("100.00"), TransactionType.DEPOSIT);

        ledgerService.createTransaction(depositRequest, account.accountId());

        assertThat(ledgerService.getBalance(account.accountId())).isEqualByComparingTo("100.00");
    }

    @Test
    void getBalance_shouldReturnCorrectBalanceAfterMultipleTransactions() {
        AccountDTO account = ledgerService.createAccount();

        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("100.00"), TransactionType.DEPOSIT),
                account.accountId());
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("30.00"), TransactionType.WITHDRAWAL),
                account.accountId());
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("50.00"), TransactionType.DEPOSIT),
                account.accountId());

        assertThat(ledgerService.getBalance(account.accountId())).isEqualByComparingTo("120.00");
    }

    @Test
    void getTransactions_shouldReturnEmptyListForNewAccount() {
        AccountDTO account = ledgerService.createAccount();

        List<TransactionDTO> transactions = ledgerService.getTransactions(account.accountId());

        assertThat(transactions).isEmpty();
    }

    @Test
    void getTransactions_shouldThrowExceptionForNonExistentAccount() {
        String nonExistentAccountId = "non-existent-id";

        assertThatThrownBy(() -> ledgerService.getTransactions(nonExistentAccountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(nonExistentAccountId);
    }

    @Test
    void getTransactions_shouldReturnTransactionsSortedByTimestampDescending() throws InterruptedException {
        AccountDTO account = ledgerService.createAccount();

        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("100.00"), TransactionType.DEPOSIT),
                account.accountId());
        Thread.sleep(10);
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("50.00"), TransactionType.DEPOSIT),
                account.accountId());
        Thread.sleep(10);
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("25.00"), TransactionType.WITHDRAWAL),
                account.accountId());

        List<TransactionDTO> transactions = ledgerService.getTransactions(account.accountId());

        assertThat(transactions).hasSize(3);
        assertThat(transactions.get(0).amount()).isEqualByComparingTo("25.00");
        assertThat(transactions.get(0).type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(transactions.get(1).amount()).isEqualByComparingTo("50.00");
        assertThat(transactions.get(2).amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void createTransaction_deposit_shouldCreateDepositAndUpdateBalance() {
        AccountDTO account = ledgerService.createAccount();
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("100.00"), TransactionType.DEPOSIT);

        TransactionDTO transaction = ledgerService.createTransaction(request, account.accountId());

        assertThat(transaction).isNotNull();
        assertThat(transaction.id()).isNotNull();
        assertThat(transaction.accountId()).isEqualTo(account.accountId());
        assertThat(transaction.amount()).isEqualByComparingTo("100.00");
        assertThat(transaction.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(transaction.timestamp()).isNotNull();
        assertThat(ledgerService.getBalance(account.accountId())).isEqualByComparingTo("100.00");
    }

    @Test
    void createTransaction_deposit_shouldAllowMultipleDeposits() {
        AccountDTO account = ledgerService.createAccount();

        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("100.00"), TransactionType.DEPOSIT),
                account.accountId());
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("200.00"), TransactionType.DEPOSIT),
                account.accountId());
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("50.50"), TransactionType.DEPOSIT),
                account.accountId());

        assertThat(ledgerService.getBalance(account.accountId())).isEqualByComparingTo("350.50");
        assertThat(ledgerService.getTransactions(account.accountId())).hasSize(3);
    }

    @Test
    void createTransaction_withdrawal_shouldCreateWithdrawalAndUpdateBalance() {
        AccountDTO account = ledgerService.createAccount();
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("100.00"), TransactionType.DEPOSIT),
                account.accountId());

        TransactionDTO transaction = ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("30.00"), TransactionType.WITHDRAWAL),
                account.accountId());

        assertThat(transaction).isNotNull();
        assertThat(transaction.type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(transaction.amount()).isEqualByComparingTo("30.00");
        assertThat(ledgerService.getBalance(account.accountId())).isEqualByComparingTo("70.00");
    }

    @Test
    void createTransaction_withdrawal_shouldAllowWithdrawalOfEntireBalance() {
        AccountDTO account = ledgerService.createAccount();
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("100.00"), TransactionType.DEPOSIT),
                account.accountId());

        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("100.00"), TransactionType.WITHDRAWAL),
                account.accountId());

        assertThat(ledgerService.getBalance(account.accountId())).isEqualByComparingTo("0.00");
    }

    @Test
    void createTransaction_withdrawal_shouldThrowExceptionWhenBalanceInsufficient() {
        AccountDTO account = ledgerService.createAccount();
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("100.00"), TransactionType.DEPOSIT),
                account.accountId());

        CreateTransactionRequest overdraftRequest = new CreateTransactionRequest(
                new BigDecimal("150.00"), TransactionType.WITHDRAWAL);

        assertThatThrownBy(() -> ledgerService.createTransaction(overdraftRequest, account.accountId()))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(ledgerService.getBalance(account.accountId())).isEqualByComparingTo("100.00");
    }

    @Test
    void createTransaction_validation_shouldThrowExceptionForZeroAmount() {
        AccountDTO account = ledgerService.createAccount();
        CreateTransactionRequest request = new CreateTransactionRequest(
                BigDecimal.ZERO, TransactionType.DEPOSIT);

        assertThatThrownBy(() -> ledgerService.createTransaction(request, account.accountId()))
                .isInstanceOf(InvalidTransactionAmountException.class);
    }

    @Test
    void createTransaction_validation_shouldThrowExceptionForNegativeAmount() {
        AccountDTO account = ledgerService.createAccount();
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("-50.00"), TransactionType.DEPOSIT);

        assertThatThrownBy(() -> ledgerService.createTransaction(request, account.accountId()))
                .isInstanceOf(InvalidTransactionAmountException.class);
    }

    @Test
    void createTransaction_validation_shouldThrowExceptionForNonExistentAccount() {
        String nonExistentAccountId = "non-existent-id";
        CreateTransactionRequest request = new CreateTransactionRequest(
                new BigDecimal("100.00"), TransactionType.DEPOSIT);

        assertThatThrownBy(() -> ledgerService.createTransaction(request, nonExistentAccountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(nonExistentAccountId);
    }

    @Test
    void createTransaction_precision_shouldHandleDecimalPrecisionCorrectly() {
        AccountDTO account = ledgerService.createAccount();

        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("10.10"), TransactionType.DEPOSIT),
                account.accountId());
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("20.20"), TransactionType.DEPOSIT),
                account.accountId());
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("5.05"), TransactionType.WITHDRAWAL),
                account.accountId());

        assertThat(ledgerService.getBalance(account.accountId())).isEqualByComparingTo("25.25");
    }

    @Test
    void createTransaction_precision_shouldHandleSmallDecimalAmounts() {
        AccountDTO account = ledgerService.createAccount();

        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("0.01"), TransactionType.DEPOSIT),
                account.accountId());

        assertThat(ledgerService.getBalance(account.accountId())).isEqualByComparingTo("0.01");
    }

    @Test
    void accountIsolation_transactionsShouldNotAffectOtherAccounts() {
        AccountDTO account1 = ledgerService.createAccount();
        AccountDTO account2 = ledgerService.createAccount();

        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("100.00"), TransactionType.DEPOSIT),
                account1.accountId());
        ledgerService.createTransaction(
                new CreateTransactionRequest(new BigDecimal("200.00"), TransactionType.DEPOSIT),
                account2.accountId());

        assertThat(ledgerService.getBalance(account1.accountId())).isEqualByComparingTo("100.00");
        assertThat(ledgerService.getBalance(account2.accountId())).isEqualByComparingTo("200.00");
        assertThat(ledgerService.getTransactions(account1.accountId())).hasSize(1);
        assertThat(ledgerService.getTransactions(account2.accountId())).hasSize(1);
    }
}
