package com.chawki.webclient.logs.webclient_logs.resolver;

import com.example.security.CustomAuthenticationManagerResolver;
import com.example.security.TokenAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private final CustomAuthenticationManagerResolver authManagerResolver;
    private final SecurityProperties securityProperties;

    public SecurityConfig(CustomAuthenticationManagerResolver authManagerResolver,
                         SecurityProperties securityProperties) {
        this.authManagerResolver = authManagerResolver;
        this.securityProperties = securityProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new TokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .authenticationManagerResolver(authManagerResolver)
            .authorizeHttpRequests(authz -> {
                // Configuration dynamique basée sur les propriétés
                configureActuatorEndpoints(authz);
                
                // Autres endpoints nécessitent une authentification
                authz.requestMatchers("/api/**").authenticated()
                     .anyRequest().permitAll();
            })
            .httpBasic(httpBasic -> {});

        return http.build();
    }

    private void configureActuatorEndpoints(
            org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        
        if (!securityProperties.actuator().enabled()) {
            authz.requestMatchers("/actuator/**").denyAll();
            return;
        }

        for (SecurityProperties.EndpointConfig endpoint : securityProperties.actuator().endpoints()) {
            String url = endpoint.url().startsWith("/") ? endpoint.url() : "/" + endpoint.url();
            
            if (!endpoint.authenticated()) {
                authz.requestMatchers(url).permitAll();
            } else if (!endpoint.roles().isEmpty()) {
                String[] roles = endpoint.roles().toArray(new String[0]);
                authz.requestMatchers(url).hasAnyRole(roles);
            } else {
                authz.requestMatchers(url).authenticated();
            }
        }
        
        // Par défaut, bloquer les autres endpoints actuator non configurés
        authz.requestMatchers("/actuator/**").denyAll();
    }
}
