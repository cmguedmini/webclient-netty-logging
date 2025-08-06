package com.chawki.webclient.logs.webclient_logs.security;

//1. Configuration principale Spring Security
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

 @Autowired
 private BasicAuthenticationProvider basicAuthenticationProvider;
 
 @Autowired
 private TokenAuthenticationProvider tokenAuthenticationProvider;
 
 @Autowired
 private JEFSecurityProperties jefSecurityProperties;
 
 @Value("${spring.profiles.active:dev}")
 private String activeProfile;

 @Bean
 public AuthenticationManager authenticationManager() {
     return new MultiProviderAuthenticationManager(
         Arrays.asList(basicAuthenticationProvider, tokenAuthenticationProvider)
     );
 }

 @Bean
 public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
     http
         .authorizeHttpRequests(authz -> {
             // Configuration des endpoints spécifiques via la map
             Map<String, JEFSecurityLevel> endpointSecurity = jefSecurityProperties.getEndpoints();
             
             for (Map.Entry<String, JEFSecurityLevel> entry : endpointSecurity.entrySet()) {
                 String endpoint = entry.getKey();
                 JEFSecurityLevel securityLevel = entry.getValue();
                 
                 configureEndpointSecurity(authz, endpoint, securityLevel);
             }
             
             // Configuration des endpoints Actuator selon l'environnement
             configureActuatorSecurity(authz);
             
             // Application du niveau de sécurité par défaut sur anyRequest
             JEFSecurityLevel defaultSecurity = jefSecurityProperties.getDefaultSecurity();
             configureDefaultSecurity(authz, defaultSecurity);
         })
         .authenticationManager(authenticationManager())
         .addFilterBefore(new TokenAuthenticationFilter(authenticationManager()), 
                        UsernamePasswordAuthenticationFilter.class)
         .httpBasic(Customizer.withDefaults())
         .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
         .csrf(csrf -> csrf.disable());

     return http.build();
 }
 
 private void configureEndpointSecurity(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz, 
                                      String endpoint, JEFSecurityLevel securityLevel) {
     if (securityLevel.isPermit()) {
         authz.requestMatchers(endpoint).permitAll();
     } else if (securityLevel.getRoles().isEmpty()) {
         // Si permit = false et roles = empty, alors authenticated()
         authz.requestMatchers(endpoint).authenticated();
     } else {
         // Appliquer les rôles spécifiques
         String[] roles = securityLevel.getRoles().toArray(new String[0]);
         authz.requestMatchers(endpoint).hasAnyRole(extractRoleNames(roles));
     }
 }
 
 private void configureActuatorSecurity(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
     if ("uat".equalsIgnoreCase(activeProfile)) {
         // En UAT : tous les endpoints Actuator sont publics
         authz.requestMatchers("/actuator/**").permitAll();
     } else if ("prod".equalsIgnoreCase(activeProfile)) {
         // En PROD : endpoints Actuator sécurisés par rôle ACTUATOR
         authz.requestMatchers("/actuator/health").permitAll()
               .requestMatchers("/actuator/info").permitAll()
               .requestMatchers("/actuator/**").hasRole("ACTUATOR");
     } else {
         // Autres environnements (dev, test) : accès restreint aux administrateurs
         authz.requestMatchers("/actuator/**").hasRole("ADMIN");
     }
 }
 
 private void configureDefaultSecurity(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz, 
                                     JEFSecurityLevel defaultSecurity) {
     if (defaultSecurity.isPermit()) {
         authz.anyRequest().permitAll();
     } else if (defaultSecurity.getRoles().isEmpty()) {
         // Si permit = false et roles = empty, alors authenticated()
         authz.anyRequest().authenticated();
     } else {
         // Appliquer les rôles par défaut
         String[] roles = defaultSecurity.getRoles().toArray(new String[0]);
         authz.anyRequest().hasAnyRole(extractRoleNames(roles));
     }
 }
 
 private String[] extractRoleNames(String[] roles) {
     // Supprimer le préfixe ROLE_ si présent pour éviter la double préfixation
     return Arrays.stream(roles)
             .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
             .toArray(String[]::new);
 }
}

//JEFSecurityLevel - Classe pour définir les niveaux de sécurité
public class JEFSecurityLevel {
 
 private boolean permit = false;
 private List<String> roles = new ArrayList<>();
 
 // Constructeurs
 public JEFSecurityLevel() {}
 
 public JEFSecurityLevel(boolean permit, List<String> roles) {
     this.permit = permit;
     this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
 }
 
