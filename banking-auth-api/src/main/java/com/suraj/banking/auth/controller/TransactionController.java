package com.suraj.banking.auth.controller;

import com.suraj.banking.auth.dto.request.TransferRequest;
import com.suraj.banking.auth.dto.response.ApiResponse;
import com.suraj.banking.auth.dto.response.TransactionResponse;
import com.suraj.banking.auth.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Transactions", description = "Fund transfer and transaction history")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer/{fromAccountId}")
    @Operation(summary = "Transfer funds from one account to another")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @PathVariable Long fromAccountId,
            @Valid @RequestBody TransferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        TransactionResponse tx = transactionService.transfer(fromAccountId, request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer successful", tx));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get paginated transaction history for an account")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getHistory(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        Page<TransactionResponse> history = transactionService.getTransactionHistory(
                accountId, userDetails.getUsername(), PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
