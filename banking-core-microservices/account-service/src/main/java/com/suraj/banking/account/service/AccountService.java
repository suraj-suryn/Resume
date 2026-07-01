package com.suraj.banking.account.service;

import com.suraj.banking.account.dto.request.CreateAccountRequest;
import com.suraj.banking.account.dto.response.AccountResponse;

import java.math.BigDecimal;
import java.util.List;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccountById(String id);

    AccountResponse getAccountByNumber(String accountNumber);

    List<AccountResponse> getAccountsByUserId(String userId);

    AccountResponse updateBalance(String accountNumber, BigDecimal newBalance);

    void deactivateAccount(String id);
}