 // Factory methods pour faciliter la création
 public static JEFSecurityLevel permitAll() {
     return new JEFSecurityLevel(true, Collections.emptyList());
 }
 
 public static JEFSecurityLevel authenticated() {
     return new JEFSecurityLevel(false, Collections.emptyList());
 }
 
 public static JEFSecurityLevel hasRole(String role) {
     return new JEFSecurityLevel(false, Collections.singletonList(role));
 }
 
 public static JEFSecurityLevel hasAnyRole(String... roles) {
     return new JEFSecurityLevel(false, Arrays.asList(roles));
 }
 
 // Getters et Setters
 public boolean isPermit() {
     return permit;
 }
 
 public void setPermit(boolean permit) {
     this.permit = permit;
 }
 
 public List<String> getRoles() {
     return roles;
 }
 
 public void setRoles(List<String> roles) {
     this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
 }
 
 // Méthodes utilitaires
 public boolean requiresAuthentication() {
     return !permit;
 }
 
 public boolean hasSpecificRoles() {
     return !permit && !roles.isEmpty();
 }
 
 public boolean isPublic() {
     return permit;
 }
 
 @Override
 public String toString() {
     if (permit) {
         return "JEFSecurityLevel{permit=true}";
     } else if (roles.isEmpty()) {
         return "JEFSecurityLevel{authenticated=true}";
     } else {
         return "JEFSecurityLevel{roles=" + roles + "}";
     }
 }
 
 @Override
 public boolean equals(Object o) {
     if (this == o) return true;
     if (o == null || getClass() != o.getClass()) return false;
     JEFSecurityLevel that = (JEFSecurityLevel) o;
     return permit == that.permit && Objects.equals(roles, that.roles);
 }
 
 @Override
 public int hashCode() {
     return Objects.hash(permit, roles);
 }
}

//JEFSecurityProperties - Configuration properties pour la sécurité
@ConfigurationProperties(prefix = "jef.security")
@Component
@Data
@Slf4j
public class JEFSecurityProperties {
 
 /**
  * Niveau de sécurité par défaut appliqué à anyRequest()
  */
 private JEFSecurityLevel defaultSecurity = JEFSecurityLevel.authenticated();
 
 /**
  * Configuration de sécurité par endpoint
  * Clé: pattern d'endpoint (ex: "/api/admin/**")
  * Valeur: niveau de sécurité à appliquer
  */
 private Map<String, JEFSecurityLevel> endpoints = new HashMap<>();
 
 /**
  * Initialisation post-construction pour valider la configuration
  */
 @PostConstruct
 public void init() {
     log.info("=== JEF SECURITY CONFIGURATION ===");
     log.info("Default Security: {}", defaultSecurity);
     log.info("Endpoint Specific Security:");
     
     endpoints.forEach((endpoint, security) -> {
         log.info("  {} -> {}", endpoint, security);
     });
     
     // Validation de la configuration
     validateConfiguration();
     log.info("===================================");
 }
 
 private void validateConfiguration() {
     // Valider le niveau de sécurité par défaut
     if (defaultSecurity == null) {
         log.warn("Default security is null, setting to authenticated()");
         defaultSecurity = JEFSecurityLevel.authenticated();
     }
     
     // Valider les configurations d'endpoints
     endpoints.entrySet().removeIf(entry -> {
         if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
             log.warn("Removing endpoint with null or empty pattern");
             return true;
         }
         if (entry.getValue() == null) {
             log.warn("Removing endpoint '{}' with null security level", entry.getKey());
             return true;
         }
         return false;
     });
     
