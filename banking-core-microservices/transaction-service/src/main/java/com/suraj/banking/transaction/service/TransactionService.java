package com.suraj.banking.transaction.service;

import com.suraj.banking.transaction.dto.request.TransferRequest;
import com.suraj.banking.transaction.dto.response.TransactionResponse;

import java.util.List;

public interface TransactionService {

    TransactionResponse initiateTransfer(TransferRequest request);

    List<TransactionResponse> getTransactionsByAccountId(String accountId);

    TransactionResponse getTransactionById(String id);
}
