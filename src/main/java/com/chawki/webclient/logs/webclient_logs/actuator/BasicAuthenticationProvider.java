package com.chawki.webclient.logs.webclient_logs.resolver;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BasicAuthenticationProvider implements AuthenticationProvider {

    private static final Map<String, UserInfo> USERS = Map.of(
        "admin", new UserInfo("admin", "adminpass", List.of("ROLE_ADMIN")),
        "manager", new UserInfo("manager", "managerpass", List.of("ROLE_MANAGER")),
        "user", new UserInfo("user", "userpass", List.of("ROLE_USER"))
    );

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        UserInfo userInfo = USERS.get(username);
        if (userInfo != null && userInfo.password().equals(password)) {
            return new UsernamePasswordAuthenticationToken(
                username,
                password,
                userInfo.roles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList()
            );
        }

        throw new BadCredentialsException("Invalid username or password");
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private record UserInfo(String username, String password, List<String> roles) {}
}
