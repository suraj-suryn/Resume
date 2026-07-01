package com.suraj.banking.transaction.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank(message = "Source account ID is required")
    @Schema(example = "acc-id-111")
    private String fromAccountId;

    @NotBlank(message = "Destination account ID is required")
    @Schema(example = "acc-id-222")
    private String toAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Transfer amount must be greater than zero")
    @Schema(example = "500.00")
    private BigDecimal amount;

    @Schema(example = "Monthly rent payment")
    private String description;
}
