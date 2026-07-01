package com.suraj.banking.account.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountEvent {

    private String eventType;       // ACCOUNT_CREATED, ACCOUNT_UPDATED
    private String accountId;
    private String accountNumber;
    private String userId;
    private BigDecimal balance;
    private String accountType;
    private long timestamp;         // epoch millis — avoids Jackson LocalDateTime serialization config
}