     // Ajouter des configurations par défaut si elles n'existent pas
     addDefaultEndpointIfNotExists("/api/public/**", JEFSecurityLevel.permitAll());
 }
 
 private void addDefaultEndpointIfNotExists(String endpoint, JEFSecurityLevel security) {
     if (!endpoints.containsKey(endpoint)) {
         endpoints.put(endpoint, security);
         log.info("Added default endpoint configuration: {} -> {}", endpoint, security);
     }
 }
 
 /**
  * Obtenir le niveau de sécurité pour un endpoint spécifique
  */
 public JEFSecurityLevel getSecurityLevelForEndpoint(String endpoint) {
     return endpoints.getOrDefault(endpoint, defaultSecurity);
 }
 
 /**
  * Ajouter ou mettre à jour la sécurité d'un endpoint
  */
 public void setEndpointSecurity(String endpoint, JEFSecurityLevel security) {
     if (endpoint != null && !endpoint.trim().isEmpty() && security != null) {
         endpoints.put(endpoint, security);
         log.info("Updated endpoint security: {} -> {}", endpoint, security);
     }
 }
 
 /**
  * Supprimer la configuration de sécurité d'un endpoint
  */
 public void removeEndpointSecurity(String endpoint) {
     JEFSecurityLevel removed = endpoints.remove(endpoint);
     if (removed != null) {
         log.info("Removed endpoint security: {} (was: {})", endpoint, removed);
     }
 }
 
 /**
  * Obtenir tous les endpoints configurés
  */
 public Set<String> getConfiguredEndpoints() {
     return new HashSet<>(endpoints.keySet());
 }
 
 /**
  * Vérifier si un endpoint a une configuration spécifique
  */
 public boolean hasEndpointConfiguration(String endpoint) {
     return endpoints.containsKey(endpoint);
 }
}
//2. AuthenticationManager personnalisé
@Component
public class MultiProviderAuthenticationManager implements AuthenticationManager {
 
 private final List<AuthenticationProvider> providers;
 
 public MultiProviderAuthenticationManager(List<AuthenticationProvider> providers) {
     this.providers = providers;
 }

 @Override
 public Authentication authenticate(Authentication authentication) throws AuthenticationException {
     
     for (AuthenticationProvider provider : providers) {
         if (provider.supports(authentication.getClass())) {
             try {
                 Authentication result = provider.authenticate(authentication);
                 if (result != null) {
                     return result;
                 }
             } catch (AuthenticationException e) {
                 // Log l'erreur et continue avec le prochain provider
                 System.err.println("Authentication failed with provider: " + 
                                  provider.getClass().getSimpleName() + " - " + e.getMessage());
             }
         }
     }
     
     throw new BadCredentialsException("Authentication failed with all providers");
 }
}

//3. BasicAuthenticationProvider
@Component
public class BasicAuthenticationProvider implements AuthenticationProvider {
 
 @Autowired
 private UserDetailsService userDetailsService;
 
 @Autowired
 private PasswordEncoder passwordEncoder;

 @Override
 public Authentication authenticate(Authentication authentication) throws AuthenticationException {
     
     String username = authentication.getName();
     String password = authentication.getCredentials().toString();
     
     try {
         UserDetails userDetails = userDetailsService.loadUserByUsername(username);
         
         if (passwordEncoder.matches(password, userDetails.getPassword())) {
             return new UsernamePasswordAuthenticationToken(
                 userDetails, 
                 password, 
                 userDetails.getAuthorities()
             );
         } else {
             throw new BadCredentialsException("Invalid username or password");
         }
         
     } catch (UsernameNotFoundException e) {
         throw new BadCredentialsException("User not found: " + username);
     }
 }

 @Override
 public boolean supports(Class<?> authentication) {
     return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
 }
}

//4. TokenAuthenticationProvider
@Component
public class TokenAuthenticationProvider implements AuthenticationProvider {
 
 @Autowired
 private JwtTokenService jwtTokenService;
 
 @Autowired
 private UserDetailsService userDetailsService;

 @Override
 public Authentication authenticate(Authentication authentication) throws AuthenticationException {
     
     if (!(authentication instanceof TokenAuthentication)) {
         throw new IllegalArgumentException("Unsupported authentication type");
     }
     
     TokenAuthentication tokenAuth = (TokenAuthentication) authentication;
     String token = tokenAuth.getToken();
     
     try {
         // Valider le token JWT
         String username = jwtTokenService.validateTokenAndGetUsername(token);
         
         if (username != null) {
             UserDetails userDetails = userDetailsService.loadUserByUsername(username);
             
             return new TokenAuthentication(
                 token,
                 userDetails,
                 userDetails.getAuthorities()
             );
         } else {
             throw new BadCredentialsException("Invalid or expired token");
         }
         
     } catch (Exception e) {
         throw new BadCredentialsException("Token validation failed: " + e.getMessage());
     }
 }

 @Override
 public boolean supports(Class<?> authentication) {
     return TokenAuthentication.class.isAssignableFrom(authentication);
 }
}

//5. TokenAuthentication - Token personnalisé
public class TokenAuthentication extends AbstractAuthenticationToken {
 
 private final String token;
 private final Object principal;

 public TokenAuthentication(String token) {
     super(null);
     this.token = token;
     this.principal = null;
     setAuthenticated(false);
 }
 
 public TokenAuthentication(String token, Object principal, Collection<? extends GrantedAuthority> authorities) {
     super(authorities);
     this.token = token;
     this.principal = principal;
     setAuthenticated(true);
 }

