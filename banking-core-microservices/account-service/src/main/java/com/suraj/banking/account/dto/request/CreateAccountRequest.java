package com.suraj.banking.account.dto.request;

import com.suraj.banking.account.entity.AccountType;
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
public class CreateAccountRequest {

    @NotBlank(message = "User ID is required")
    @Schema(example = "user-abc-123")
    private String userId;

    @NotNull(message = "Account type is required")
    @Schema(example = "SAVINGS", allowableValues = {"SAVINGS", "CHECKING", "CURRENT"})
    private AccountType accountType;

    @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance cannot be negative")
    @Schema(example = "1000.00", description = "Optional initial deposit amount (defaults to 0.00)")
    private BigDecimal initialBalance;
}
