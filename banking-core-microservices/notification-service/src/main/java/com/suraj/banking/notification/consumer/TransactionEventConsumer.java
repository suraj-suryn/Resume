package com.suraj.banking.notification.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suraj.banking.notification.event.AccountEvent;
import com.suraj.banking.notification.event.TransactionEvent;
import com.suraj.banking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for notification events.
 *
 * @RetryableTopic: retries failed messages 3 times (attempts=4: 1 initial + 3 retries)
 * with exponential backoff. After all attempts exhausted, message is routed to
 * the dead-letter topic ({topic}-dlt) for manual inspection via Kafdrop UI.
 * Non-blocking: failed messages don't block other partition messages.
 *
 * String deserialization used (not JsonDeserializer) to avoid type-header issues
 * when consuming events from multiple services with different event classes.
 *
 * See Implementation-Notes.html #9 for full before/after analysis.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 1.5),
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "transaction.completed", groupId = "notification-service-group")
    public void onTransactionCompleted(String payload) throws JsonProcessingException {
        log.info("Received message on transaction.completed topic");
        TransactionEvent event = objectMapper.readValue(payload, TransactionEvent.class);
        log.info("Processing TRANSACTION_COMPLETED event: {}", event.getTransactionId());
        notificationService.sendTransactionNotification(event);
    }

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 1.5),
            autoCreateTopics = "true"
    )
    @KafkaListener(topics = "account.created", groupId = "notification-service-group")
    public void onAccountCreated(String payload) throws JsonProcessingException {
        log.info("Received message on account.created topic");
        AccountEvent event = objectMapper.readValue(payload, AccountEvent.class);
        log.info("Processing ACCOUNT_CREATED event: {}", event.getAccountNumber());
        notificationService.sendAccountCreatedNotification(event);
    }

    /**
     * DLT handler — invoked automatically when all retry attempts are exhausted.
     * The failed message lands in the dead-letter topic for inspection via Kafdrop.
     * In production: alert on-call, persist to dead_letter_events table, trigger incident.
     */
    @DltHandler
    public void handleDeadLetter(String payload) {
        log.error("[DLT] Message exhausted all retry attempts and was routed to DLT. Payload: {}", payload);
    }
}
