// ===== Test d'intégration complet =====

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "app.security.actuator.debug=true",
    "management.endpoints.web.exposure.include=*"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ActuatorSecurityService securityService;

    @MockBean
    private UserDetailsService userDetailsService;

    private static final String BASIC_AUTH_ADMIN = "Basic " + 
            Base64.getEncoder().encodeToString("admin:password".getBytes());
    
    private static final String BASIC_AUTH_USER = "Basic " + 
            Base64.getEncoder().encodeToString("user:password".getBytes());

    @BeforeEach
    void setUp() {
        // Mock des utilisateurs avec rôles
        UserDetails adminUser = User.builder()
                .username("admin")
                .password("{noop}password")
                .roles("ADMIN", "USER")
                .build();

        UserDetails regularUser = User.builder()
                .username("user")
                .password("{noop}password")
                .roles("USER")
                .build();

        UserDetails qaUser = User.builder()
                .username("qauser")
                .password("{noop}password")
                .roles("QA_USER", "QA_ADMIN")
                .build();

        when(userDetailsService.loadUserByUsername("admin")).thenReturn(adminUser);
        when(userDetailsService.loadUserByUsername("user")).thenReturn(regularUser);
        when(userDetailsService.loadUserByUsername("qauser")).thenReturn(qaUser);
    }

    @Nested
    @DisplayName("Tests pour l'environnement DEVELOP")
    @TestPropertySource(properties = {"spring.profiles.active=develop"})
    class DevelopEnvironmentTests {

        @Test
        @DisplayName("GET /actuator/health doit être accessible sans authentification")
        void healthEndpoint_ShouldBeAccessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("POST /actuator/health doit nécessiter une authentification admin")
        void healthEndpointPost_ShouldRequireAdminAuth() throws Exception {
            // Sans auth - doit échouer
            mockMvc.perform(post("/actuator/health"))
                    .andExpect(status().isUnauthorized());

            // Avec auth user - doit échouer
            mockMvc.perform(post("/actuator/health")
                    .header("Authorization", BASIC_AUTH_USER))
                    .andExpect(status().isForbidden());

            // Avec auth admin - doit réussir
            mockMvc.perform(post("/actuator/health")
                    .header("Authorization", BASIC_AUTH_ADMIN))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /actuator/info doit être accessible sans authentification")
        void infoEndpoint_ShouldBeAccessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /actuator/beans doit nécessiter le rôle DEVELOPER")
        void beansEndpoint_ShouldRequireDeveloperRole() throws Exception {
            // Sans auth - doit échouer
            mockMvc.perform(get("/actuator/beans"))
                    .andExpect(status().isUnauthorized());

            // Avec auth user normal - doit échouer
            mockMvc.perform(get("/actuator/beans")
                    .header("Authorization", BASIC_AUTH_USER))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /actuator/metrics/** doit être accessible sans authentification")
        void metricsEndpoint_ShouldBeAccessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/actuator/metrics"))
                    .andExpect(status().isOk());
            
            mockMvc.perform(get("/actuator/metrics/jvm.memory.used"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Tests pour l'environnement QA")
    @TestPropertySource(properties = {"spring.profiles.active=qa"})
    class QAEnvironmentTests {

        @Test
        @DisplayName("GET /actuator/health doit être accessible sans authentification")
        void healthEndpoint_ShouldBeAccessibleWithoutAuth() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpected(status().isOk());
        }

        @Test
        @DisplayName("GET /actuator/info doit nécessiter le rôle QA_USER")
        void infoEndpoint_ShouldRequireQAUserRole() throws Exception {
            // Sans auth - doit échouer
            mockMvc.perform(get("/actuator/info"))
                    .andExpect(status().isUnauthorized());

            // Avec auth user normal - doit échouer
            mockMvc.perform(get("/actuator/info")
                    .header("Authorization", BASIC_AUTH_USER))
                    .andExpect(status().isForbidden());

            // Avec auth QA user - doit réussir
            mockMvc.perform(get("/actuator/info")
                    .header("Authorization", BASIC_AUTH_QA_USER))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /actuator/beans doit nécessiter le rôle QA_ADMIN")
        void beansEndpoint_ShouldRequireQAAdminRole() throws Exception {
            // Sans auth - doit échouer
            mockMvc.perform(get("/actuator/beans"))
                    .andExpected(status().isUnauthorized());

            // Avec auth QA user - doit échouer (besoin QA_ADMIN)
            mockMvc.perform(get("/actuator/beans")
                    .header("Authorization", BASIC_AUTH_QA_USER))
                    .andExpected(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Tests pour l'environnement PROD")
    @TestPropertySource(properties = {"spring.profiles.active=prod"})
    class ProdEnvironmentTests {

        @Test
        @DisplayName("GET /actuator/health doit nécessiter le rôle MONITORING")
        void healthEndpoint_ShouldRequireMonitoringRole() throws Exception {
            // Sans auth - doit échouer
            mockMvc.perform(get("/actuator/health"))
                    .andExpected(status().isUnauthorized());

            // Avec auth user normal - doit échouer
            mockMvc.perform(get("/actuator/health")
                    .header("Authorization", BASIC_AUTH_USER))
                    .andExpected(status().isForbidden());
        }

        @Test
        @DisplayName("POST /actuator/health doit nécessiter le rôle ADMIN")
        void healthEndpointPost_ShouldRequireAdminRole() throws Exception {
            mockMvc.perform(post("/actuator/health")
                    .header("Authorization", BASIC_AUTH_ADMIN))
                    .andExpected(status().isOk());
        }

        @Test
        @DisplayName("GET /actuator/beans doit nécessiter le rôle ADMIN")
        void beansEndpoint_ShouldRequireAdminRole() throws Exception {
            mockMvc.perform(get("/actuator/beans")
                    .header("Authorization", BASIC_AUTH_ADMIN))
                    .andExpected(status().isOk());
        }
    }
}

// ===== Test unitaire du service =====

@ExtendWith(MockitoExtension.class)
class ActuatorSecurityServiceTest {

    @Mock
    private Environment environment;

    @InjectMocks
    private ActuatorSecurityService securityService;

    private ActuatorSecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = createTestProperties();
        securityService = new ActuatorSecurityService(properties, environment);
    }

    @Test
    @DisplayName("Doit déterminer l'environnement DEVELOP par défaut")
    void shouldDetermineDefaultEnvironment() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{});
        
        ActuatorSecurityService service = new ActuatorSecurityService(properties, environment);
        
        assertEquals("DEVELOP", service.getCurrentEnvironment());
    }

    @Test
    @DisplayName("Doit déterminer l'environnement à partir du profil actif")
    void shouldDetermineEnvironmentFromActiveProfile() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"qa"});
        
        ActuatorSecurityService service = new ActuatorSecurityService(properties, environment);
        
        assertEquals("QA", service.getCurrentEnvironment());
    }

    @Test
    @DisplayName("Doit trouver la règle de sécurité pour une URL exacte")
    void shouldFindSecurityRuleForExactUrl() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"develop"});
        ActuatorSecurityService service = new ActuatorSecurityService(properties, environment);
        
        Optional<ActuatorSecurityProperties.SecurityRule> rule = 
                service.getSecurityRule("/actuator/health", "GET");
        
        assertTrue(rule.isPresent());
        assertFalse(rule.get().isAuthenticated());
    }

    @Test
    @DisplayName("Doit trouver la règle de sécurité pour une URL avec wildcard")
    void shouldFindSecurityRuleForWildcardUrl() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"develop"});
        ActuatorSecurityService service = new ActuatorSecurityService(properties, environment);
        
        Optional<ActuatorSecurityProperties.SecurityRule> rule = 
                service.getSecurityRule("/actuator/metrics/jvm.memory.used", "GET");
        
        assertTrue(rule.isPresent());
        assertFalse(rule.get().isAuthenticated());
    }

    @Test
    @DisplayName("Doit retourner empty si aucune règle trouvée")
    void shouldReturnEmptyIfNoRuleFound() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"develop"});
        ActuatorSecurityService service = new ActuatorSecurityService(properties, environment);
        
        Optional<ActuatorSecurityProperties.SecurityRule> rule = 
                service.getSecurityRule("/actuator/unknown", "GET");
        
        assertFalse(rule.isPresent());
    }

    @Test
    @DisplayName("Doit retourner la règle par défaut pour l'environnement")
    void shouldReturnDefaultRuleForEnvironment() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        ActuatorSecurityService service = new ActuatorSecurityService(properties, environment);
        
        ActuatorSecurityProperties.SecurityRule defaultRule = service.getDefaultSecurityRule();
        
        assertTrue(defaultRule.isAuthenticated());
        assertEquals(Arrays.asList("ROLE_ADMIN"), defaultRule.getRoles());
    }

    private ActuatorSecurityProperties createTestProperties() {
        ActuatorSecurityProperties props = new ActuatorSecurityProperties();
        
        // Configuration des environnements
        Map<String, ActuatorSecurityProperties.EnvironmentConfig> envs = new HashMap<>();
        
        ActuatorSecurityProperties.EnvironmentConfig developConfig = 
                new ActuatorSecurityProperties.EnvironmentConfig();
        developConfig.setDefaultAuthenticated(false);
        developConfig.setDefaultRoles(Arrays.asList());
        envs.put("DEVELOP", developConfig);
        
        ActuatorSecurityProperties.EnvironmentConfig prodConfig = 
                new ActuatorSecurityProperties.EnvironmentConfig();
        prodConfig.setDefaultAuthenticated(true);
        prodConfig.setDefaultRoles(Arrays.asList("ROLE_ADMIN"));
        envs.put("PROD", prodConfig);
        
        props.setEnvironments(envs);
        
        // Configuration des endpoints
        List<ActuatorSecurityProperties.EndpointConfig> endpoints = new ArrayList<>();
        
        // Endpoint /actuator/health
        ActuatorSecurityProperties.EndpointConfig healthEndpoint = 
                new ActuatorSecurityProperties.EndpointConfig();
        healthEndpoint.setUrl("/actuator/health");
        
        Map<String, ActuatorSecurityProperties.EndpointEnvironmentConfig> healthEnvs = new HashMap<>();
        
        ActuatorSecurityProperties.EndpointEnvironmentConfig healthDevelop = 
                new ActuatorSecurityProperties.EndpointEnvironmentConfig();
        
        ActuatorSecurityProperties.SecurityRule healthGetRule = 
                new ActuatorSecurityProperties.SecurityRule();
        healthGetRule.setMethods(Arrays.asList("GET"));
        healthGetRule.setAuthenticated(false);
        
        healthDevelop.setRules(Arrays.asList(healthGetRule));
        healthEnvs.put("DEVELOP", healthDevelop);
        
        healthEndpoint.setEnvironments(healthEnvs);
        endpoints.add(healthEndpoint);
        
        // Endpoint /actuator/metrics/**
        ActuatorSecurityProperties.EndpointConfig metricsEndpoint = 
                new ActuatorSecurityProperties.EndpointConfig();
        metricsEndpoint.setUrl("/actuator/metrics/**");
        
        Map<String, ActuatorSecurityProperties.EndpointEnvironmentConfig> metricsEnvs = new HashMap<>();
        
        ActuatorSecurityProperties.EndpointEnvironmentConfig metricsDevelop = 
                new ActuatorSecurityProperties.EndpointEnvironmentConfig();
        
        ActuatorSecurityProperties.SecurityRule metricsGetRule = 
                new ActuatorSecurityProperties.SecurityRule();
        metricsGetRule.setMethods(Arrays.asList("GET"));
        metricsGetRule.setAuthenticated(false);
        
        metricsDevelop.setRules(Arrays.asList(metricsGetRule));
        metricsEnvs.put("DEVELOP", metricsDevelop);
        
        metricsEndpoint.setEnvironments(metricsEnvs);
        endpoints.add(metricsEndpoint);
        
        props.setEndpoints(endpoints);
        
        return props;
    }
}

// ===== Test de configuration Spring Boot =====

@TestConfiguration
@EnableConfigurationProperties(ActuatorSecurityProperties.class)
public class TestSecurityConfiguration {

    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password("{noop}password")
                .roles("ADMIN", "USER", "MONITORING")
                .build();

        UserDetails user = User.builder()
                .username("user")
                .password("{noop}password")
                .roles("USER")
                .build();

        UserDetails qaUser = User.builder()
                .username("qauser")
                .password("{noop}password")
                .roles("QA_USER", "QA_ADMIN")
                .build();

        UserDetails developer = User.builder()
                .username("developer")
                .password("{noop}password")
                .roles("DEVELOPER", "USER")
                .build();

        return new InMemoryUserDetailsManager(admin, user, qaUser, developer);
    }
}

// ===== Test des propriétés de configuration =====

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class ActuatorSecurityPropertiesTest {

    @Autowired
    private ActuatorSecurityProperties properties;

    @Test
    @DisplayName("Doit charger la configuration depuis application-test.yml")
    void shouldLoadConfigurationFromYaml() {
        assertNotNull(properties);
        assertFalse(properties.getEnvironments().isEmpty());
        assertFalse(properties.getEndpoints().isEmpty());
    }

    @Test
    @DisplayName("Doit avoir la configuration des environnements")
    void shouldHaveEnvironmentConfiguration() {
        assertTrue(properties.getEnvironments().containsKey("DEVELOP"));
        assertTrue(properties.getEnvironments().containsKey("QA"));
        assertTrue(properties.getEnvironments().containsKey("PROD"));
        
        ActuatorSecurityProperties.EnvironmentConfig developConfig = 
                properties.getEnvironments().get("DEVELOP");
        assertFalse(developConfig.isDefaultAuthenticated());
        assertTrue(developConfig.getDefaultRoles().isEmpty());
    }

    @Test
    @DisplayName("Doit avoir la configuration des endpoints")
    void shouldHaveEndpointConfiguration() {
        List<ActuatorSecurityProperties.EndpointConfig> endpoints = properties.getEndpoints();
        
        assertTrue(endpoints.stream()
                .anyMatch(endpoint -> "/actuator/health".equals(endpoint.getUrl())));
        
        assertTrue(endpoints.stream()
                .anyMatch(endpoint -> "/actuator/metrics/**".equals(endpoint.getUrl())));
    }
}

// ===== Fichier de test application-test.yml =====
/*
app:
  security:
    actuator:
      environments:
        DEVELOP:
          default-authenticated: false
          default-roles: []
        QA:
          default-authenticated: true
          default-roles: ["ROLE_QA_USER"]
        PROD:
          default-authenticated: true
          default-roles: ["ROLE_ADMIN"]
      endpoints:
        - url: "/actuator/health"
          environments:
            DEVELOP:
              rules:
                - methods: ["GET"]
                  authenticated: false
            QA:
              rules:
                - methods: ["GET"]
                  authenticated: false
            PROD:
              rules:
                - methods: ["GET"]
                  authenticated: true
                  roles: ["ROLE_MONITORING"]
        - url: "/actuator/metrics/**"
          environments:
            DEVELOP:
              rules:
                - methods: ["GET"]
                  authenticated: false

management:
  endpoints:
    web:
      exposure:
        include: "*"
*/
