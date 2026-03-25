package com.example.doitnow.service;

import com.example.doitnow.model.User;
import com.example.doitnow.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Doit inscrire un utilisateur avec mot de passe encodé")
    void shouldRegisterUser() {
        User user = new User();
        user.setId("should-be-null");
        user.setEmail("test@example.com");
        user.setPassword("plain");

        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId("generated-id");
            return u;
        });

        User result = userService.register(user);

        assertEquals("encoded", result.getPassword());
        assertEquals("generated-id", result.getId());
        verify(passwordEncoder).encode("plain");
    }

    @Test
    @DisplayName("Doit charger un utilisateur par email")
    void shouldLoadUserByUsername() {
        User user = User.builder().id("u1").email("test@example.com").password("encoded").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails result = userService.loadUserByUsername("test@example.com");

        assertEquals("test@example.com", result.getUsername());
    }

    @Test
    @DisplayName("Doit lever UsernameNotFoundException si l'utilisateur n'existe pas")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("unknown@example.com"));
    }
}
