package com.suraj.banking.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(example = "john_doe")
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(example = "Secret@123")
    private String password;
}
