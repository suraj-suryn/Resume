package com.suraj.banking.auth.service;

import com.suraj.banking.auth.dto.request.LoginRequest;
import com.suraj.banking.auth.dto.request.RegisterRequest;
import com.suraj.banking.auth.dto.response.TokenResponse;

public interface AuthService {
    TokenResponse register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse refreshToken(String refreshToken);
}
