package com.suraj.banking.account.controller;

import com.suraj.banking.account.dto.request.CreateAccountRequest;
import com.suraj.banking.account.dto.response.AccountResponse;
import com.suraj.banking.account.dto.response.ApiResponse;
import com.suraj.banking.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "APIs for managing bank accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "Create a new bank account")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @RequestBody @Valid CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(accountService.createAccount(request), "Account created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<ApiResponse<AccountResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountById(id), "Account retrieved"));
    }

    @GetMapping("/number/{accountNumber}")
    @Operation(summary = "Get account by account number")
    public ResponseEntity<ApiResponse<AccountResponse>> getByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountByNumber(accountNumber), "Account retrieved"));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all active accounts for a user")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAccountsByUserId(userId), "Accounts retrieved"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate an account")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable String id) {
        accountService.deactivateAccount(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Account deactivated successfully"));
    }
}
