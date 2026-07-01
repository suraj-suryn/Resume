package com.suraj.banking.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Local DTO mirroring transaction-service's TransactionEvent.
 * Services stay independent — no shared libraries between microservices.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    private String eventType;
    private String transactionId;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String status;
    private String description;
    private long timestamp;
}
