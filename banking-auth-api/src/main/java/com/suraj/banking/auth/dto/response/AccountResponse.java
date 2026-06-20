package com.suraj.banking.auth.dto.response;

import com.suraj.banking.auth.entity.enums.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountResponse {

    private Long id;
    private String accountNumber;
    private BigDecimal balance;
    private AccountType accountType;
    private LocalDateTime createdAt;
    private boolean active;
}
