// ===== Modèles de configuration =====

@ConfigurationProperties(prefix = "app.security.actuator")
@Data
@Component
public class ActuatorSecurityProperties {
    
    private Map<String, EnvironmentConfig> environments = new HashMap<>();
    private List<EndpointConfig> endpoints = new ArrayList<>();
    
    @Data
    public static class EnvironmentConfig {
        private boolean defaultAuthenticated = true;
        private List<String> defaultRoles = new ArrayList<>();
    }
    
    @Data
    public static class EndpointConfig {
        private String url;
        private Map<String, EndpointEnvironmentConfig> environments = new HashMap<>();
    }
    
    @Data
    public static class EndpointEnvironmentConfig {
        private List<SecurityRule> rules = new ArrayList<>();
    }
    
    @Data
    public static class SecurityRule {
        private List<String> methods = Arrays.asList("GET");
        private boolean authenticated = true;
        private List<String> roles = new ArrayList<>();
    }
}

// ===== Service de gestion des règles de sécurité =====

@Service
@Slf4j
public class ActuatorSecurityService {
    
    private final ActuatorSecurityProperties properties;
    private final String currentEnvironment;
    
    public ActuatorSecurityService(ActuatorSecurityProperties properties, Environment environment) {
        this.properties = properties;
        this.currentEnvironment = determineCurrentEnvironment(environment);
        log.info("Actuator security initialized for environment: {}", currentEnvironment);
    }
    
    private String determineCurrentEnvironment(Environment environment) {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            String profile = activeProfiles[0].toUpperCase();
            if (Arrays.asList("DEVELOP", "QA", "PROD").contains(profile)) {
                return profile;
            }
        }
        return "DEVELOP"; // par défaut
    }
    
    public Optional<SecurityRule> getSecurityRule(String url, String method) {
        return properties.getEndpoints().stream()
                .filter(endpoint -> matchesUrl(endpoint.getUrl(), url))
                .findFirst()
                .flatMap(endpoint -> findMatchingRule(endpoint, method));
    }
    
    private boolean matchesUrl(String pattern, String url) {
        if (pattern.endsWith("/**")) {
            String basePattern = pattern.substring(0, pattern.length() - 3);
            return url.startsWith(basePattern);
        }
        return pattern.equals(url);
    }
    
    private Optional<SecurityRule> findMatchingRule(EndpointConfig endpoint, String method) {
        EndpointEnvironmentConfig envConfig = endpoint.getEnvironments().get(currentEnvironment);
        if (envConfig == null) {
            return Optional.empty();
        }
        
        return envConfig.getRules().stream()
                .filter(rule -> rule.getMethods().contains(method.toUpperCase()))
                .findFirst();
    }
    
    public SecurityRule getDefaultSecurityRule() {
        EnvironmentConfig envConfig = properties.getEnvironments().get(currentEnvironment);
        if (envConfig != null) {
            SecurityRule defaultRule = new SecurityRule();
            defaultRule.setAuthenticated(envConfig.isDefaultAuthenticated());
            defaultRule.setRoles(envConfig.getDefaultRoles());
            return defaultRule;
        }
        
        // Fallback si aucune configuration d'environnement
        SecurityRule fallback = new SecurityRule();
        fallback.setAuthenticated(true);
        fallback.setRoles(Arrays.asList("ROLE_ADMIN"));
        return fallback;
    }
}

// ===== Configuration de sécurité principale =====

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
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(Customizer.withDefaults());
            
        return http.build();
    }
    
    private void configureActuatorEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        // Configuration par endpoint et méthode
        configureSpecificEndpoints(authz);
        
        // Configuration par défaut pour tous les autres endpoints actuator
        SecurityRule defaultRule = securityService.getDefaultSecurityRule();
        applySecurityRule(authz.requestMatchers("/actuator/**"), defaultRule);
    }
    
    private void configureSpecificEndpoints(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
        // Parcourir tous les endpoints configurés
        ActuatorSecurityProperties properties = securityService.properties;
        
        for (ActuatorSecurityProperties.EndpointConfig endpoint : properties.getEndpoints()) {
            String url = endpoint.getUrl();
            
            // Obtenir la configuration pour l'environnement courant
            ActuatorSecurityProperties.EndpointEnvironmentConfig envConfig = 
                endpoint.getEnvironments().get(securityService.currentEnvironment);
            
            if (envConfig != null) {
                for (ActuatorSecurityProperties.SecurityRule rule : envConfig.getRules()) {
                    for (String method : rule.getMethods()) {
                        RequestMatcher matcher = createMethodAndUrlMatcher(method, url);
                        applySecurityRule(authz.requestMatchers(matcher), rule);
                        
                        log.debug("Configured security for {} {} - Auth: {}, Roles: {}", 
                                method, url, rule.isAuthenticated(), rule.getRoles());
                    }
                }
            }
        }
    }
    
    private RequestMatcher createMethodAndUrlMatcher(String method, String url) {
        return new AndRequestMatcher(
            new AntPathRequestMatcher(url),
            new HttpMethodRequestMatcher(method)
        );
    }
    
    private void applySecurityRule(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl authorizedUrl, 
            ActuatorSecurityProperties.SecurityRule rule) {
        
        if (!rule.isAuthenticated()) {
            authorizedUrl.permitAll();
        } else if (rule.getRoles().isEmpty()) {
            authorizedUrl.authenticated();
        } else {
            String[] roles = rule.getRoles().stream()
                    .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                    .toArray(String[]::new);
            authorizedUrl.hasAnyRole(roles);
        }
    }
}

// ===== Configuration pour tests et monitoring =====

@RestController
@RequestMapping("/actuator/security")
@ConditionalOnProperty(name = "app.security.actuator.debug", havingValue = "true")
public class ActuatorSecurityDebugController {
    
    private final ActuatorSecurityService securityService;
    
    public ActuatorSecurityDebugController(ActuatorSecurityService securityService) {
        this.securityService = securityService;
    }
    
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getSecurityConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("currentEnvironment", securityService.currentEnvironment);
        config.put("defaultRule", securityService.getDefaultSecurityRule());
        return ResponseEntity.ok(config);
    }
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpointSecurity(
            @RequestParam String url,
            @RequestParam(defaultValue = "GET") String method) {
        
        Map<String, Object> result = new HashMap<>();
        result.put("url", url);
        result.put("method", method);
        result.put("environment", securityService.currentEnvironment);
        
        Optional<ActuatorSecurityProperties.SecurityRule> rule = 
                securityService.getSecurityRule(url, method);
        
        if (rule.isPresent()) {
            result.put("rule", rule.get());
            result.put("configured", true);
        } else {
            result.put("rule", securityService.getDefaultSecurityRule());
            result.put("configured", false);
        }
        
        return ResponseEntity.ok(result);
    }
}
