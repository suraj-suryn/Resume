package com.suraj.banking.auth.dto.response;

import com.suraj.banking.auth.entity.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {

    private Long id;
    private String referenceNumber;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String description;
    private String targetAccountNumber;
    private LocalDateTime createdAt;
}
