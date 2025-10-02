import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ConfigurationProperties(prefix = "app.security.actuator")
@Data
@Component
public class ActuatorSecurityProperties {
    
    private DefaultConfig defaultConfig = new DefaultConfig();
    private List<EndpointConfig> endpoints = new ArrayList<>();
    
    @Data
    public static class DefaultConfig {
        private boolean authenticated = true;
        private List<String> roles = new ArrayList<>();
    }
    
    @Data
    public static class EndpointConfig {
        private String url;
        private List<SecurityRule> rules = new ArrayList<>();
    }
    
    @Data
    public static class SecurityRule {
        private List<String> methods = Arrays.asList("GET");
        private boolean authenticated = true;
        private List<String> roles = new ArrayList<>();
    }
}

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.yourcompany.security.actuator.ActuatorSecurityProperties.*;

@Service
@Slf4j
public class ActuatorSecurityService {
    
    private final ActuatorSecurityProperties properties;
    
    public ActuatorSecurityService(ActuatorSecurityProperties properties) {
        this.properties = properties;
        log.info("Actuator security service initialized with {} endpoints", 
                properties.getEndpoints().size());
    }
    
    /**
     * Recherche une règle de sécurité pour un endpoint et une méthode HTTP
     */
    public Optional<SecurityRule> getSecurityRule(String url, String method) {
        return properties.getEndpoints().stream()
                .filter(endpoint -> matchesUrl(endpoint.getUrl(), url))
                .findFirst()
                .flatMap(endpoint -> findMatchingRule(endpoint, method));
    }
    
    /**
     * Vérifie si l'URL correspond au pattern (support des wildcards **)
     */
    private boolean matchesUrl(String pattern, String url) {
        if (pattern.endsWith("/**")) {
            String basePattern = pattern.substring(0, pattern.length() - 3);
            return url.startsWith(basePattern);
        }
        return pattern.equals(url);
    }
    
    /**
     * Trouve la règle correspondant à la méthode HTTP
     */
    private Optional<SecurityRule> findMatchingRule(EndpointConfig endpoint, String method) {
        return endpoint.getRules().stream()
                .filter(rule -> rule.getMethods().contains(method.toUpperCase()))
                .findFirst();
    }
    
    /**
     * Retourne la règle de sécurité par défaut
     */
    public SecurityRule getDefaultSecurityRule() {
        SecurityRule defaultRule = new SecurityRule();
        defaultRule.setAuthenticated(properties.getDefaultConfig().isAuthenticated());
        defaultRule.setRoles(properties.getDefaultConfig().getRoles());
        return defaultRule;
    }
    
    /**
     * Retourne tous les endpoints configurés
     */
    public List<EndpointConfig> getAllEndpoints() {
        return properties.getEndpoints();
    }
}

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static com.yourcompany.security.actuator.ActuatorSecurityProperties.*;

@Configuration
@EnableWebSecurity
@Slf4j
public class JefActuatorWebSecurityConfiguration {
    
    private final ActuatorSecurityService securityService;
    
    public JefActuatorWebSecurityConfiguration(ActuatorSecurityService securityService) {
        this.securityService = securityService;
    }
    
    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/actuator/**")
            .authorizeHttpRequests(authz -> {
                configureActuatorEndpoints(authz);
            })
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(Customizer.withDefaults());
            
        return http.build();
    }
    
    private void configureActuatorEndpoints(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        
        // Configuration des endpoints spécifiques avec leurs méthodes HTTP
        configureSpecificEndpoints(authz);
        
        // Configuration par défaut pour tous les autres endpoints actuator
        SecurityRule defaultRule = securityService.getDefaultSecurityRule();
        applySecurityRule(authz.requestMatchers("/actuator/**"), defaultRule);
    }
    
    private void configureSpecificEndpoints(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        
        for (EndpointConfig endpoint : securityService.getAllEndpoints()) {
            String url = endpoint.getUrl();
            
            for (SecurityRule rule : endpoint.getRules()) {
                for (String method : rule.getMethods()) {
                    RequestMatcher matcher = createMethodAndUrlMatcher(method, url);
                    applySecurityRule(authz.requestMatchers(matcher), rule);
                    
                    log.debug("Configured security for {} {} - Auth: {}, Roles: {}", 
                            method, url, rule.isAuthenticated(), rule.getRoles());
                }
            }
        }
    }
    
    /**
     * Crée un matcher pour une méthode HTTP et une URL spécifiques
     */
    private RequestMatcher createMethodAndUrlMatcher(String method, String url) {
        return new AndRequestMatcher(
            new AntPathRequestMatcher(url),
            request -> method.equalsIgnoreCase(request.getMethod())
        );
    }
    
    /**
     * Applique une règle de sécurité à un endpoint
     */
    private void applySecurityRule(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl authorizedUrl, 
            SecurityRule rule) {
        
        if (!rule.isAuthenticated()) {
            authorizedUrl.permitAll();
        } else if (rule.getRoles().isEmpty()) {
            authorizedUrl.authenticated();
        } else {
            // Retire le préfixe ROLE_ si présent car hasAnyRole l'ajoute automatiquement
            String[] roles = rule.getRoles().stream()
                    .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                    .toArray(String[]::new);
            authorizedUrl.hasAnyRole(roles);
        }
    }
}

