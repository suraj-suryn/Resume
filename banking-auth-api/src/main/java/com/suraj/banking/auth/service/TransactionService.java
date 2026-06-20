package com.suraj.banking.auth.service;

import com.suraj.banking.auth.dto.request.TransferRequest;
import com.suraj.banking.auth.dto.response.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {
    TransactionResponse transfer(Long fromAccountId, TransferRequest request, String username);
    Page<TransactionResponse> getTransactionHistory(Long accountId, String username, Pageable pageable);
}
