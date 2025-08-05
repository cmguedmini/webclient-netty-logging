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
             authz.requestMatchers("/api/public/**").permitAll();
             
             // Configuration des endpoints Actuator selon l'environnement
             if ("uat".equalsIgnoreCase(activeProfile)) {
                 // En UAT : tous les endpoints Actuator sont publics
                 authz.requestMatchers("/actuator/**").permitAll();
             } else if ("prod".equalsIgnoreCase(activeProfile)) {
                 // En PROD : endpoints Actuator sécurisés par rôle ACTUATOR
                 authz.requestMatchers("/actuator/health").permitAll() // Health toujours public
                       .requestMatchers("/actuator/info").permitAll()   // Info toujours public
                       .requestMatchers("/actuator/**").hasRole("ACTUATOR");
             } else {
                 // Autres environnements (dev, test) : accès restreint aux administrateurs
                 authz.requestMatchers("/actuator/**").hasRole("ADMIN");
             }
             
             authz.anyRequest().authenticated();
         })
         .authenticationManager(authenticationManager())
         .addFilterBefore(new TokenAuthenticationFilter(authenticationManager()), 
                        UsernamePasswordAuthenticationFilter.class)
         .httpBasic(Customizer.withDefaults())
         .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
         .csrf(csrf -> csrf.disable());

     return http.build();
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
                 .authorities("ROLE_ADMIN", "ROLE_ACTUATOR")
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
 
 @Value("${spring.profiles.active:dev}")
 private String activeProfile;
 
 @PostMapping("/login")
 public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest loginRequest) {
     // Ce endpoint pourrait utiliser l'AuthenticationManager pour valider les credentials
     // et retourner un token JWT
     
     try {
         // Simuler la validation (en production, utilisez l'AuthenticationManager)
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
 
 @GetMapping("/environment")
 public ResponseEntity<Map<String, String>> getEnvironmentInfo(Authentication authentication) {
     Map<String, String> response = new HashMap<>();
     response.put("activeProfile", activeProfile);
     response.put("user", authentication.getName());
     response.put("actuatorAccess", getActuatorAccessInfo());
     return ResponseEntity.ok(response);
 }
 
 private String getActuatorAccessInfo() {
     return switch (activeProfile.toLowerCase()) {
         case "uat" -> "All actuator endpoints are public";
         case "prod" -> "Actuator endpoints require ROLE_ACTUATOR (except /health and /info)";
         default -> "Actuator endpoints require ROLE_ADMIN";
     };
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