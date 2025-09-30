package com.example.config;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.Validation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SslPropertiesTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    void shouldAcceptValidSslProtocols() {
        SslProperties props = new SslProperties();
        props.setSslProtocols(List.of("TLSv1.3", "TLSv1.2"));

        Set<ConstraintViolation<SslProperties>> violations = validator.validate(props);

        assertTrue(violations.isEmpty(), "Valid protocols should not produce violations");
    }

    @Test
    void shouldRejectNullSslProtocolsList() {
        SslProperties props = new SslProperties();
        props.setSslProtocols(null);

        Set<ConstraintViolation<SslProperties>> violations = validator.validate(props);

        assertFalse(violations.isEmpty(), "Null list should be rejected");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("must not be null") || 
                              v.getMessage().contains("ne doit pas être nul")));
    }

    @Test
    void shouldRejectEmptySslProtocolsList() {
        SslProperties props = new SslProperties();
        props.setSslProtocols(Collections.emptyList());

        Set<ConstraintViolation<SslProperties>> violations = validator.validate(props);

        assertFalse(violations.isEmpty(), "Empty list should be rejected");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("must not be empty") || 
                              v.getMessage().contains("ne doit pas être vide")));
    }

    @Test
    void shouldRejectNullElementInSslProtocols() {
        SslProperties props = new SslProperties();
        props.setSslProtocols(Arrays.asList("TLSv1.3", null, "TLSv1.2"));

        Set<ConstraintViolation<SslProperties>> violations = validator.validate(props);

        // Afficher les violations pour debugging
        System.out.println("\n=== TEST: shouldRejectNullElementInSslProtocols ===");
        System.out.println("Nombre de violations détectées: " + violations.size());
        violations.forEach(v -> 
            System.out.println("  - Violation: " + v.getPropertyPath() + " = " + v.getMessage())
        );
        
        if (violations.isEmpty()) {
            fail("ATTENTION: Aucune violation détectée!\n" +
                 "Cela peut arriver avec Hibernate Validator 6.1+ qui valide automatiquement.\n" +
                 "Cependant, l'erreur HV000187 dans Spring Boot prouve que @Valid EST REQUIS.\n" +
                 "Vérifiez la version d'Hibernate Validator et la configuration Spring Boot.");
        }
        
        // Vérifier que la violation concerne bien un élément de la liste
        boolean hasListElementViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().matches("sslProtocols\\[\\d+\\]"));
        
        assertTrue(hasListElementViolation, 
            "Should have violation on list element (e.g., 'sslProtocols[1]')");
    }
    
    @Test
    void verifyValidatorConfiguration() {
        // Test pour vérifier la configuration du validator
        System.out.println("\n=== Configuration du Validator ===");
        System.out.println("ValidatorFactory: " + validatorFactory.getClass().getName());
        System.out.println("Validator: " + validator.getClass().getName());
        
        // Teste si le validator détecte les annotations de type
        SslProperties props = new SslProperties();
        props.setSslProtocols(Arrays.asList("TLSv1.3", null));
        
        Set<ConstraintViolation<SslProperties>> violations = validator.validate(props);
        
        System.out.println("Validation automatique des éléments: " + 
            (violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("[")) 
                ? "ACTIVÉE (Hibernate Validator 6.1+)" 
                : "DÉSACTIVÉE (nécessite @Valid explicite)"));
    }

    @Test
    void shouldAcceptDefaultSslProtocols() {
        SslProperties props = new SslProperties();
        // Utilise la valeur par défaut

        Set<ConstraintViolation<SslProperties>> violations = validator.validate(props);

        assertTrue(violations.isEmpty(), "Default protocols should be valid");
    }

    @Test
    void shouldRejectMultipleNullElementsInSslProtocols() {
        SslProperties props = new SslProperties();
        props.setSslProtocols(Arrays.asList(null, "TLSv1.3", null));

        Set<ConstraintViolation<SslProperties>> violations = validator.validate(props);

        // Devrait détecter 2 violations (index 0 et 2)
        assertFalse(violations.isEmpty(), 
            "Multiple null elements should be rejected");
        
        long nullElementViolations = violations.stream()
                .filter(v -> v.getPropertyPath().toString().matches("sslProtocols\\[\\d+\\]"))
                .count();
        
        assertEquals(2, nullElementViolations, 
            "Should detect exactly 2 null elements in the list");
    }
}
