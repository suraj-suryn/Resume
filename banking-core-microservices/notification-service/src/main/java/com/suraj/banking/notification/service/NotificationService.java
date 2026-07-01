package com.suraj.banking.notification.service;

import com.suraj.banking.notification.event.AccountEvent;
import com.suraj.banking.notification.event.TransactionEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class NotificationService {

    /**
     * Sends a completed-transfer notification asynchronously.
     *
     * Java 8 features: CompletableFuture.supplyAsync() + exceptionally()
     * In production: replace log statements with email/SMS/push service calls.
     */
    public void sendTransactionNotification(TransactionEvent event) {
        CompletableFuture.supplyAsync(() -> {
                    log.info("[NOTIFY] Transfer completed | Amount: {} | From: {} -> To: {} | Ref: {}",
                            event.getAmount(),
                            event.getFromAccountId(),
                            event.getToAccountId(),
                            event.getTransactionId());
                    return true;
                })
                .exceptionally(ex -> {
                    log.error("[NOTIFY] Failed to send transaction notification for: {}",
                            event.getTransactionId(), ex);
                    return false;
                });
    }

    /**
     * Sends an account-creation welcome notification asynchronously.
     *
     * Java 8: CompletableFuture chaining with exceptionally()
     */
    public void sendAccountCreatedNotification(AccountEvent event) {
        CompletableFuture.supplyAsync(() -> {
                    log.info("[NOTIFY] Account created | Account: {} | Type: {} | User: {}",
                            event.getAccountNumber(),
                            event.getAccountType(),
                            event.getUserId());
                    return true;
                })
                .exceptionally(ex -> {
                    log.error("[NOTIFY] Failed to send account creation notification for: {}",
                            event.getAccountNumber(), ex);
                    return false;
                });
    }
}
