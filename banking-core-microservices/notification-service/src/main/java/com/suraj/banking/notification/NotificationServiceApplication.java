package com.suraj.banking.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Pure Kafka consumer — no web server, no Eureka registration.
 * Spring Boot detects web-application-type=NONE automatically
 * since spring-boot-starter-web is not on the classpath.
 * See Implementation-Notes.html #5 for full design rationale.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
