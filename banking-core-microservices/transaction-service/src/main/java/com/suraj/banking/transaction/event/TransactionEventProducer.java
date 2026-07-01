package com.suraj.banking.transaction.event;

import com.suraj.banking.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionEventProducer {

    private static final String TOPIC_TRANSACTION_INITIATED = "transaction.initiated";
    private static final String TOPIC_TRANSACTION_COMPLETED = "transaction.completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTransactionInitiated(Transaction transaction) {
        TransactionEvent event = buildEvent(transaction, "TRANSACTION_INITIATED");
        kafkaTemplate.send(TOPIC_TRANSACTION_INITIATED, transaction.getId(), event)
                .addCallback(
                        r -> log.info("Published TRANSACTION_INITIATED: {}", transaction.getId()),
                        e -> log.error("Failed to publish TRANSACTION_INITIATED: {}", transaction.getId(), e)
                );
    }

    public void publishTransactionCompleted(Transaction transaction) {
        TransactionEvent event = buildEvent(transaction, "TRANSACTION_COMPLETED");
        kafkaTemplate.send(TOPIC_TRANSACTION_COMPLETED, transaction.getId(), event)
                .addCallback(
                        r -> log.info("Published TRANSACTION_COMPLETED: {}", transaction.getId()),
                        e -> log.error("Failed to publish TRANSACTION_COMPLETED: {}", transaction.getId(), e)
                );
    }

    private TransactionEvent buildEvent(Transaction t, String eventType) {
        return TransactionEvent.builder()
                .eventType(eventType)
                .transactionId(t.getId())
                .fromAccountId(t.getFromAccountId())
                .toAccountId(t.getToAccountId())
                .amount(t.getAmount())
                .status(t.getStatus().name())
                .description(t.getDescription())
                .timestamp(System.currentTimeMillis())
                .build();
    }
}
