package com.suraj.banking.account.event;

import com.suraj.banking.account.entity.Account;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountEventProducer {

    private static final String TOPIC_ACCOUNT_CREATED = "account.created";
    private static final String TOPIC_ACCOUNT_UPDATED = "account.updated";

    // KafkaTemplate<String, Object> — avoids generic type mismatch with Spring Boot auto-config
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAccountCreated(Account account) {
        AccountEvent event = buildEvent(account, "ACCOUNT_CREATED");
        kafkaTemplate.send(TOPIC_ACCOUNT_CREATED, account.getId(), event)
                .addCallback(
                        result -> log.info("Published ACCOUNT_CREATED for account: {}", account.getAccountNumber()),
                        ex    -> log.error("Failed to publish ACCOUNT_CREATED for: {}", account.getAccountNumber(), ex)
                );
    }

    public void publishAccountUpdated(Account account) {
        AccountEvent event = buildEvent(account, "ACCOUNT_UPDATED");
        kafkaTemplate.send(TOPIC_ACCOUNT_UPDATED, account.getId(), event)
                .addCallback(
                        result -> log.info("Published ACCOUNT_UPDATED for account: {}", account.getAccountNumber()),
                        ex    -> log.error("Failed to publish ACCOUNT_UPDATED for: {}", account.getAccountNumber(), ex)
                );
    }

    private AccountEvent buildEvent(Account account, String eventType) {
        return AccountEvent.builder()
                .eventType(eventType)
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .userId(account.getUserId())
                .balance(account.getBalance())
                .accountType(account.getAccountType().name())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
