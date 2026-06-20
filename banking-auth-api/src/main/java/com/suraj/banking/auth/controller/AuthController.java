package com.suraj.banking.auth.controller;

import com.suraj.banking.auth.dto.request.LoginRequest;
import com.suraj.banking.auth.dto.request.RegisterRequest;
import com.suraj.banking.auth.dto.response.ApiResponse;
import com.suraj.banking.auth.dto.response.TokenResponse;
import com.suraj.banking.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, and token refresh")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user — returns JWT access + refresh tokens")
    public ResponseEntity<ApiResponse<TokenResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        TokenResponse token = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", token));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive JWT tokens")
    public ResponseEntity<ApiResponse<TokenResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange refresh token for a new access token")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @RequestHeader("Refresh-Token") String refreshToken) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authService.refreshToken(refreshToken)));
    }
}
