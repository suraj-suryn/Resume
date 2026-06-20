package com.suraj.banking.auth.security;

import com.suraj.banking.auth.exception.InvalidTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "banking-auth-api-secret-key-256-bits-min-for-hmacsha256");
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpirationMs", 86400000L);

        userDetails = new User("testuser", "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void generateAccessToken_ShouldReturnNonEmptyToken() {
        String token = jwtService.generateAccessToken(userDetails);
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        String token = jwtService.generateAccessToken(userDetails);
        assertEquals("testuser", jwtService.extractUsername(token));
    }

    @Test
    void isTokenValid_WithCorrectUser_ShouldReturnTrue() {
        String token = jwtService.generateAccessToken(userDetails);
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_WithWrongUser_ShouldReturnFalse() {
        String token = jwtService.generateAccessToken(userDetails);
        UserDetails otherUser = new User("other", "pass",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    void extractUsername_WithInvalidToken_ShouldThrowInvalidTokenException() {
        assertThrows(InvalidTokenException.class, () ->
                jwtService.extractUsername("invalid.token.value"));
    }

    @Test
    void generateRefreshToken_ShouldContainCorrectSubject() {
        String refresh = jwtService.generateRefreshToken(userDetails);
        assertEquals("testuser", jwtService.extractUsername(refresh));
    }
}
