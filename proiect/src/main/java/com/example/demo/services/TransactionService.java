package com.example.demo.services;

import com.example.demo.domain.Transaction;
import com.example.demo.domain.User;
import com.example.demo.dto.*;

public interface TransactionService {
    Transaction initiatePayment(PaymentRequestDTO paymentRequest, User user);
    Transaction authorizePayment(int transactionId, String password, User user);
    void executeTransaction(int transactionId);
    Transaction transferBetweenOwnAccounts(OwnAccountTransferDTO dto, User user);
    Transaction performCurrencyExchange(CurrencyExchangeDTO dto, User user);
    PageResponseDTO<TransactionSummaryDTO> getTransactionsForUserPaged(int userId, int page, int size, String sortBy, String direction);
    PageResponseDTO<TransactionSummaryDTO> getTransactionsForAccountPaged(Long accountId, int userId, int page, int size, String sortBy, String direction);
}