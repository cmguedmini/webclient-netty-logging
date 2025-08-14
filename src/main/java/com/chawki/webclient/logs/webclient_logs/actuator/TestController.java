package com.chawki.webclient.logs.webclient_logs.resolver;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
            "message", "Welcome to Spring Security Auth Resolver Demo",
            "endpoints", "/api/secure, /api/public, /actuator/health, /actuator/beans"
        );
    }

    @GetMapping("/api/public")
    public Map<String, String> publicEndpoint() {
        return Map.of(
            "message", "This is a public endpoint",
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
    }

    @GetMapping("/api/secure")
    public Map<String, Object> secureEndpoint() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Map.of(
            "message", "This is a secure endpoint",
            "user", auth.getName(),
            "authorities", auth.getAuthorities(),
            "timestamp", String.valueOf(System.currentTimeMillis())
        );
    }

    @GetMapping("/api/user-info")
    public Map<String, Object> userInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Map.of(
            "username", auth.getName(),
            "authorities", auth.getAuthorities(),
            "authenticated", auth.isAuthenticated(),
            "principal", auth.getPrincipal().toString()
        );
    }
}