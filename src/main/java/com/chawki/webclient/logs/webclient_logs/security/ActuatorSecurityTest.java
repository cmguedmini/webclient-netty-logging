package com.chawki.webclient.logs.webclient_logs.security;

package com.chawki.webclient.logs.webclient_logs.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour valider la configuration de sécurité des endpoints
 * selon la configuration JEFSecurityProperties.
 */
@ExtendWith(MockitoExtension.class)
class ActuatorSecurityTest {

    @Mock
    private BasicAuthenticationProvider basicAuthenticationProvider;

    @Mock
    private TokenAuthenticationProvider tokenAuthenticationProvider;

    @Mock
    private JEFSecurityProperties jefSecurityProperties;

    @Mock
    private HttpSecurity httpSecurity;

    @Mock
    private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authzRegistry;

    @Mock
    private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl authorizedUrl;

    @InjectMocks
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        // Configuration des mocks de base
        when(authzRegistry.requestMatchers(any(String.class))).thenReturn(authorizedUrl);
        when(authorizedUrl.permitAll()).thenReturn(authzRegistry);
        when(authorizedUrl.authenticated()).thenReturn(authzRegistry);
        when(authorizedUrl.hasRole(any(String.class))).thenReturn(authzRegistry);
        when(authorizedUrl.hasAnyRole(any(String[].class))).thenReturn(authzRegistry);
        when(authorizedUrl.anyRequest()).thenReturn(authorizedUrl);
        when(authzRegistry.anyRequest()).thenReturn(authorizedUrl);
    }

    @Test
    void testEndpointSecurityWithPermitAll() {
        // Given
        String endpoint = "/api/public/**";
        JEFSecurityLevel securityLevel = JEFSecurityLevel.permitAll();

        // When
        invokeConfigureEndpointSecurity(endpoint, securityLevel);

        // Then
        verify(authzRegistry).requestMatchers(endpoint);
        verify(authorizedUrl).permitAll();
        verify(authorizedUrl, never()).authenticated();
        verify(authorizedUrl, never()).hasAnyRole(any(String[].class));
    }

    @Test
    void testEndpointSecurityWithAuthenticated() {
        // Given
        String endpoint = "/api/protected/**";
        JEFSecurityLevel securityLevel = JEFSecurityLevel.authenticated();

        // When
        invokeConfigureEndpointSecurity(endpoint, securityLevel);

        // Then
        verify(authzRegistry).requestMatchers(endpoint);
        verify(authorizedUrl).authenticated();
        verify(authorizedUrl, never()).permitAll();
        verify(authorizedUrl, never()).hasAnyRole(any(String[].class));
    }

    @Test
    void testEndpointSecurityWithSingleRole() {
        // Given
        String endpoint = "/api/admin/**";
        JEFSecurityLevel securityLevel = JEFSecurityLevel.hasRole("ADMIN");

        // When
        invokeConfigureEndpointSecurity(endpoint, securityLevel);

        // Then
        verify(authzRegistry).requestMatchers(endpoint);
        verify(authorizedUrl).hasAnyRole(eq(new String[]{"ADMIN"}));
        verify(authorizedUrl, never()).permitAll();
        verify(authorizedUrl, never()).authenticated();
    }

    @Test
    void testEndpointSecurityWithMultipleRoles() {
        // Given
        String endpoint = "/api/management/**";
        JEFSecurityLevel securityLevel = JEFSecurityLevel.hasAnyRole("ADMIN", "MANAGER", "SUPERVISOR");

        // When
        invokeConfigureEndpointSecurity(endpoint, securityLevel);

        // Then
        verify(authzRegistry).requestMatchers(endpoint);
        verify(authorizedUrl).hasAnyRole(eq(new String[]{"ADMIN", "MANAGER", "SUPERVISOR"}));
        verify(authorizedUrl, never()).permitAll();
        verify(authorizedUrl, never()).authenticated();
    }

    @Test
    void testDefaultSecurityConfiguration() {
        // Given
        JEFSecurityLevel defaultSecurity = JEFSecurityLevel.hasAnyRole("USER", "ADMIN");
        Map<String, JEFSecurityLevel> endpoints = new HashMap<>();
        endpoints.put("/api/public/**", JEFSecurityLevel.permitAll());
        endpoints.put("/api/admin/**", JEFSecurityLevel.hasRole("ADMIN"));

        when(jefSecurityProperties.getDefaultSecurity()).thenReturn(defaultSecurity);
        when(jefSecurityProperties.getEndpoints()).thenReturn(endpoints);

        // When
        invokeConfigureDefaultSecurity(defaultSecurity);

        // Then
        verify(authzRegistry).anyRequest();
        verify(authorizedUrl).hasAnyRole(eq(new String[]{"USER", "ADMIN"}));
        verify(authorizedUrl, never()).permitAll();
        verify(authorizedUrl, never()).authenticated();
    }

    /**
     * Méthode utilitaire pour invoquer la méthode privée configureEndpointSecurity
     */
    private void invokeConfigureEndpointSecurity(String endpoint, JEFSecurityLevel securityLevel) {
        try {
            ReflectionTestUtils.invokeMethod(
                securityConfig,
                "configureEndpointSecurity",
                authzRegistry,
                endpoint,
                securityLevel
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'invocation de configureEndpointSecurity", e);
        }
    }

    /**
     * Méthode utilitaire pour invoquer la méthode privée configureDefaultSecurity
     */
    private void invokeConfigureDefaultSecurity(JEFSecurityLevel defaultSecurity) {
        try {
            ReflectionTestUtils.invokeMethod(
                securityConfig,
                "configureDefaultSecurity",
                authzRegistry,
                defaultSecurity
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'invocation de configureDefaultSecurity", e);
        }
    }
}