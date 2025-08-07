= MultiProviderAuthenticationManager et JEFSecurityLevel
:toc:
:toc-title: Sommaire
:sectnums:
:icons: font
:source-highlighter: highlightjs

[.lead]
Documentation technique pour développeurs sur l'implémentation d'un système d'authentification multi-fournisseurs avec niveaux de sécurité configurables par endpoint.

== Contexte et problématique

=== Besoin métier

Dans les environnements d'entreprise modernes, les applications doivent souvent supporter **plusieurs mécanismes d'authentification** simultanément :

* Authentification basique (BasicAuthentication) pour les utilisateurs standards
* Authentification par token (TokenAuthentication) pour les API et services
* Support multi-fournisseurs avec mécanisme de fallback

De plus, chaque **endpoint nécessite des niveaux de sécurité configurables** :

* Endpoints publics (`permitAll()`)
* Endpoints nécessitant une authentification simple (`authenticated()`)  
* Endpoints avec contrôle d'accès par rôles (`hasRole()`, `hasAnyRole()`)
* Configuration différenciée par environnement (dev, uat, prod)

=== Limitations de Spring Security par défaut

Spring Security propose `ProviderManager` qui utilise une liste de fournisseurs, mais présente des limitations :

* Arrêt au premier échec au lieu de tenter les autres fournisseurs
* Pas de gestion native des niveaux de sécurité
* Configuration complexe pour des scénarios avancés

== Architecture de la solution

=== MultiProviderAuthenticationManager

[source,java]
----
@Component
public class MultiProviderAuthenticationManager implements AuthenticationManager {
    
    private final List<AuthenticationProvider> providers;
    
    @Override
    public Authentication authenticate(Authentication auth) throws AuthenticationException {
        AuthenticationException lastException = null;
        
        for (AuthenticationProvider provider : providers) {
            if (provider.supports(auth.getClass())) {
                try {
                    return provider.authenticate(auth);
                } catch (AuthenticationException e) {
                    lastException = e;
                    // Continue avec le prochain fournisseur
                }
            }
        }
        
        throw lastException != null ? lastException : 
            new ProviderNotFoundException("Aucun fournisseur disponible");
    }
}
----

=== JEFSecurityLevel

[source,java]
----
public class JEFSecurityLevel {
    private boolean permit = false;
    private List<String> roles = new ArrayList<>();
    
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
    
    // Méthodes utilitaires
    public boolean isPublic() { return permit; }
    public boolean requiresAuthentication() { return !permit; }
    public boolean hasSpecificRoles() { return !permit && !roles.isEmpty(); }
}
----

== Fonctionnalités clés

=== Mécanisme de fallback

Le `MultiProviderAuthenticationManager` tente **tous les fournisseurs compatibles** dans l'ordre :

1. **Vérification du support** : `provider.supports(authClass)`
2. **Tentative d'authentification** : Si échec, passe au suivant
3. **Retour du premier succès** ou de la dernière exception

=== Configuration par endpoint avec JEFSecurityProperties

La sécurité est configurée de manière **déclarative via des properties** externes :

[source,java]
----
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(authz -> {
        // Configuration des endpoints spécifiques via la map
        Map<String, JEFSecurityLevel> endpointSecurity = jefSecurityProperties.getEndpoints();
        
        for (Map.Entry<String, JEFSecurityLevel> entry : endpointSecurity.entrySet()) {
            String endpoint = entry.getKey();
            JEFSecurityLevel securityLevel = entry.getValue();
            configureEndpointSecurity(authz, endpoint, securityLevel);
        }
        
        // Configuration des endpoints Actuator selon l'environnement
        configureActuatorSecurity(authz);
        
        // Application du niveau de sécurité par défaut
        JEFSecurityLevel defaultSecurity = jefSecurityProperties.getDefaultSecurity();
        configureDefaultSecurity(authz, defaultSecurity);
    })
    .authenticationManager(authenticationManager())
    .addFilterBefore(new TokenAuthenticationFilter(authenticationManager()), 
                   UsernamePasswordAuthenticationFilter.class);
}
----

=== Logique de configuration des endpoints

[source,java]
----
private void configureEndpointSecurity(AuthorizeHttpRequestsConfigurer authz, 
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
----

=== Configuration Spring

[source,java]
----
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public MultiProviderAuthenticationManager authManager(
            @Qualifier("basicAuthenticationProvider") AuthenticationProvider basic,
            @Qualifier("tokenAuthenticationProvider") AuthenticationProvider token) {
        
        return new MultiProviderAuthenticationManager(
            Arrays.asList(basic, token)
        );
    }
}
----

== Cas d'usage pratiques

=== Authentification en cascade

