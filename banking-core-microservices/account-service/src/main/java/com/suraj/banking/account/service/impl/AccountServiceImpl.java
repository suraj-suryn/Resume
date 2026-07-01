package com.suraj.banking.account.service.impl;

import com.suraj.banking.account.dto.request.CreateAccountRequest;
import com.suraj.banking.account.dto.response.AccountResponse;
import com.suraj.banking.account.entity.Account;
import com.suraj.banking.account.event.AccountEventProducer;
import com.suraj.banking.account.exception.AccountNotFoundException;
import com.suraj.banking.account.repository.AccountRepository;
import com.suraj.banking.account.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountEventProducer eventProducer;

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = Account.builder()
                .userId(request.getUserId())
                .accountType(request.getAccountType())
                // Optional.ofNullable + orElse — Java 8
                .balance(Optional.ofNullable(request.getInitialBalance()).orElse(BigDecimal.ZERO))
                .build();

        Account saved = accountRepository.save(account);
        log.info("Account created: {} for user: {}", saved.getAccountNumber(), saved.getUserId());
        eventProducer.publishAccountCreated(saved);
        return mapToResponse(saved);
    }

    @Override
    public AccountResponse getAccountById(String id) {
        // Optional.map + orElseThrow — Java 8
        return accountRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
    }

    @Override
    public AccountResponse getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(this::mapToResponse)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
    }

    @Override
    public List<AccountResponse> getAccountsByUserId(String userId) {
        // Stream API + filter lambda + Comparator.comparing method reference + map + collect — Java 8
        return accountRepository.findByUserIdAndActive(userId, true)
                .stream()
                .filter(account -> account.getBalance().compareTo(BigDecimal.ZERO) >= 0)
                .sorted(Comparator.comparing(Account::getCreatedAt).reversed())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AccountResponse updateBalance(String accountNumber, BigDecimal newBalance) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        account.setBalance(newBalance);
        Account updated = accountRepository.save(account);
        eventProducer.publishAccountUpdated(updated);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deactivateAccount(String id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + id));
        account.setActive(false);
        accountRepository.save(account);
        log.info("Account deactivated: {}", account.getAccountNumber());
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .accountType(account.getAccountType().name())
                .userId(account.getUserId())
                .active(account.getActive())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
