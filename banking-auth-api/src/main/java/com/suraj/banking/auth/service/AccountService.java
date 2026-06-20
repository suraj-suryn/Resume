package com.suraj.banking.auth.service;

import com.suraj.banking.auth.dto.response.AccountResponse;
import com.suraj.banking.auth.entity.enums.AccountType;

import java.util.List;

public interface AccountService {
    AccountResponse createAccount(String username, AccountType accountType);
    List<AccountResponse> getMyAccounts(String username);
    AccountResponse getAccount(Long accountId, String username);
    void closeAccount(Long accountId, String username);
}
