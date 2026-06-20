package com.suraj.banking.auth.controller;

import com.suraj.banking.auth.dto.response.AccountResponse;
import com.suraj.banking.auth.dto.response.ApiResponse;
import com.suraj.banking.auth.entity.enums.AccountType;
import com.suraj.banking.auth.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Accounts", description = "Banking account management")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Open a new bank account")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @RequestParam AccountType accountType,
            @AuthenticationPrincipal UserDetails userDetails) {
        AccountResponse account = accountService.createAccount(userDetails.getUsername(), accountType);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account opened successfully", account));
    }

    @GetMapping
    @Operation(summary = "List all accounts belonging to the authenticated user")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getMyAccounts(userDetails.getUsername())));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get a specific account by ID")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success(accountService.getAccount(accountId, userDetails.getUsername())));
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "Close an account (soft delete)")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> closeAccount(
            @PathVariable Long accountId,
            @AuthenticationPrincipal UserDetails userDetails) {
        accountService.closeAccount(accountId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Account closed", null));
    }
}
