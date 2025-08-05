package com.chawki.webclient.logs.webclient_logs.security;

//Configuration Actuator par environnement
@Configuration
@Slf4j
public class ActuatorConfig {

 @Value("${spring.profiles.active:dev}")
 private String activeProfile;

 @PostConstruct
 public void logActuatorConfiguration() {
     String actuatorPolicy = switch (activeProfile.toLowerCase()) {
         case "uat" -> "All actuator endpoints are PUBLIC - no authentication required";
         case "prod" -> "Actuator endpoints are SECURED - ROLE_ACTUATOR required (except /health and /info)";
         default -> "Actuator endpoints require ROLE_ADMIN";
     };
     
     log.info("=== ACTUATOR SECURITY CONFIGURATION ===");
     log.info("Active Profile: {}", activeProfile);
     log.info("Security Policy: {}", actuatorPolicy);
     log.info("======================================");
 }
}

//Endpoint personnalisé pour afficher la configuration de sécurité
@Component
@Endpoint(id = "security-info")
public class SecurityInfoEndpoint {

 @Value("${spring.profiles.active:dev}")
 private String activeProfile;

 @ReadOperation
 public Map<String, Object> securityInfo() {
     Map<String, Object> info = new HashMap<>();
     info.put("activeProfile", activeProfile);
     info.put("timestamp", Instant.now());
     
     Map<String, String> actuatorSecurity = new HashMap<>();
     
     switch (activeProfile.toLowerCase()) {
         case "uat" -> {
             actuatorSecurity.put("policy", "PUBLIC_ACCESS");
             actuatorSecurity.put("description", "All actuator endpoints are accessible without authentication");
             actuatorSecurity.put("reason", "UAT environment for testing purposes");
         }
         case "prod" -> {
             actuatorSecurity.put("policy", "ROLE_BASED_ACCESS");
             actuatorSecurity.put("description", "Actuator endpoints require ROLE_ACTUATOR");
             actuatorSecurity.put("exceptions", "/health and /info are always public");
             actuatorSecurity.put("reason", "Production environment with restricted access");
         }
         default -> {
             actuatorSecurity.put("policy", "ADMIN_ACCESS");
             actuatorSecurity.put("description", "Actuator endpoints require ROLE_ADMIN");
             actuatorSecurity.put("reason", "Development/Test environment with admin restriction");
         }
     }
     
     info.put("actuatorSecurity", actuatorSecurity);
     
     return info;
 }
}

//Configuration des Health Indicators personnalisés
@Component
public class CustomHealthIndicator implements HealthIndicator {

 @Value("${spring.profiles.active:dev}")
 private String activeProfile;

 @Override
 public Health health() {
     Map<String, Object> details = new HashMap<>();
     details.put("environment", activeProfile);
     details.put("securityMode", getSecurityMode());
     details.put("timestamp", Instant.now());
     
     return Health.up()
             .withDetails(details)
             .build();
 }
 
 private String getSecurityMode() {
     return switch (activeProfile.toLowerCase()) {
         case "uat" -> "OPEN_ACCESS";
         case "prod" -> "SECURED_ACCESS";
         default -> "RESTRICTED_ACCESS";
     };
 }
}

//Configuration pour personnaliser les réponses selon l'environnement
@Configuration
public class ActuatorCustomizationConfig {

 @Value("${spring.profiles.active:dev}")
 private String activeProfile;

 @Bean
 @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "prod")
 public HealthEndpointGroupsRegistrar prodHealthGroupsRegistrar() {
     return (groups, names) -> {
         // En production, créer des groupes de health checks spécifiques
         groups.add("readiness", "db", "diskSpace");
         groups.add("liveness", "ping");
     };
 }

 @Bean
 @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "uat")
 public InfoContributor uatInfoContributor() {
     return builder -> {
         Map<String, Object> uatInfo = new HashMap<>();
         uatInfo.put("environment", "UAT");
         uatInfo.put("securityNote", "All actuator endpoints are public in UAT");
         uatInfo.put("testingPurpose", true);
         builder.withDetail("uat", uatInfo);
     };
 }

 @Bean
 @ConditionalOnProperty(name = "spring.profiles.active", havingValue = "prod")
 public InfoContributor prodInfoContributor() {
     return builder -> {
         Map<String, Object> prodInfo = new HashMap<>();
         prodInfo.put("environment", "PRODUCTION");
         prodInfo.put("securityNote", "Actuator endpoints are secured with ROLE_ACTUATOR");
         prodInfo.put("highSecurity", true);
         builder.withDetail("production", prodInfo);
     };
 }
}

//Service pour gérer les politiques de sécurité par environnement
@Service
@Slf4j
public class EnvironmentSecurityService {

 @Value("${spring.profiles.active:dev}")
 private String activeProfile;

 public boolean isActuatorPublic() {
     boolean isPublic = "uat".equalsIgnoreCase(activeProfile);
     log.debug("Actuator public access for profile '{}': {}", activeProfile, isPublic);
     return isPublic;
 }

 public boolean requiresActuatorRole() {
     boolean requiresRole = "prod".equalsIgnoreCase(activeProfile);
     log.debug("Actuator role requirement for profile '{}': {}", activeProfile, requiresRole);
     return requiresRole;
 }

 public String getRequiredRole() {
     return switch (activeProfile.toLowerCase()) {
         case "prod" -> "ROLE_ACTUATOR";
         case "uat" -> null; // Pas de rôle requis
         default -> "ROLE_ADMIN";
     };
 }

 public List<String> getPublicActuatorEndpoints() {
     return switch (activeProfile.toLowerCase()) {
         case "uat" -> List.of("/actuator/**"); // Tous publics
         case "prod" -> List.of("/actuator/health", "/actuator/info");
         default -> List.of("/actuator/health");
     };
 }

 @EventListener
 public void handleContextRefresh(ContextRefreshedEvent event) {
     log.info("=== ENVIRONMENT SECURITY SUMMARY ===");
     log.info("Profile: {}", activeProfile);
     log.info("Actuator Public: {}", isActuatorPublic());
     log.info("Required Role: {}", getRequiredRole());
     log.info("Public Endpoints: {}", getPublicActuatorEndpoints());
     log.info("===================================");
 }
}
