package com.chawki.webclient.logs.webclient_logs.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.web.authentication.AuthenticationManagerResolver;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomAuthenticationManagerResolver implements AuthenticationManagerResolver<HttpServletRequest> {

    private final AuthenticationManager actuatorAuthManager;
    private final AuthenticationManager defaultAuthManager;

    public CustomAuthenticationManagerResolver(
            TokenAuthenticationProvider tokenAuthProvider,
            BasicAuthenticationProvider basicAuthProvider) {
        
        // AuthenticationManager pour /actuator/** avec Token et Basic Auth
        this.actuatorAuthManager = new ProviderManager(
            List.of(tokenAuthProvider, basicAuthProvider)
        );
        
        // AuthenticationManager par défaut pour les autres chemins
        this.defaultAuthManager = new ProviderManager(
            List.of(basicAuthProvider)
        );
    }

    @Override
    public AuthenticationManager resolve(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        
        if (requestURI.startsWith("/actuator")) {
            return actuatorAuthManager;
        }
        
        return defaultAuthManager;
    }
}