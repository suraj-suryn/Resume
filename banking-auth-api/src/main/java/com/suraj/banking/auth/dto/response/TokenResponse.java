package com.suraj.banking.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private String username;
    private String role;
}
