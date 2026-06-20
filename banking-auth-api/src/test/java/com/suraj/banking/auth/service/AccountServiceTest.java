package com.suraj.banking.auth.service;

import com.suraj.banking.auth.dto.response.AccountResponse;
import com.suraj.banking.auth.entity.Account;
import com.suraj.banking.auth.entity.User;
import com.suraj.banking.auth.entity.enums.AccountType;
import com.suraj.banking.auth.entity.enums.Role;
import com.suraj.banking.auth.exception.AccountNotFoundException;
import com.suraj.banking.auth.exception.UnauthorizedAccessException;
import com.suraj.banking.auth.repository.AccountRepository;
import com.suraj.banking.auth.repository.UserRepository;
import com.suraj.banking.auth.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User mockUser;
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L).username("testuser").email("test@example.com")
                .role(Role.USER).active(true).build();

        mockAccount = Account.builder()
                .id(1L).accountNumber("ACC123456")
                .balance(BigDecimal.valueOf(1000))
                .accountType(AccountType.SAVINGS)
                .user(mockUser)
                .createdAt(LocalDateTime.now())
                .active(true).build();
    }

    @Test
    void createAccount_ShouldReturnAccountResponse() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);

        AccountResponse response = accountService.createAccount("testuser", AccountType.SAVINGS, new java.math.BigDecimal("1000.00"));

        assertNotNull(response);
        assertEquals("ACC123456", response.getAccountNumber());
        assertEquals(AccountType.SAVINGS, response.getAccountType());
    }

    @Test
    void getMyAccounts_ShouldReturnListOfAccounts() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(accountRepository.findActiveAccountsByUser(1L))
                .thenReturn(Collections.singletonList(mockAccount));

        List<AccountResponse> accounts = accountService.getMyAccounts("testuser");

        assertEquals(1, accounts.size());
        assertEquals("ACC123456", accounts.get(0).getAccountNumber());
    }

    @Test
    void getAccount_WhenCallerDoesNotOwnAccount_ShouldThrowUnauthorizedAccess() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(mockAccount));

        assertThrows(UnauthorizedAccessException.class,
                () -> accountService.getAccount(1L, "differentuser"));
    }

    @Test
    void getAccount_WhenAccountNotFound_ShouldThrowAccountNotFoundException() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class,
                () -> accountService.getAccount(99L, "testuser"));
    }
}
