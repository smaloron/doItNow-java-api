package com.example.doitnow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests unitaires pour User")
class UserTest {

    @Test
    @DisplayName("getUsername doit retourner l'email")
    void shouldReturnEmailAsUsername() {
        User user = User.builder().email("test@example.com").build();
        assertEquals("test@example.com", user.getUsername());
    }

    @Test
    @DisplayName("getAuthorities doit retourner une liste vide")
    void shouldReturnEmptyAuthorities() {
        User user = new User();
        assertTrue(user.getAuthorities().isEmpty());
    }

    @Test
    @DisplayName("Les méthodes de statut du compte doivent retourner true")
    void shouldReturnTrueForAccountStatus() {
        User user = new User();
        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }
}
