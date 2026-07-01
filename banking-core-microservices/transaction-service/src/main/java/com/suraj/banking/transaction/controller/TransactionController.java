package com.suraj.banking.transaction.controller;

import com.suraj.banking.transaction.dto.request.TransferRequest;
import com.suraj.banking.transaction.dto.response.ApiResponse;
import com.suraj.banking.transaction.dto.response.TransactionResponse;
import com.suraj.banking.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "APIs for fund transfers and transaction history")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    @Operation(summary = "Initiate a fund transfer between two accounts")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @RequestBody @Valid TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(transactionService.initiateTransfer(request), "Transfer completed successfully"));
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get transaction history for an account")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getByAccountId(
            @PathVariable String accountId) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getTransactionsByAccountId(accountId), "Transactions retrieved"));
    }

    @GetMapping("/detail/{id}")
    @Operation(summary = "Get a specific transaction by ID")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getTransactionById(id), "Transaction retrieved"));
    }
}
