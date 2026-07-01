package com.suraj.banking.transaction.consumer;

import com.suraj.banking.transaction.event.AccountEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumes account.created events to maintain a local cache of valid account IDs.
 *
 * Java 8 features demonstrated:
 * - ConcurrentHashMap.newKeySet() — thread-safe Set backed by ConcurrentHashMap
 * - Optional.ofNullable + ifPresent + lambda
 */
@Component
@Slf4j
public class AccountEventConsumer {

    // Thread-safe Set — Java 8 ConcurrentHashMap.newKeySet()
    private final Set<String> knownAccountIds = ConcurrentHashMap.newKeySet();

    @KafkaListener(topics = "account.created", groupId = "transaction-service-group")
    public void onAccountCreated(AccountEvent event) {
        log.info("Received ACCOUNT_CREATED event for account: {}", event.getAccountNumber());
        // Optional.ofNullable + ifPresent + lambda — Java 8
        Optional.ofNullable(event.getAccountId())
                .ifPresent(id -> {
                    knownAccountIds.add(id);
                    log.debug("Cached account ID: {} (total cached: {})", id, knownAccountIds.size());
                });
    }

    public boolean isKnownAccount(String accountId) {
        return knownAccountIds.contains(accountId);
    }
}
