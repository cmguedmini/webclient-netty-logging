
package com.example.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class JefActuatorWebSecurityConfigurationConfigureAuthorizedUrlTest {

    @Test
    void configureAuthorizedUrl_permitAll_when_authenticated_false() {
        // Given
        var config = new JefActuatorWebSecurityConfiguration(); // ta classe réelle
        var authorizedUrl = mock(AuthorizeHttpRequestsConfigurer.AuthorizedUrl.class);

        var level = new EndpointSecurityLevel();
        level.setAuthenticated(false);
        level.setRoles(List.of()); // peu importe ici

        // When
        config.configureAuthorizedUrl(level, authorizedUrl);

        // Then
        verify(authorizedUrl).permitAll();
        verifyNoMoreInteractions(authorizedUrl);
    }

    @Test
    void configureAuthorizedUrl_authenticated_when_roles_empty() {
        var config = new JefActuatorWebSecurityConfiguration();
        var authorizedUrl = mock(AuthorizeHttpRequestsConfigurer.AuthorizedUrl.class);

        var level = new EndpointSecurityLevel();
        level.setAuthenticated(true);
        level.setRoles(List.of()); // => authenticated()

        config.configureAuthorizedUrl(level, authorizedUrl);

        verify(authorizedUrl).authenticated();
        verifyNoMoreInteractions(authorizedUrl);
    }

    @Test
    void configureAuthorizedUrl_hasAnyRole_when_roles_present() {
        var config = new JefActuatorWebSecurityConfiguration();
        var authorizedUrl = mock(AuthorizeHttpRequestsConfigurer.AuthorizedUrl.class);

        var level = new EndpointSecurityLevel();
        level.setAuthenticated(true);
        level.setRoles(List.of("MANAGER", "gtx_tx_mge_cb"));

        config.configureAuthorizedUrl(level, authorizedUrl);

        verify(authorizedUrl).hasAnyRole("MANAGER", "gtx_tx_mge_cb");
        verifyNoMoreInteractions(authorizedUrl);
    }
}
