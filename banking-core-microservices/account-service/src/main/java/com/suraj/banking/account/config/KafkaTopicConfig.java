package com.suraj.banking.account.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Explicit Kafka topic creation via @Bean NewTopic.
 * KafkaAdmin auto-creates these topics on startup if they don't exist.
 * Preferred over relying on auto.create.topics.enable (disabled in production).
 * Partitions=3 for parallel consumption; replicas=1 for local dev (set 3 in prod).
 * See Implementation-Notes.html #6 for full before/after analysis.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic accountCreatedTopic() {
        return TopicBuilder.name("account.created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic accountUpdatedTopic() {
        return TopicBuilder.name("account.updated")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
