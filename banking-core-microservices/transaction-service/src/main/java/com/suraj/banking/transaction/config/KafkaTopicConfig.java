package com.suraj.banking.transaction.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Explicit Kafka topic creation via @Bean NewTopic.
 * KafkaAdmin auto-creates these topics on startup if missing.
 * See Implementation-Notes.html #6 for full before/after analysis.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic transactionInitiatedTopic() {
        return TopicBuilder.name("transaction.initiated")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic transactionCompletedTopic() {
        return TopicBuilder.name("transaction.completed")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
