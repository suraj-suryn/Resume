package com.suraj.banking.auth.service.impl;

import com.suraj.banking.auth.dto.response.AccountResponse;
import com.suraj.banking.auth.entity.Account;
import com.suraj.banking.auth.entity.User;
import com.suraj.banking.auth.entity.enums.AccountType;
import com.suraj.banking.auth.exception.AccountNotFoundException;
import com.suraj.banking.auth.exception.UnauthorizedAccessException;
import com.suraj.banking.auth.repository.AccountRepository;
import com.suraj.banking.auth.repository.UserRepository;
import com.suraj.banking.auth.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AccountResponse createAccount(String username, AccountType accountType, BigDecimal initialBalance) {
        User user = findUserOrThrow(username);

        Account account = Account.builder()
                .accountNumber(generateUniqueAccountNumber())
                .balance(initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) >= 0 ? initialBalance : BigDecimal.ZERO)
                .accountType(accountType)
                .user(user)
                .build();

        return mapToResponse(accountRepository.save(account));
    }

    @Override
    public List<AccountResponse> getMyAccounts(String username) {
        User user = findUserOrThrow(username);
        // Uses custom JPQL query — filters active only, ordered by creation date (Java 8 Stream + Collector)
        return accountRepository.findActiveAccountsByUser(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AccountResponse getAccount(Long accountId, String username) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        if (!account.getUser().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You do not own this account");
        }
        return mapToResponse(account);
    }

    @Override
    @Transactional
    public void closeAccount(Long accountId, String username) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found with id: " + accountId));

        if (!account.getUser().getUsername().equals(username)) {
            throw new UnauthorizedAccessException("You do not own this account");
        }
        account.setActive(false);
        accountRepository.save(account);
    }

    private User findUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    private String generateUniqueAccountNumber() {
        String number;
        do {
            // ACC + timestamp millis + 4-char UUID segment → guaranteed unique
            number = "ACC" + System.currentTimeMillis()
                    + UUID.randomUUID().toString().replace("-", "").substring(0, 4).toUpperCase();
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .accountType(account.getAccountType())
                .createdAt(account.getCreatedAt())
                .active(account.isActive())
                .build();
    }
}
