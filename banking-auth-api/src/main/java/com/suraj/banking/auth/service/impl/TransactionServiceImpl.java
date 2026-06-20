package com.suraj.banking.auth.service.impl;

import com.suraj.banking.auth.dto.request.TransferRequest;
import com.suraj.banking.auth.dto.response.TransactionResponse;
import com.suraj.banking.auth.entity.Account;
import com.suraj.banking.auth.entity.Transaction;
import com.suraj.banking.auth.entity.enums.TransactionType;
import com.suraj.banking.auth.exception.AccountNotFoundException;
import com.suraj.banking.auth.exception.InsufficientFundsException;
import com.suraj.banking.auth.exception.UnauthorizedAccessException;
import com.suraj.banking.auth.repository.AccountRepository;
import com.suraj.banking.auth.repository.TransactionRepository;
import com.suraj.banking.auth.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional  // Both account updates + transaction insert happen atomically
    public TransactionResponse transfer(Long fromAccountId, TransferRequest request, String username) {
        Account fromAccount = accountRepository.findById(fromAccountId)
                .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + fromAccountId));

        if (!fromAccount.getUser().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You do not own account: " + fromAccountId);
        }

        if (fromAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds. Available: " + fromAccount.getBalance()
                            + ", Requested: " + request.getAmount());
        }

        Account toAccount = accountRepository.findByAccountNumber(request.getTargetAccountNumber())
                .orElseThrow(() -> new AccountNotFoundException(
                        "Target account not found: " + request.getTargetAccountNumber()));

        // Atomic debit / credit
        fromAccount.setBalance(fromAccount.getBalance().subtract(request.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(request.getAmount()));
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transaction tx = Transaction.builder()
                .referenceNumber(generateReference())
                .amount(request.getAmount())
                .transactionType(TransactionType.TRANSFER)
                .description(request.getDescription())
                .account(fromAccount)
                .targetAccountNumber(request.getTargetAccountNumber())
                .build();

        return mapToResponse(transactionRepository.save(tx));
    }

    @Override
    public Page<TransactionResponse> getTransactionHistory(Long accountId, String username, Pageable pageable) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));

        if (!account.getUser().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You do not own account: " + accountId);
        }

        // Uses Spring Data Pageable — demonstrates pagination skills from resume
        return transactionRepository
                .findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(this::mapToResponse);
    }

    private String generateReference() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private TransactionResponse mapToResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .referenceNumber(tx.getReferenceNumber())
                .amount(tx.getAmount())
                .transactionType(tx.getTransactionType())
                .description(tx.getDescription())
                .targetAccountNumber(tx.getTargetAccountNumber())
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
