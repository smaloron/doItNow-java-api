package com.example.doitnow.config;

import com.example.doitnow.service.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour WebSocketConfig")
class WebSocketConfigTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @Nested
    @DisplayName("Tests de configuration")
    class ConfigurationTests {

        @Test
        @DisplayName("Doit configurer le broker avec /topic et /app")
        void shouldConfigureMessageBroker() {
            var registry = mock(MessageBrokerRegistry.class);
            when(registry.enableSimpleBroker("/topic")).thenReturn(null);
            when(registry.setApplicationDestinationPrefixes("/app")).thenReturn(registry);

            webSocketConfig.configureMessageBroker(registry);

            verify(registry).enableSimpleBroker("/topic");
            verify(registry).setApplicationDestinationPrefixes("/app");
        }

        @Test
        @DisplayName("Doit enregistrer l'endpoint /ws avec SockJS")
        void shouldRegisterStompEndpoint() {
            var registry = mock(StompEndpointRegistry.class);
            var registration = mock(StompWebSocketEndpointRegistration.class);

            when(registry.addEndpoint("/ws")).thenReturn(registration);
            when(registration.setAllowedOriginPatterns("*")).thenReturn(registration);

            webSocketConfig.registerStompEndpoints(registry);

            verify(registry).addEndpoint("/ws");
            verify(registration).setAllowedOriginPatterns("*");
            verify(registration).withSockJS();
        }

        @Test
        @DisplayName("Doit enregistrer un intercepteur sur le canal entrant")
        void shouldRegisterChannelInterceptor() {
            var registration = mock(ChannelRegistration.class);

            webSocketConfig.configureClientInboundChannel(registration);

            verify(registration).interceptors(any());
        }
    }
}
