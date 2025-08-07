package com.chawki.webclient.logs.webclient_logs.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiProviderAuthenticationManagerTest {

    @Mock
    private AuthenticationProvider basicProvider;
    
    @Mock
    private AuthenticationProvider ldapProvider;
    
    @Mock
    private AuthenticationProvider oauthProvider;

    private MultiProviderAuthenticationManager authManager;
    private Authentication validAuth;
    private Authentication invalidAuth;

    @BeforeEach
    void setUp() {
        List<AuthenticationProvider> providers = Arrays.asList(basicProvider, ldapProvider, oauthProvider);
        authManager = new MultiProviderAuthenticationManager(providers);
        
        validAuth = new UsernamePasswordAuthenticationToken("user", "password");
        invalidAuth = new UsernamePasswordAuthenticationToken("invalid", "invalid");
    }

    @Test
    void shouldAuthenticateWithFirstSupportingProvider() {
        // Given
        Authentication expectedAuth = new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        
        when(basicProvider.supports(UsernamePasswordAuthenticationToken.class)).thenReturn(true);
        when(basicProvider.authenticate(validAuth)).thenReturn(expectedAuth);
        when(ldapProvider.supports(UsernamePasswordAuthenticationToken.class)).thenReturn(false);
        when(oauthProvider.supports(UsernamePasswordAuthenticationToken.class)).thenReturn(false);

        // When
        Authentication result = authManager.authenticate(validAuth);

        // Then
        assertEquals(expectedAuth, result);
        verify(basicProvider).authenticate(validAuth);
        verify(ldapProvider, never()).authenticate(any());
        verify(oauthProvider, never()).authenticate(any());
    }

    @Test
    void shouldFallbackToSecondProviderWhenFirstFails() {
        // Given
        Authentication expectedAuth = new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        
        when(basicProvider.supports(UsernamePasswordAuthenticationToken.class)).thenReturn(true);
        when(basicProvider.authenticate(validAuth)).thenThrow(new BadCredentialsException("Failed"));
        when(ldapProvider.supports(UsernamePasswordAuthenticationToken.class)).thenReturn(true);
        when(ldapProvider.authenticate(validAuth)).thenReturn(expectedAuth);
        when(oauthProvider.supports(UsernamePasswordAuthenticationToken.class)).thenReturn(false);

        // When
        Authentication result = authManager.authenticate(validAuth);

        // Then
        assertEquals(expectedAuth, result);
        verify(basicProvider).authenticate(validAuth);
        verify(ldapProvider).authenticate(validAuth);
        verify(oauthProvider, never()).authenticate(any());
    }

    @Test
    void shouldThrowExceptionWhenNoProviderSupportsAuthentication() {
        // Given
        when(basicProvider.supports(any())).thenReturn(false);
        when(ldapProvider.supports(any())).thenReturn(false);
        when(oauthProvider.supports(any())).thenReturn(false);

        // When & Then
        assertThrows(AuthenticationException.class, () -> {
            authManager.authenticate(validAuth);
        });
        
        verify(basicProvider, never()).authenticate(any());
        verify(ldapProvider, never()).authenticate(any());
        verify(oauthProvider, never()).authenticate(any());
    }

    @Test
    void shouldThrowExceptionWhenAllProvidersFailAuthentication() {
        // Given
        when(basicProvider.supports(UsernamePasswordAuthenticationToken.class)).thenReturn(true);
        when(basicProvider.authenticate(invalidAuth)).thenThrow(new BadCredentialsException("Basic failed"));
        when(ldapProvider.supports(UsernamePasswordAuthenticationToken.class)).thenReturn(true);
        when(ldapProvider.authenticate(invalidAuth)).thenThrow(new BadCredentialsException("LDAP failed"));
        when(oauthProvider.supports(UsernamePasswordAuthenticationToken.class)).thenReturn(true);
        when(oauthProvider.authenticate(invalidAuth)).thenThrow(new BadCredentialsException("OAuth failed"));

        // When & Then
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> {
            authManager.authenticate(invalidAuth);
        });
        
        assertEquals("OAuth failed", exception.getMessage());
        verify(basicProvider).authenticate(invalidAuth);
        verify(ldapProvider).authenticate(invalidAuth);
        verify(oauthProvider).authenticate(invalidAuth);
    }

    @Test
    void shouldValidateSecurityLevelBasic() {
        // Given
        Authentication basicAuth = new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        SecurityLevel requiredLevel = SecurityLevel.BASIC;

        // When
        boolean isValid = authManager.validateSecurityLevel(basicAuth, requiredLevel);

        // Then
        assertTrue(isValid);
    }

    @Test
    void shouldValidateSecurityLevelMfaRequirement() {
        // Given
        Authentication mfaAuth = new UsernamePasswordAuthenticationToken("user", null, 
            Collections.singletonList(() -> "ROLE_MFA_VERIFIED"));
        SecurityLevel requiredLevel = SecurityLevel.MFA_REQUIRED;

        // When
        boolean isValid = authManager.validateSecurityLevel(mfaAuth, requiredLevel);

        // Then
        assertTrue(isValid);
        
        // Test with non-MFA auth
        Authentication basicAuth = new UsernamePasswordAuthenticationToken("user", null, Collections.emptyList());
        boolean isValidBasic = authManager.validateSecurityLevel(basicAuth, requiredLevel);
        assertFalse(isValidBasic);
    }
}
