package com.teya.ledger.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teya.ledger.dto.AccountDTO;
import com.teya.ledger.dto.CreateTransactionRequest;
import com.teya.ledger.dto.TransactionDTO;
import com.teya.ledger.exception.AccountNotFoundException;
import com.teya.ledger.exception.ErrorResponse;
import com.teya.ledger.exception.InsufficientBalanceException;
import com.teya.ledger.model.TransactionType;
import com.teya.ledger.service.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LedgerController.class)
class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LedgerService ledgerService;

    private static final String BASE_URL = "/api/v1/ledger";

    @Nested
    @DisplayName("POST /api/v1/ledger/accounts")
    class CreateAccountTests {

        @Test
        void shouldCreateAccountAndReturn201() throws Exception {
            String accountId = UUID.randomUUID().toString();
            AccountDTO expected = new AccountDTO(accountId);
            when(ledgerService.createAccount()).thenReturn(expected);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/accounts"))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            AccountDTO actual = objectMapper.readValue(result.getResponse().getContentAsString(), AccountDTO.class);

            assertThat(actual).isEqualTo(expected);
            verify(ledgerService, times(1)).createAccount();
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ledger/accounts/{accountId}/balance")
    class GetBalanceTests {

        @Test
        void shouldReturnBalanceWith200Status() throws Exception {
            String accountId = "test-account-id";
            BigDecimal expected = new BigDecimal("150.50");
            when(ledgerService.getBalance(accountId)).thenReturn(expected);

            MvcResult result = mockMvc.perform(get(BASE_URL + "/accounts/{accountId}/balance", accountId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            BigDecimal actual = objectMapper.readValue(result.getResponse().getContentAsString(), BigDecimal.class);

            assertThat(actual).isEqualByComparingTo(expected);
            verify(ledgerService, times(1)).getBalance(accountId);
        }

        @Test
        void shouldReturn404ForNonExistentAccount() throws Exception {
            String accountId = "non-existent-id";
            when(ledgerService.getBalance(accountId)).thenThrow(new AccountNotFoundException(accountId));

            mockMvc.perform(get(BASE_URL + "/accounts/{accountId}/balance", accountId))
                    .andExpect(status().isNotFound());

            verify(ledgerService, times(1)).getBalance(accountId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/ledger/accounts/{accountId}/transactions")
    class GetTransactionsTests {

        @Test
        void shouldReturnTransactionsWith200Status() throws Exception {
            String accountId = "test-account-id";
            UUID transactionId = UUID.randomUUID();
            Instant timestamp = Instant.now();
            TransactionDTO expected = new TransactionDTO(
                    transactionId, accountId, new BigDecimal("100.00"), TransactionType.DEPOSIT, timestamp);
            when(ledgerService.getTransactions(accountId)).thenReturn(List.of(expected));

            MvcResult result = mockMvc.perform(get(BASE_URL + "/accounts/{accountId}/transactions", accountId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            List<TransactionDTO> actual = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertThat(actual).hasSize(1);
            assertThat(actual.get(0).id()).isEqualTo(transactionId);
            assertThat(actual.get(0).accountId()).isEqualTo(accountId);
            assertThat(actual.get(0).amount()).isEqualByComparingTo("100.00");
            assertThat(actual.get(0).type()).isEqualTo(TransactionType.DEPOSIT);
            verify(ledgerService, times(1)).getTransactions(accountId);
        }

        @Test
        void shouldReturnEmptyListForAccountWithNoTransactions() throws Exception {
            String accountId = "test-account-id";
            when(ledgerService.getTransactions(accountId)).thenReturn(List.of());

            MvcResult result = mockMvc.perform(get(BASE_URL + "/accounts/{accountId}/transactions", accountId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            List<TransactionDTO> actual = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertThat(actual).isEmpty();
            verify(ledgerService, times(1)).getTransactions(accountId);
        }

        @Test
        void shouldReturn404ForNonExistentAccount() throws Exception {
            String accountId = "non-existent-id";
            when(ledgerService.getTransactions(accountId)).thenThrow(new AccountNotFoundException(accountId));

            mockMvc.perform(get(BASE_URL + "/accounts/{accountId}/transactions", accountId))
                    .andExpect(status().isNotFound());

            verify(ledgerService, times(1)).getTransactions(accountId);
        }

        @Test
        void shouldReturnMultipleTransactionsSortedCorrectly() throws Exception {
            String accountId = "test-account-id";
            Instant now = Instant.now();
            TransactionDTO tx1 = new TransactionDTO(
                    UUID.randomUUID(), accountId, new BigDecimal("100.00"), TransactionType.DEPOSIT, now.minusSeconds(20));
            TransactionDTO tx2 = new TransactionDTO(
                    UUID.randomUUID(), accountId, new BigDecimal("50.00"), TransactionType.WITHDRAWAL, now.minusSeconds(10));
            TransactionDTO tx3 = new TransactionDTO(
                    UUID.randomUUID(), accountId, new BigDecimal("25.00"), TransactionType.DEPOSIT, now);

            when(ledgerService.getTransactions(accountId)).thenReturn(List.of(tx3, tx2, tx1));

            MvcResult result = mockMvc.perform(get(BASE_URL + "/accounts/{accountId}/transactions", accountId))
                    .andExpect(status().isOk())
                    .andReturn();

            List<TransactionDTO> actual = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {}
            );

            assertThat(actual).hasSize(3);
            assertThat(actual.get(0).amount()).isEqualByComparingTo("25.00");
            assertThat(actual.get(1).amount()).isEqualByComparingTo("50.00");
            assertThat(actual.get(2).amount()).isEqualByComparingTo("100.00");
            verify(ledgerService, times(1)).getTransactions(accountId);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/ledger/accounts/{accountId}/transactions")
    class CreateTransactionTests {

        @Test
        void shouldCreateDepositTransactionAndReturn201() throws Exception {
            String accountId = "test-account-id";
            UUID transactionId = UUID.randomUUID();
            Instant timestamp = Instant.now();
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("100.00"), TransactionType.DEPOSIT);
            TransactionDTO expected = new TransactionDTO(
                    transactionId, accountId, new BigDecimal("100.00"), TransactionType.DEPOSIT, timestamp);

            when(ledgerService.createTransaction(any(CreateTransactionRequest.class), eq(accountId)))
                    .thenReturn(expected);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andReturn();

            TransactionDTO actual = objectMapper.readValue(result.getResponse().getContentAsString(), TransactionDTO.class);

            assertThat(actual.id()).isEqualTo(transactionId);
            assertThat(actual.accountId()).isEqualTo(accountId);
            assertThat(actual.amount()).isEqualByComparingTo("100.00");
            assertThat(actual.type()).isEqualTo(TransactionType.DEPOSIT);
            verify(ledgerService, times(1)).createTransaction(any(CreateTransactionRequest.class), eq(accountId));
        }

        @Test
        void shouldCreateWithdrawalTransactionAndReturn201() throws Exception {
            String accountId = "test-account-id";
            UUID transactionId = UUID.randomUUID();
            Instant timestamp = Instant.now();
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("50.00"), TransactionType.WITHDRAWAL);
            TransactionDTO expected = new TransactionDTO(
                    transactionId, accountId, new BigDecimal("50.00"), TransactionType.WITHDRAWAL, timestamp);

            when(ledgerService.createTransaction(any(CreateTransactionRequest.class), eq(accountId)))
                    .thenReturn(expected);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            TransactionDTO actual = objectMapper.readValue(result.getResponse().getContentAsString(), TransactionDTO.class);

            assertThat(actual.type()).isEqualTo(TransactionType.WITHDRAWAL);
            assertThat(actual.amount()).isEqualByComparingTo("50.00");
            verify(ledgerService, times(1)).createTransaction(any(CreateTransactionRequest.class), eq(accountId));
        }

        @Test
        void shouldReturn404ForNonExistentAccount() throws Exception {
            String accountId = "non-existent-id";
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("100.00"), TransactionType.DEPOSIT);

            when(ledgerService.createTransaction(any(CreateTransactionRequest.class), eq(accountId)))
                    .thenThrow(new AccountNotFoundException(accountId));

            mockMvc.perform(post(BASE_URL + "/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());

            verify(ledgerService, times(1)).createTransaction(any(CreateTransactionRequest.class), eq(accountId));
        }

        @Test
        void shouldReturn400ForInsufficientBalance() throws Exception {
            String accountId = "test-account-id";
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("1000.00"), TransactionType.WITHDRAWAL);

            when(ledgerService.createTransaction(any(CreateTransactionRequest.class), eq(accountId)))
                    .thenThrow(new InsufficientBalanceException());

            MvcResult result = mockMvc.perform(post(BASE_URL + "/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ErrorResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

            assertThat(actual.status()).isEqualTo(400);
            assertThat(actual.message()).isEqualTo("Account has insufficient balance for this transaction");
            verify(ledgerService, times(1)).createTransaction(any(CreateTransactionRequest.class), eq(accountId));
        }

        @Test
        void shouldReturn400ForNegativeTransactionAmount() throws Exception {
            String accountId = "test-account-id";
            CreateTransactionRequest request = new CreateTransactionRequest(
                    new BigDecimal("-50.00"), TransactionType.DEPOSIT);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ErrorResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

            assertThat(actual.status()).isEqualTo(400);
            assertThat(actual.message()).contains("Amount must be greater than zero");
            verify(ledgerService, times(0)).createTransaction(any(), any());
        }

        @Test
        void shouldReturn400ForZeroTransactionAmount() throws Exception {
            String accountId = "test-account-id";
            CreateTransactionRequest request = new CreateTransactionRequest(
                    BigDecimal.ZERO, TransactionType.DEPOSIT);

            MvcResult result = mockMvc.perform(post(BASE_URL + "/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            ErrorResponse actual = objectMapper.readValue(result.getResponse().getContentAsString(), ErrorResponse.class);

            assertThat(actual.status()).isEqualTo(400);
            verify(ledgerService, times(0)).createTransaction(any(), any());
        }

        @Test
        void shouldReturn400ForNullAmount() throws Exception {
            String accountId = "test-account-id";
            String invalidRequest = "{\"type\": \"DEPOSIT\"}";

            mockMvc.perform(post(BASE_URL + "/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());

            verify(ledgerService, times(0)).createTransaction(any(), any());
        }

        @Test
        void shouldReturn400ForNullType() throws Exception {
            String accountId = "test-account-id";
            String invalidRequest = "{\"amount\": 100.00}";

            mockMvc.perform(post(BASE_URL + "/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());

            verify(ledgerService, times(0)).createTransaction(any(), any());
        }
    }
}
