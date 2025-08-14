package com.chawki.webclient.logs.webclient_logs.resolver;


import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TokenAuthenticationProvider implements AuthenticationProvider {

    private static final String VALID_TOKEN = "admin-token-123";
    private static final String MANAGER_TOKEN = "manager-token-456";

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getPrincipal();
        
        if (VALID_TOKEN.equals(token)) {
            return new PreAuthenticatedAuthenticationToken(
                "admin", 
                token, 
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
        } else if (MANAGER_TOKEN.equals(token)) {
            return new PreAuthenticatedAuthenticationToken(
                "manager", 
                token, 
                List.of(new SimpleGrantedAuthority("ROLE_MANAGER"))
            );
        }
        
        throw new BadCredentialsException("Invalid token: " + token);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
