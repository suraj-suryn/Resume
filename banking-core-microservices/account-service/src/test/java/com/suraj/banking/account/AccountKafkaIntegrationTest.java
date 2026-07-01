package com.suraj.banking.account;

import com.suraj.banking.account.entity.Account;
import com.suraj.banking.account.entity.AccountType;
import com.suraj.banking.account.event.AccountEventProducer;
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
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: verifies AccountEventProducer publishes to the correct
 * Kafka topics with correctly structured JSON payloads.
 *
 * Design choices:
 *  - @EmbeddedKafka (bootstrapServersProperty): wires Spring Boot Kafka
 *    auto-configuration to use the in-process broker — no external Kafka needed.
 *  - spring.cloud.config.enabled=false: bypasses Config Server bootstrap so
 *    the test context starts without a running config-server.
 *  - eureka.client.enabled=false: prevents Eureka registration attempts.
 *  - Raw KafkaConsumer (not @KafkaListener) gives exact control over poll timing
 *    and avoids interference with any application-level consumer groups.
 *
 * Java 8 highlights:
 *  - Optional.ofNullable + filter + findFirst to locate the target record
 *  - try-with-resources for KafkaConsumer lifecycle management
 *  - Stream-based assertion on record value fields
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"account.created", "account.updated"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
        // Bypass Spring Cloud Config bootstrap — no config-server required in tests.
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.fail-fast=false",
        // Disable Eureka registration.
        "eureka.client.enabled=false",
        // H2 in-memory datasource for JPA.
        "spring.datasource.url=jdbc:h2:mem:account_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // Kafka producer serializer config (normally served by config-server).
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.add.type.headers=false"
})
class AccountKafkaIntegrationTest {

    @Autowired
    private AccountEventProducer accountEventProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    // ── Test 1: publishAccountCreated ─────────────────────────────────────────

    @Test
    void publishAccountCreated_shouldSendMessageToAccountCreatedTopic() {
        Account account = Account.builder()
                .id("test-acc-001")
                .accountNumber("ACC-11112222")
                .userId("user-001")
                .balance(BigDecimal.valueOf(5000))
                .accountType(AccountType.SAVINGS)
                .active(Boolean.TRUE)
                .build();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group-created", "true", embeddedKafkaBroker);

        // Java 8: try-with-resources — KafkaConsumer is AutoCloseable
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                consumerProps, new StringDeserializer(), new StringDeserializer())) {

            consumer.subscribe(Collections.singletonList("account.created"));

            accountEventProducer.publishAccountCreated(account);

            // Poll until the record arrives (up to 10 seconds).
            ConsumerRecord<String, String> received = pollUntilRecord(consumer, 10_000);

            assertThat(received).isNotNull();
            assertThat(received.topic()).isEqualTo("account.created");
            assertThat(received.key()).isEqualTo("test-acc-001");

            // Java 8: String contains checks on the JSON payload
            assertThat(received.value()).contains("ACCOUNT_CREATED");
            assertThat(received.value()).contains("ACC-11112222");
            assertThat(received.value()).contains("user-001");
            assertThat(received.value()).contains("5000");
        }
    }

    // ── Test 2: publishAccountUpdated ─────────────────────────────────────────

    @Test
    void publishAccountUpdated_shouldSendMessageToAccountUpdatedTopic() {
        Account account = Account.builder()
                .id("test-acc-002")
                .accountNumber("ACC-33334444")
                .userId("user-002")
                .balance(BigDecimal.valueOf(12_500))
                .accountType(AccountType.CHECKING)
                .active(Boolean.TRUE)
                .build();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group-updated", "true", embeddedKafkaBroker);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(
                consumerProps, new StringDeserializer(), new StringDeserializer())) {

            consumer.subscribe(Collections.singletonList("account.updated"));

            accountEventProducer.publishAccountUpdated(account);

            ConsumerRecord<String, String> received = pollUntilRecord(consumer, 10_000);

            assertThat(received).isNotNull();
            assertThat(received.topic()).isEqualTo("account.updated");
            assertThat(received.key()).isEqualTo("test-acc-002");
            assertThat(received.value()).contains("ACCOUNT_UPDATED");
            assertThat(received.value()).contains("ACC-33334444");
            assertThat(received.value()).contains("CHECKING");
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Java 8: polls the consumer until one record is found or the deadline passes.
     * Uses Optional.ofNullable + filter + findFirst to locate the record from
     * any given poll batch — avoids imperative nested loops.
     */
    private ConsumerRecord<String, String> pollUntilRecord(
            KafkaConsumer<String, String> consumer, long timeoutMs) {

        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            // Java 8: stream + findFirst on Iterable via StreamSupport
            Optional<ConsumerRecord<String, String>> found =
                    java.util.stream.StreamSupport
                            .stream(records.spliterator(), false)
                            .findFirst();
            if (found.isPresent()) {
                return found.get();
            }
        }
        return null;
    }
}
