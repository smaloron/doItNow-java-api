package com.example.doitnow.service;

import com.example.doitnow.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires pour JwtService")
class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        user = User.builder()
                .id("user-1")
                .email("test@example.com")
                .password("encoded-password")
                .build();
    }

    @Test
    @DisplayName("Doit générer un token non null")
    void shouldGenerateToken() {
        String token = jwtService.generateToken(user);
        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    @DisplayName("Doit extraire le username du token")
    void shouldExtractUsername() {
        String token = jwtService.generateToken(user);
        String username = jwtService.extractUsername(token);
        assertEquals("test@example.com", username);
    }

    @Test
    @DisplayName("Doit valider un token valide")
    void shouldValidateToken() {
        String token = jwtService.generateToken(user);
        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    @DisplayName("Doit rejeter un token pour un autre utilisateur")
    void shouldRejectTokenForDifferentUser() {
        String token = jwtService.generateToken(user);
        User otherUser = User.builder().email("other@example.com").build();
        assertFalse(jwtService.isTokenValid(token, otherUser));
    }

    @Test
    @DisplayName("Doit générer un token avec extra claims")
    void shouldGenerateTokenWithExtraClaims() {
        var claims = java.util.Map.<String, Object>of("role", "ADMIN");
        String token = jwtService.generateToken(claims, user);
        assertNotNull(token);
        assertEquals("test@example.com", jwtService.extractUsername(token));
    }
}
