package com.suraj.banking.transaction.service.impl;

import com.suraj.banking.transaction.dto.request.TransferRequest;
import com.suraj.banking.transaction.dto.response.TransactionResponse;
import com.suraj.banking.transaction.entity.Transaction;
import com.suraj.banking.transaction.entity.TransactionStatus;
import com.suraj.banking.transaction.entity.TransactionType;
import com.suraj.banking.transaction.event.TransactionEventProducer;
import com.suraj.banking.transaction.repository.TransactionRepository;
import com.suraj.banking.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventProducer eventProducer;

    // Java 8 Predicate — reusable, composable validation logic
    private final Predicate<BigDecimal> isPositiveAmount =
            amount -> amount != null && amount.compareTo(BigDecimal.ZERO) > 0;

    private final Predicate<String> isNonEmptyString =
            s -> s != null && !s.trim().isEmpty();

    @Override
    @Transactional
    public TransactionResponse initiateTransfer(TransferRequest request) {
        if (!isPositiveAmount.test(request.getAmount())) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
        // Predicate.and() — composing predicates — Java 8
        if (!isNonEmptyString.and(s -> !s.equals(request.getToAccountId())).test(request.getFromAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts must differ");
        }

        Transaction transaction = Transaction.builder()
                .fromAccountId(request.getFromAccountId())
                .toAccountId(request.getToAccountId())
                .amount(request.getAmount())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.INITIATED)
                .description(request.getDescription())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction INITIATED: {} | Amount: {}", saved.getId(), saved.getAmount());
        eventProducer.publishTransactionInitiated(saved);

        // Mark as completed (production: balance debit/credit via event-sourcing or OpenFeign)
        saved.setStatus(TransactionStatus.COMPLETED);
        saved.setCompletedAt(LocalDateTime.now());
        Transaction completed = transactionRepository.save(saved);
        log.info("Transaction COMPLETED: {}", completed.getId());
        eventProducer.publishTransactionCompleted(completed);

        return mapToResponse(completed);
    }

    @Override
    public List<TransactionResponse> getTransactionsByAccountId(String accountId) {
        // Stream API + filter lambda + map method-reference + collect — Java 8
        return transactionRepository
                .findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId)
                .stream()
                .filter(t -> t.getStatus() != TransactionStatus.FAILED)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TransactionResponse getTransactionById(String id) {
        // Optional.orElseThrow + lambda — Java 8
        return transactionRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    private TransactionResponse mapToResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .fromAccountId(t.getFromAccountId())
                .toAccountId(t.getToAccountId())
                .amount(t.getAmount())
                .type(t.getType().name())
                .status(t.getStatus().name())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .completedAt(t.getCompletedAt())
                .build();
    }
}
