package com.example.transactionservice.services;

import com.example.transactionservice.domain.Transaction;
import com.example.transactionservice.dto.*;

public interface TransactionService {
    Transaction initiatePayment(PaymentRequestDTO paymentRequest, Integer userId);
    Transaction authorizePayment(Long transactionId, String password, Integer userId);
    void executeTransaction(Long transactionId);
    Transaction transferBetweenOwnAccounts(OwnAccountTransferDTO dto, Integer userId);
    Transaction performCurrencyExchange(CurrencyExchangeDTO dto, Integer userId);
    PageResponseDTO<TransactionSummaryDTO> getTransactionsForUserPaged(int userId, int page, int size, String sortBy, String direction);
    PageResponseDTO<TransactionSummaryDTO> getTransactionsForAccountPaged(Long accountId, int userId, int page, int size, String sortBy, String direction);
}
