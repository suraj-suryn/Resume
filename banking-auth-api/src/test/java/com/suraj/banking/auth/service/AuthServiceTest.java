package com.suraj.banking.auth.service;

import com.suraj.banking.auth.dto.request.LoginRequest;
import com.suraj.banking.auth.dto.request.RegisterRequest;
import com.suraj.banking.auth.dto.response.TokenResponse;
import com.suraj.banking.auth.entity.User;
import com.suraj.banking.auth.entity.enums.Role;
import com.suraj.banking.auth.exception.DuplicateUserException;
import com.suraj.banking.auth.repository.UserRepository;
import com.suraj.banking.auth.security.CustomUserDetailsService;
import com.suraj.banking.auth.security.JwtService;
import com.suraj.banking.auth.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L).username("testuser").email("test@example.com")
                .password("encoded").role(Role.USER).active(true).build();

        mockUserDetails = new org.springframework.security.core.userdetails.User(
                "testuser", "encoded",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void register_WithNewUser_ShouldReturnTokenResponse() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mockUserDetails);
        when(jwtService.generateAccessToken(mockUserDetails)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(mockUserDetails)).thenReturn("refresh-token");

        TokenResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("testuser", response.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WithExistingUsername_ShouldThrowDuplicateUserException() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        assertThrows(DuplicateUserException.class, () -> authService.register(req));
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnTokenResponse() {
        LoginRequest req = new LoginRequest();
        req.setUsername("testuser");
        req.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mockUserDetails);
        when(jwtService.generateAccessToken(mockUserDetails)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(mockUserDetails)).thenReturn("refresh-token");

        TokenResponse response = authService.login(req);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
    }
}
