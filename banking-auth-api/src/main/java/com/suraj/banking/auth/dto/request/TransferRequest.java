package com.suraj.banking.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotBlank(message = "Target account number is required")
    @Schema(example = "ACC-7f3a2b1c")
    private String targetAccountNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Schema(example = "500.00")
    private BigDecimal amount;

    @Schema(example = "Monthly rent payment")
    private String description;
}
