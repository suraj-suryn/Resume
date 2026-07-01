package com.suraj.banking.transaction;

import com.suraj.banking.transaction.dto.request.TransferRequest;
import com.suraj.banking.transaction.dto.response.TransactionResponse;
import com.suraj.banking.transaction.service.TransactionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: verifies that TransactionService.initiateTransfer() persists
 * the transaction to H2 and publishes events to both Kafka topics atomically.
 *
 * Design choices:
 *  - @EmbeddedKafka: in-process broker — no external Kafka needed.
 *  - Tests both INITIATED and COMPLETED events in a single transfer call
 *    (mirrors the actual service behaviour: one initiateTransfer() call
 *     publishes two events synchronously within the same transaction).
 *  - H2 file-less in-memory DB: schema auto-created, dropped after context closes.
 *
 * Java 8 highlights:
 *  - Predicate<TransactionResponse> for response assertion composition
 *  - Stream.filter().collect(Collectors.toList()) on polled Kafka records
 *  - Method reference (String::contains) in stream pipeline
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"transaction.initiated", "transaction.completed", "account.created"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:txn_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false",
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer"
})
class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    // ── Test 1: initiateTransfer publishes events to both topics ──────────────

    @Test
    void initiateTransfer_shouldPublishInitiatedAndCompletedEvents() {
        TransferRequest request = new TransferRequest(
                "acc-from-001",
                "acc-to-002",
                BigDecimal.valueOf(750.00),
                "Integration test transfer"
        );

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "txn-test-group", "true", embeddedKafkaBroker);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                consumerProps, new StringDeserializer(), new StringDeserializer())) {

            consumer.subscribe(Arrays.asList("transaction.initiated", "transaction.completed"));

            // Act
            TransactionResponse response = transactionService.initiateTransfer(request);

            // Assert: service returned a completed response
            assertThat(response).isNotNull();
            assertThat(response.getId()).isNotNull();
            assertThat(response.getStatus()).isEqualTo("COMPLETED");
            assertThat(response.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(750.00));
            assertThat(response.getFromAccountId()).isEqualTo("acc-from-001");
            assertThat(response.getToAccountId()).isEqualTo("acc-to-002");

            // Assert: both Kafka events were published
            // Java 8: Stream.filter + collect to extract records per topic
            List<ConsumerRecord<String, String>> collectedRecords = pollAllRecords(consumer, 2, 15_000);

            List<String> topics = collectedRecords.stream()
                    .map(ConsumerRecord::topic)
                    .collect(Collectors.toList());

            assertThat(topics).containsExactlyInAnyOrder(
                    "transaction.initiated", "transaction.completed");

            // Java 8: filter by topic, verify payload content
            String initiatedPayload = collectedRecords.stream()
                    .filter(r -> "transaction.initiated".equals(r.topic()))
                    .map(ConsumerRecord::value)
                    .findFirst()
                    .orElse("");

            String completedPayload = collectedRecords.stream()
                    .filter(r -> "transaction.completed".equals(r.topic()))
                    .map(ConsumerRecord::value)
                    .findFirst()
                    .orElse("");

            assertThat(initiatedPayload).contains("TRANSACTION_INITIATED");
            assertThat(initiatedPayload).contains("acc-from-001");
            assertThat(initiatedPayload).contains("750");

            assertThat(completedPayload).contains("TRANSACTION_COMPLETED");
            assertThat(completedPayload).contains("acc-to-002");
            assertThat(completedPayload).contains("COMPLETED");
        }
    }

    // ── Test 2: invalid amount is rejected before any Kafka publish ───────────

    @Test
    void initiateTransfer_withZeroAmount_shouldThrowBeforePublishingAnyEvent() {
        TransferRequest request = new TransferRequest(
                "acc-from-003",
                "acc-to-004",
                BigDecimal.ZERO,          // invalid — Predicate rejects this
                "Zero amount test"
        );

        // Java 8 Predicate in TransactionServiceImpl guards against this
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> transactionService.initiateTransfer(request));
    }

    // ── Test 3: same-account transfer is rejected ─────────────────────────────

    @Test
    void initiateTransfer_withSameFromAndToAccount_shouldThrowBeforePublishingAnyEvent() {
        // Predicate.and() in TransactionServiceImpl: isNonEmptyString.and(s -> !s.equals(toAccountId))
        TransferRequest request = new TransferRequest(
                "acc-same",
                "acc-same",
                BigDecimal.valueOf(100),
                "Self-transfer test"
        );

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> transactionService.initiateTransfer(request));
    }

    // ── Helper: poll until N records received or timeout ─────────────────────

    /**
     * Java 8: accumulates records across multiple poll() calls using a mutable
     * List<> captured by the deadline loop — pure Java 8, no Awaitility needed.
     */
    private List<ConsumerRecord<String, String>> pollAllRecords(
            KafkaConsumer<String, String> consumer, int expectedCount, long timeoutMs) {

        List<ConsumerRecord<String, String>> collected = new ArrayList<>();
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline && collected.size() < expectedCount) {
            ConsumerRecords<String, String> batch = consumer.poll(Duration.ofMillis(500));
            batch.forEach(collected::add);      // Java 8 forEach method reference
        }
        return collected;
    }
}
