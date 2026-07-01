package com.suraj.banking.notification.config;

import org.springframework.context.annotation.Configuration;

/**
 * Kafka configuration for notification-service.
 * @RetryableTopic is auto-enabled by Spring Boot 2.7.x + spring-kafka 2.8.x
 * auto-configuration — @EnableRetryTopic is NOT needed (added in 2.9+).
 */
@Configuration
public class KafkaConfig {
}