 @Override
 public Object getCredentials() {
     return token;
 }

 @Override
 public Object getPrincipal() {
     return principal;
 }
 
 public String getToken() {
     return token;
 }
}

//6. Filtre pour l'authentification par token
public class TokenAuthenticationFilter extends OncePerRequestFilter {
 
 private final AuthenticationManager authenticationManager;
 
 public TokenAuthenticationFilter(AuthenticationManager authenticationManager) {
     this.authenticationManager = authenticationManager;
 }

 @Override
 protected void doFilterInternal(HttpServletRequest request, 
                               HttpServletResponse response, 
                               FilterChain filterChain) throws ServletException, IOException {
     
     String token = extractTokenFromRequest(request);
     
     if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
         try {
             TokenAuthentication tokenAuth = new TokenAuthentication(token);
             Authentication authenticated = authenticationManager.authenticate(tokenAuth);
             SecurityContextHolder.getContext().setAuthentication(authenticated);
         } catch (AuthenticationException e) {
             // Token invalide, continuer sans authentifier
             System.err.println("Token authentication failed: " + e.getMessage());
         }
     }
     
     filterChain.doFilter(request, response);
 }
 
 private String extractTokenFromRequest(HttpServletRequest request) {
     String bearerToken = request.getHeader("Authorization");
     if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
         return bearerToken.substring(7);
     }
     return null;
 }
}

//7. Service JWT (exemple d'implémentation)
@Service
public class JwtTokenService {
 
 private static final String SECRET_KEY = "mySecretKey123456789"; // À sécuriser en production
 private static final long EXPIRATION_TIME = 86400000; // 24 heures
 
 private final SecretKey key;
 
 public JwtTokenService() {
     this.key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
 }
 
 public String generateToken(String username) {
     return Jwts.builder()
             .setSubject(username)
             .setIssuedAt(new Date())
             .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
             .signWith(key)
             .compact();
 }
 
 public String validateTokenAndGetUsername(String token) {
     try {
         Claims claims = Jwts.parserBuilder()
                 .setSigningKey(key)
                 .build()
                 .parseClaimsJws(token)
                 .getBody();
         
         return claims.getSubject();
     } catch (JwtException | IllegalArgumentException e) {
         return null;
     }
 }
}

//8. UserDetailsService personnalisé (exemple)
@Service
public class CustomUserDetailsService implements UserDetailsService {
 
 @Autowired
 private PasswordEncoder passwordEncoder;
 
 @Override
 public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
     // Ici vous devriez charger l'utilisateur depuis votre base de données
     // Exemple avec des utilisateurs en dur pour la démo
     
     if ("admin".equals(username)) {
         return User.builder()
                 .username("admin")
                 .password(passwordEncoder.encode("admin123"))
                 .authorities("ROLE_ADMIN", "ROLE_ACTUATOR", "ROLE_USER", "ROLE_SUPER_ADMIN")
                 .build();
     } else if ("user".equals(username)) {
         return User.builder()
                 .username("user")
                 .password(passwordEncoder.encode("user123"))
                 .authorities("ROLE_USER")
                 .build();
     } else if ("actuator".equals(username)) {
         return User.builder()
                 .username("actuator")
                 .password(passwordEncoder.encode("actuator123"))
                 .authorities("ROLE_ACTUATOR")
                 .build();
     } else if ("tester".equals(username)) {
         return User.builder()
                 .username("tester")
                 .password(passwordEncoder.encode("tester123"))
                 .authorities("ROLE_TESTER", "ROLE_USER")
                 .build();
     } else if ("superadmin".equals(username)) {
         return User.builder()
                 .username("superadmin")
                 .password(passwordEncoder.encode("super123"))
                 .authorities("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_ACTUATOR", "ROLE_USER")
                 .build();
     } else {
         throw new UsernameNotFoundException("User not found: " + username);
     }
 }
}

//9. Configuration des beans additionnels
@Configuration
public class AuthConfig {
 
 @Bean
 public PasswordEncoder passwordEncoder() {
     return new BCryptPasswordEncoder();
 }
}

//10. Contrôleur de test
@RestController
@RequestMapping("/api")
public class AuthTestController {
 
 @Autowired
 private JwtTokenService jwtTokenService;
 
 @Autowired
 private JEFSecurityProperties jefSecurityProperties;
 
 @Value("${spring.profiles.active:dev}")
 private String activeProfile;
 
