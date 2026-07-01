package com.suraj.banking.notification.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Local DTO mirroring account-service's AccountEvent.
 * Services stay independent — no shared libraries between microservices.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountEvent {

    private String eventType;
    private String accountId;
    private String accountNumber;
    private String userId;
    private BigDecimal balance;
    private String accountType;
    private long timestamp;
}
