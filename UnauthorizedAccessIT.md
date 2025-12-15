
// src/test/java/com/example/security/UnauthorizedAccessIT.java
package com.example.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.result.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@ExtendWith(SpringExtension.class)
@EnableAutoConfiguration // active les autoconfigs Spring Boot, dont Actuator
@ContextConfiguration(classes = {
        // Ta config sécurité à tester
        JefActuatorWebSecurityConfiguration.class,
        // Users/password encoder pour les tests
        TestUsersConfig.class
})
@Import({
        // Si ta config dépend d’autres beans de ton projet, ajoute-les ici
})
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.yml")
class UnauthorizedAccessIT {

    private MockMvc mockMvc;

    private final WebApplicationContext context;

    UnauthorizedAccessIT(WebApplicationContext context) {
        this.context = context;
    }

    @BeforeEach
    void setupMockMvc() {
        this.mockMvc = webAppContextSetup(context)
                .apply(springSecurity()) // intègre Spring Security dans MockMvc
                .build();
    }

    @Test
    @DisplayName("Accès anonyme à /actuator/beans -> 401 + WWW-Authenticate: Basic")
    void protected_actuator_endpoint_unauthenticated_results_in_401_and_authenticate_header() throws Exception {
        mockMvc.perform(get("/actuator/beans").with(anonymous()))
               .andExpect(status().isUnauthorized())
               .andExpect(unauthenticated())
               .andExpect(header().string("WWW-Authenticate", startsWith("Basic")));
    }
}
