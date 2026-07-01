package com.suraj.banking.transaction.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {

    private String eventType;       // TRANSACTION_INITIATED, TRANSACTION_COMPLETED
    private String transactionId;
    private String fromAccountId;
    private String toAccountId;
    private BigDecimal amount;
    private String status;
    private String description;
    private long timestamp;         // epoch millis
}