[cols="1,2,3"]
|===
|Ordre |Fournisseur |Cas d'utilisation

|1 |Basic Authentication Provider |Authentification username/password classique
|2 |Token Authentication Provider |Authentification par token JWT/API Key
|===

=== Configuration des endpoints par environnement

==== Exemple de configuration YAML

[source,yaml]
----
jef:
  security:
    default-security:
      permit: false
      roles: []  # authenticated() par défaut
    endpoints:
      "/api/public/**":
        permit: true
      "/api/user/**":
        permit: false
        roles: []  # authenticated()
      "/api/admin/**":
        permit: false
        roles: ["ADMIN"]
      "/api/financial/**":
        permit: false
        roles: ["ADMIN", "FINANCIAL_OFFICER"]
----

==== Configuration des endpoints Actuator par environnement

[cols="2,3,3"]
|===
|Environnement |Configuration |Justification

|**UAT** |`/actuator/**` → `permitAll()` |Facilite les tests et monitoring
|**PROD** |`/actuator/health,info` → `permitAll()` +
`/actuator/**` → `hasRole("ACTUATOR")` |Sécurité renforcée en production
|**DEV/TEST** |`/actuator/**` → `hasRole("ADMIN")` |Accès restreint aux développeurs
|===

== Implémentation recommandée

=== Structure des packages

[source]
----
com.company.security/
├── manager/
│   └── MultiProviderAuthenticationManager.java
├── level/
│   ├── JEFSecurityLevel.java
│   └── JEFSecurityProperties.java
├── provider/
│   ├── BasicAuthenticationProvider.java
│   └── TokenAuthenticationProvider.java
├── filter/
│   └── TokenAuthenticationFilter.java
└── config/
    └── SecurityConfig.java
----

=== Tests unitaires essentiels

* Test du mécanisme de fallback entre BasicAuth et TokenAuth
* Validation de la configuration des endpoints par JEFSecurityLevel
* Test des factory methods de JEFSecurityLevel
* Validation de la configuration Actuator par environnement
* Test de l'extraction des noms de rôles (suppression préfixe ROLE_)

== Bonnes pratiques

WARNING: **Sécurité** : Les niveaux de sécurité sont appliqués côté serveur via Spring Security. La configuration par properties évite le hard-coding.

TIP: **Performance** : Ordonner les fournisseurs du plus fréquemment utilisé (Basic) au moins fréquent (Token).

IMPORTANT: **Configuration** : Utiliser les factory methods de JEFSecurityLevel pour éviter les erreurs : `permitAll()`, `authenticated()`, `hasRole()`, `hasAnyRole()`.

=== Exemple d'utilisation dans un contrôleur

[source,java]
----
@RestController
@RequestMapping("/api/secure")
public class SecureController {
    
    @Autowired
    private MultiProviderAuthenticationManager authManager;
    
    // Endpoint sécurisé par configuration externe (JEFSecurityProperties)
    @GetMapping("/admin/users")
    public ResponseEntity<List<User>> getUsers(Authentication auth) {
        // Logique métier
        return ResponseEntity.ok(users);
    }
    
    // Endpoint avec sécurité programmatique additionnelle
    @PreAuthorize("hasRole('ADMIN') and authentication.name == 'superadmin'")
    @PostMapping("/system/config")
    public ResponseEntity<String> updateSystemConfig(Authentication auth) {
        // Configuration système critique
        return ResponseEntity.ok("Configuration updated");
    }
}
----

=== Exemple de JEFSecurityProperties

[source,java]
----
@ConfigurationProperties(prefix = "jef.security")
@Component
public class JEFSecurityProperties {
    
    private JEFSecurityLevel defaultSecurity = JEFSecurityLevel.authenticated();
    private Map<String, JEFSecurityLevel> endpoints = new HashMap<>();
    
    // Configuration programmatique possible
    @PostConstruct
    public void init() {
        // Endpoints publics
        endpoints.put("/api/public/**", JEFSecurityLevel.permitAll());
        endpoints.put("/api/health", JEFSecurityLevel.permitAll());
        
        // Endpoints avec rôles spécifiques
        endpoints.put("/api/admin/**", JEFSecurityLevel.hasRole("ADMIN"));
        endpoints.put("/api/financial/**", JEFSecurityLevel.hasAnyRole("ADMIN", "FINANCIAL_OFFICER"));
    }
    
    // Getters/Setters...
}
----

== Conclusion

Cette architecture offre une solution **flexible et configurable** pour gérer l'authentification multi-sources avec des niveaux de sécurité définis par endpoint, permettant une configuration externe sans redéploiement et une adaptation automatique selon l'environnement d'exécution.

---
_Document technique v1.0 - Équipe Sécurité & Architecture_