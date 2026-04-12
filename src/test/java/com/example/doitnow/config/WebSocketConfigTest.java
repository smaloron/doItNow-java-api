package com.example.doitnow.config;

import com.example.doitnow.model.User;
import com.example.doitnow.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.junit.jupiter.api.Assertions.*;
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

    @Captor
    private ArgumentCaptor<ChannelInterceptor> interceptorCaptor;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private ChannelInterceptor captureInterceptor() {
        var registration = mock(ChannelRegistration.class);
        webSocketConfig.configureClientInboundChannel(registration);
        verify(registration).interceptors(interceptorCaptor.capture());
        return interceptorCaptor.getValue();
    }

    private Message<?> buildConnectMessage(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        if (authHeader != null) {
            accessor.addNativeHeader("Authorization", authHeader);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildNonConnectMessage(StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

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

    @Nested
    @DisplayName("Tests de l'intercepteur d'authentification WebSocket")
    class WebSocketAuthInterceptorTests {

        @Test
        @DisplayName("Doit authentifier avec un token JWT valide")
        void shouldAuthenticateWithValidToken() {
            User user = new User();
            user.setId("user-1");
            user.setEmail("test@example.com");

            when(jwtService.extractUsername("valid-token")).thenReturn("test@example.com");
            when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(user);
            when(jwtService.isTokenValid("valid-token", user)).thenReturn(true);

            ChannelInterceptor interceptor = captureInterceptor();
            Message<?> connectMessage = buildConnectMessage("Bearer valid-token");

            Message<?> result = interceptor.preSend(connectMessage, mock(MessageChannel.class));

            assertNotNull(result);
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
            assertNotNull(accessor.getUser());
            assertInstanceOf(UsernamePasswordAuthenticationToken.class, accessor.getUser());

            var auth = (UsernamePasswordAuthenticationToken) accessor.getUser();
            assertEquals(user, auth.getPrincipal());
        }

        @Test
        @DisplayName("Ne doit pas authentifier avec un token JWT invalide")
        void shouldNotAuthenticateWithInvalidToken() {
            User user = new User();
            user.setEmail("test@example.com");

            when(jwtService.extractUsername("invalid-token")).thenReturn("test@example.com");
            when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(user);
            when(jwtService.isTokenValid("invalid-token", user)).thenReturn(false);

            ChannelInterceptor interceptor = captureInterceptor();
            Message<?> connectMessage = buildConnectMessage("Bearer invalid-token");

            Message<?> result = interceptor.preSend(connectMessage, mock(MessageChannel.class));

            assertNotNull(result);
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
            assertNull(accessor.getUser());
        }

        @Test
        @DisplayName("Ne doit pas authentifier sans header Authorization")
        void shouldNotAuthenticateWithoutAuthHeader() {
            ChannelInterceptor interceptor = captureInterceptor();
            Message<?> connectMessage = buildConnectMessage(null);

            Message<?> result = interceptor.preSend(connectMessage, mock(MessageChannel.class));

            assertNotNull(result);
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
            assertNull(accessor.getUser());
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Ne doit pas authentifier avec un header Authorization mal formé")
        void shouldNotAuthenticateWithMalformedAuthHeader() {
            ChannelInterceptor interceptor = captureInterceptor();
            Message<?> connectMessage = buildConnectMessage("Basic some-credentials");

            Message<?> result = interceptor.preSend(connectMessage, mock(MessageChannel.class));

            assertNotNull(result);
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
            assertNull(accessor.getUser());
            verifyNoInteractions(jwtService);
        }

        @Test
        @DisplayName("Ne doit pas authentifier quand extractUsername retourne null")
        void shouldNotAuthenticateWhenUsernameIsNull() {
            when(jwtService.extractUsername("bad-token")).thenReturn(null);

            ChannelInterceptor interceptor = captureInterceptor();
            Message<?> connectMessage = buildConnectMessage("Bearer bad-token");

            Message<?> result = interceptor.preSend(connectMessage, mock(MessageChannel.class));

            assertNotNull(result);
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
            assertNull(accessor.getUser());
            verifyNoInteractions(userDetailsService);
        }

        @Test
        @DisplayName("Ne doit pas traiter l'auth pour les commandes non-CONNECT")
        void shouldIgnoreNonConnectCommands() {
            ChannelInterceptor interceptor = captureInterceptor();

            Message<?> subscribeMessage = buildNonConnectMessage(StompCommand.SUBSCRIBE);
            Message<?> result = interceptor.preSend(subscribeMessage, mock(MessageChannel.class));

            assertNotNull(result);
            verifyNoInteractions(jwtService);
            verifyNoInteractions(userDetailsService);
        }

        @Test
        @DisplayName("Doit positionner le SecurityContext après authentification")
        void shouldSetSecurityContext() {
            User user = new User();
            user.setId("user-1");
            user.setEmail("test@example.com");

            when(jwtService.extractUsername("valid-token")).thenReturn("test@example.com");
            when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(user);
            when(jwtService.isTokenValid("valid-token", user)).thenReturn(true);

            ChannelInterceptor interceptor = captureInterceptor();
            Message<?> connectMessage = buildConnectMessage("Bearer valid-token");

            interceptor.preSend(connectMessage, mock(MessageChannel.class));

            var securityAuth = SecurityContextHolder.getContext().getAuthentication();
            assertNotNull(securityAuth);
            assertEquals(user, securityAuth.getPrincipal());
        }
    }
}
