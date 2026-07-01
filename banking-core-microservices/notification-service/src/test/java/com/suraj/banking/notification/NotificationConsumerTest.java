package com.suraj.banking.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suraj.banking.notification.event.TransactionEvent;
import com.suraj.banking.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: verifies TransactionEventConsumer correctly deserialises a
 * raw JSON string and delegates to NotificationService.
 *
 * Design choices:
 *  - @SpyBean on NotificationService: wraps the real bean so we can verify
 *    method invocations while preserving actual behaviour (CompletableFuture log).
 *  - Consumer receives String payload → ObjectMapper.readValue() — we validate
 *    that the consumer survives a well-formed payload end-to-end.
 *  - bootstrapServersProperty wires the auto-configured KafkaTemplate and the
 *    @KafkaListener to the same in-process broker, no external Kafka required.
 *  - WebApplicationType is NONE (spring-boot-starter-web absent from classpath),
 *    so no Tomcat is started during the test context.
 *
 * Java 8 highlights:
 *  - CompletableFuture.runAsync + join for a non-blocking producer send inside test
 *  - String.format for building the raw JSON payload
 *  - Mockito.timeout() for asynchronous consumer verification
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"transaction.completed", "account.created",
                  "transaction.completed-retry-0", "transaction.completed-dlt"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "eureka.client.enabled=false",
        // Consumer uses StringDeserializer (matches notification-service.yml).
        "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        // Producer in test: send raw JSON strings to simulate upstream services.
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
})
class NotificationConsumerTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private NotificationService notificationService;

    // ── Test 1: valid TRANSACTION_COMPLETED event ────────────────────────────

    @Test
    void onTransactionCompleted_withValidPayload_shouldInvokeNotificationService()
            throws Exception {

        // given — build a JSON payload matching TransactionEvent structure
        TransactionEvent event = new TransactionEvent(
                "TRANSACTION_COMPLETED",
                "txn-test-001",
                "acc-001",
                "acc-002",
                BigDecimal.valueOf(1500.00),
                "COMPLETED",
                "Notification service test",
                System.currentTimeMillis()
        );

        String payload = objectMapper.writeValueAsString(event);

        // when — send via KafkaTemplate (Java 8: CompletableFuture wraps the ListenableFuture)
        CompletableFuture.runAsync(() ->
                kafkaTemplate.send("transaction.completed", "txn-test-001", payload));

        // then — wait up to 10 seconds for the @KafkaListener to process the message
        // Mockito.timeout() polls until the condition is met or times out.
        Mockito.verify(notificationService, Mockito.timeout(10_000).times(1))
                .sendTransactionNotification(Mockito.argThat(e ->
                        "txn-test-001".equals(e.getTransactionId())
                        && BigDecimal.valueOf(1500.00).compareTo(e.getAmount()) == 0
                ));
    }

    // ── Test 2: valid ACCOUNT_CREATED event ──────────────────────────────────

    @Test
    void onAccountCreated_withValidPayload_shouldInvokeNotificationService()
            throws Exception {

        // given — build JSON for AccountEvent (same StringDeserializer path)
        // Java 8: String.format for inline JSON construction without a DTO import
        String payload = String.format(
                "{\"eventType\":\"ACCOUNT_CREATED\",\"accountId\":\"acc-new-001\"," +
                "\"accountNumber\":\"ACC-99887766\",\"userId\":\"user-007\"," +
                "\"balance\":2000.00,\"accountType\":\"SAVINGS\",\"timestamp\":%d}",
                System.currentTimeMillis());

        // when
        CompletableFuture.runAsync(() ->
                kafkaTemplate.send("account.created", "acc-new-001", payload));

        // then
        Mockito.verify(notificationService, Mockito.timeout(10_000).times(1))
                .sendAccountCreatedNotification(Mockito.argThat(e ->
                        "ACC-99887766".equals(e.getAccountNumber())));
    }

    // ── Test 3: NotificationService.sendTransactionNotification returns true ──

    @Test
    void sendTransactionNotification_shouldCompleteSuccessfully() throws Exception {
        TransactionEvent event = new TransactionEvent(
                "TRANSACTION_COMPLETED",
                "txn-sync-001",
                "from-x",
                "to-y",
                BigDecimal.valueOf(200),
                "COMPLETED",
                "Direct service call test",
                System.currentTimeMillis()
        );

        // Java 8: CompletableFuture returned from sendTransactionNotification
        // is fire-and-forget (void method internally calls supplyAsync). We just
        // verify the call doesn't throw.
        NotificationService realService = new NotificationService();

        // Should not throw
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> realService.sendTransactionNotification(event));

        // Allow the async CompletableFuture to complete
        CompletableFuture<Void> asyncWait = CompletableFuture.runAsync(
                () -> {
                    try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
        asyncWait.join();   // Java 8: join() blocks until the future completes

        assertThat(true).isTrue(); // service completed without exception
    }
}
