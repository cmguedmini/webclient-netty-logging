
// src/test/java/com/example/security/TestSupportConfig.java
package com.example.security;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@TestConfiguration
public class TestSupportConfig {

    @Bean
    JefContextService jefContextService() {
        return Mockito.mock(JefContextService.class);
    }

    @Bean
    HttpSecurityConfigurationService httpSecurityConfigurationService() {
        return Mockito.mock(HttpSecurityConfigurationService.class);
    }

    @Bean
    JefCoreProperties jefCoreProperties() {
        return Mockito.mock(JefCoreProperties.class);
    }

    @Bean
    UrlBasedCorsConfigurationSource corsRegistry() {
        // Pas besoin de mocker, un objet réel suffit
        return new UrlBasedCorsConfigurationSource();
    }

    @Bean
    JefSecurityProperties jefSecurityProperties() {
        JefSecurityProperties props = Mockito.mock(JefSecurityProperties.class);

        // ⚠️ Remplacer "ActuatorSecurityProperties" par le type réel de getActuator()
        ActuatorSecurityProperties actuatorProps = Mockito.mock(ActuatorSecurityProperties.class);
        Mockito.when(props.getActuator()).thenReturn(actuatorProps);

        return props;
    }
}
