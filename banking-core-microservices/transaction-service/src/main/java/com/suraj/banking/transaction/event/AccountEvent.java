package com.suraj.banking.transaction.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Local DTO mirroring account-service's AccountEvent structure.
 * Each service defines its own event DTOs to stay independent (no shared libraries).
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