 @PostMapping("/login")
 public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
     try {
         Map<String, String> validUsers = Map.of(
             "admin", "admin123",
             "user", "user123",
             "actuator", "actuator123"
         );
         
         if (validUsers.containsKey(loginRequest.getUsername()) && 
             validUsers.get(loginRequest.getUsername()).equals(loginRequest.getPassword())) {
             
             String token = jwtTokenService.generateToken(loginRequest.getUsername());
             Map<String, String> response = new HashMap<>();
             response.put("token", token);
             response.put("environment", activeProfile);
             return ResponseEntity.ok(response);
         } else {
             return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
         }
     } catch (Exception e) {
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
     }
 }
 
 @GetMapping("/protected")
 public ResponseEntity<Map<String, Object>> protectedEndpoint(Authentication authentication) {
     Map<String, Object> response = new HashMap<>();
     response.put("message", "Hello " + authentication.getName() + "! You are authenticated.");
     response.put("environment", activeProfile);
     response.put("authorities", authentication.getAuthorities());
     return ResponseEntity.ok(response);
 }
 
 @GetMapping("/public/health")
 public ResponseEntity<Map<String, String>> publicEndpoint() {
     Map<String, String> response = new HashMap<>();
     response.put("status", "UP");
     response.put("environment", activeProfile);
     response.put("message", "Public endpoint - no authentication required");
     return ResponseEntity.ok(response);
 }
 
 @GetMapping("/admin/users")
 public ResponseEntity<Map<String, Object>> adminEndpoint(Authentication authentication) {
     Map<String, Object> response = new HashMap<>();
     response.put("message", "Admin endpoint accessed by: " + authentication.getName());
     response.put("users", List.of("admin", "user", "actuator"));
     response.put("environment", activeProfile);
     return ResponseEntity.ok(response);
 }
 
 @PostMapping("/admin/refresh-properties")
 public ResponseEntity<Map<String, String>> refreshProperties(Authentication authentication) {
     Map<String, String> response = new HashMap<>();
     response.put("message", "Properties refreshed by: " + authentication.getName());
     response.put("timestamp", Instant.now().toString());
     response.put("status", "SUCCESS");
     return ResponseEntity.ok(response);
 }
 
 @GetMapping("/user/profile")
 public ResponseEntity<Map<String, Object>> userProfile(Authentication authentication) {
     Map<String, Object> response = new HashMap<>();
     response.put("username", authentication.getName());
     response.put("authorities", authentication.getAuthorities());
     response.put("environment", activeProfile);
     return ResponseEntity.ok(response);
 }
 
 @GetMapping("/environment")
 public ResponseEntity<Map<String, Object>> getEnvironmentInfo(Authentication authentication) {
     Map<String, Object> response = new HashMap<>();
     response.put("activeProfile", activeProfile);
     response.put("user", authentication.getName());
     response.put("actuatorAccess", getActuatorAccessInfo());
     response.put("jefSecurityConfig", getJEFSecurityInfo());
     return ResponseEntity.ok(response);
 }
 
 @GetMapping("/security-config")
 public ResponseEntity<Map<String, Object>> getSecurityConfig(Authentication authentication) {
     Map<String, Object> response = new HashMap<>();
     response.put("defaultSecurity", jefSecurityProperties.getDefaultSecurity());
     response.put("endpoints", jefSecurityProperties.getEndpoints());
     response.put("configuredEndpoints", jefSecurityProperties.getConfiguredEndpoints());
     response.put("requestedBy", authentication.getName());
     return ResponseEntity.ok(response);
 }
 
 private String getActuatorAccessInfo() {
     return switch (activeProfile.toLowerCase()) {
         case "uat" -> "All actuator endpoints are public";
         case "prod" -> "Actuator endpoints require ROLE_ACTUATOR (except /health and /info)";
         default -> "Actuator endpoints require ROLE_ADMIN";
     };
 }
 
 private Map<String, Object> getJEFSecurityInfo() {
     Map<String, Object> info = new HashMap<>();
     info.put("defaultSecurity", jefSecurityProperties.getDefaultSecurity().toString());
     info.put("endpointCount", jefSecurityProperties.getEndpoints().size());
     info.put("configuredEndpoints", jefSecurityProperties.getConfiguredEndpoints());
     return info;
 }
}

//11. DTO pour la requête de login
public class LoginRequest {
 private String username;
 private String password;
 
 // Getters et Setters
 public String getUsername() { return username; }
 public void setUsername(String username) { this.username = username; }
 public String getPassword() { return password; }
 public void setPassword(String password) { this.password = password; }
}