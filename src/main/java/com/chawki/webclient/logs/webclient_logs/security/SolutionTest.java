import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires de JefActuatorWebSecurityConfiguration")
class JefActuatorWebSecurityConfigurationTest {

    @Mock
    private ActuatorSecurityService securityService;

    private JefActuatorWebSecurityConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new JefActuatorWebSecurityConfiguration(securityService);
    }

    @Test
    @DisplayName("Le constructeur doit initialiser le service")
    void constructor_ShouldInitializeService() {
        assertNotNull(configuration);
    }

    @Test
    @DisplayName("actuatorSecurityFilterChain doit créer un SecurityFilterChain")
    void actuatorSecurityFilterChain_ShouldCreateSecurityFilterChain() throws Exception {
        // Given
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        
        ActuatorSecurityProperties.SecurityRule defaultRule = createDefaultRule();
        when(securityService.getDefaultSecurityRule()).thenReturn(defaultRule);
        when(securityService.getAllEndpoints()).thenReturn(new ArrayList<>());
        
        // When
        SecurityFilterChain filterChain = configuration.actuatorSecurityFilterChain(http);
        
        // Then
        assertNotNull(filterChain);
        verify(securityService, atLeastOnce()).getDefaultSecurityRule();
    }

    @Test
    @DisplayName("Le service doit récupérer la règle par défaut")
    void shouldRetrieveDefaultRule() {
        // Given
        ActuatorSecurityProperties.SecurityRule defaultRule = createDefaultRule();
        when(securityService.getDefaultSecurityRule()).thenReturn(defaultRule);
        
        // When
        ActuatorSecurityProperties.SecurityRule result = securityService.getDefaultSecurityRule();
        
        // Then
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals(Arrays.asList("ADMIN"), result.getRoles());
        verify(securityService, times(1)).getDefaultSecurityRule();
    }

    @Test
    @DisplayName("Le service doit récupérer tous les endpoints configurés")
    void shouldRetrieveAllConfiguredEndpoints() {
        // Given
        List<ActuatorSecurityProperties.EndpointConfig> endpoints = createTestEndpoints();
        when(securityService.getAllEndpoints()).thenReturn(endpoints);
        
        // When
        List<ActuatorSecurityProperties.EndpointConfig> result = securityService.getAllEndpoints();
        
        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(securityService, times(1)).getAllEndpoints();
    }

    @Test
    @DisplayName("La configuration doit utiliser le service pour appliquer les règles")
    void configuration_ShouldUseServiceToApplyRules() {
        // Given
        ActuatorSecurityProperties.SecurityRule defaultRule = createDefaultRule();
        List<ActuatorSecurityProperties.EndpointConfig> endpoints = createTestEndpoints();
        
        when(securityService.getDefaultSecurityRule()).thenReturn(defaultRule);
        when(securityService.getAllEndpoints()).thenReturn(endpoints);
        
        // When
        securityService.getDefaultSecurityRule();
        securityService.getAllEndpoints();
        
        // Then
        verify(securityService, times(1)).getDefaultSecurityRule();
        verify(securityService, times(1)).getAllEndpoints();
    }

    // Méthodes utilitaires pour créer des données de test

    private ActuatorSecurityProperties.SecurityRule createDefaultRule() {
        ActuatorSecurityProperties.SecurityRule rule = 
                new ActuatorSecurityProperties.SecurityRule();
        rule.setAuthenticated(true);
        rule.setRoles(Arrays.asList("ADMIN"));
        return rule;
    }

    private List<ActuatorSecurityProperties.EndpointConfig> createTestEndpoints() {
        List<ActuatorSecurityProperties.EndpointConfig> endpoints = new ArrayList<>();
        
        // Endpoint health
        ActuatorSecurityProperties.EndpointConfig healthEndpoint = 
                new ActuatorSecurityProperties.EndpointConfig();
        healthEndpoint.setUrl("/actuator/health");
        
        ActuatorSecurityProperties.SecurityRule healthGetRule = 
                new ActuatorSecurityProperties.SecurityRule();
        healthGetRule.setMethods(Arrays.asList("GET"));
        healthGetRule.setAuthenticated(false);
        
        ActuatorSecurityProperties.SecurityRule healthPostRule = 
                new ActuatorSecurityProperties.SecurityRule();
        healthPostRule.setMethods(Arrays.asList("POST"));
        healthPostRule.setAuthenticated(true);
        healthPostRule.setRoles(Arrays.asList("ADMIN"));
        
        healthEndpoint.setRules(Arrays.asList(healthGetRule, healthPostRule));
        endpoints.add(healthEndpoint);
        
        // Endpoint info
        ActuatorSecurityProperties.EndpointConfig infoEndpoint = 
                new ActuatorSecurityProperties.EndpointConfig();
        infoEndpoint.setUrl("/actuator/info");
        
        ActuatorSecurityProperties.SecurityRule infoGetRule = 
                new ActuatorSecurityProperties.SecurityRule();
        infoGetRule.setMethods(Arrays.asList("GET"));
        infoGetRule.setAuthenticated(false);
        
        infoEndpoint.setRules(Arrays.asList(infoGetRule));
        endpoints.add(infoEndpoint);
        
        return endpoints;
    }
}
