package com.suraj.banking.auth.service.impl;

import com.suraj.banking.auth.dto.request.LoginRequest;
import com.suraj.banking.auth.dto.request.RegisterRequest;
import com.suraj.banking.auth.dto.response.TokenResponse;
import com.suraj.banking.auth.entity.User;
import com.suraj.banking.auth.entity.enums.Role;
import com.suraj.banking.auth.exception.DuplicateUserException;
import com.suraj.banking.auth.exception.InvalidTokenException;
import com.suraj.banking.auth.repository.UserRepository;
import com.suraj.banking.auth.security.CustomUserDetailsService;
import com.suraj.banking.auth.security.JwtService;
import com.suraj.banking.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateUserException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        return buildTokenResponse(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails),
                user
        );
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        // AuthenticationManager validates credentials — throws BadCredentialsException on failure,
        // which GlobalExceptionHandler catches and returns as a structured 401.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        return buildTokenResponse(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails),
                user
        );
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new InvalidTokenException("Refresh token is invalid or has expired");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidTokenException("User not found"));

        return buildTokenResponse(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails),
                user
        );
    }

    private TokenResponse buildTokenResponse(String accessToken, String refreshToken, User user) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600)
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }
}
