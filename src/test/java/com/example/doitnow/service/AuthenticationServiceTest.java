package com.example.doitnow.service;

import com.example.doitnow.dto.auth.AuthenticationRequest;
import com.example.doitnow.dto.auth.AuthenticationResponse;
import com.example.doitnow.dto.auth.RegisterRequest;
import com.example.doitnow.model.User;
import com.example.doitnow.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour AuthenticationService")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("Doit inscrire un nouvel utilisateur et retourner un token")
    void shouldRegisterNewUser() {
        RegisterRequest request = RegisterRequest.builder()
                .email("new@example.com").password("password123").build();

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthenticationResponse response = authenticationService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Doit lever une exception si l'email existe déjà")
    void shouldThrowWhenEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com").password("password").build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authenticationService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Doit authentifier un utilisateur et retourner un token")
    void shouldAuthenticateUser() {
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("user@example.com").password("password").build();

        User user = User.builder().id("u1").email("user@example.com").password("encoded").build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthenticationResponse response = authenticationService.authenticate(request);

        assertEquals("jwt-token", response.getToken());
        verify(authenticationManager).authenticate(any());
    }
}
