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

        // SANS @Valid sur la List : ce test ÉCHOUERA (violations sera vide)
        // AVEC @Valid sur la List : ce test PASSERA (détecte le null à l'index 1)
        assertFalse(violations.isEmpty(), 
            "List with null element should be rejected - @Valid annotation is required on the List field!");
        
        // Vérifier que la violation concerne bien un élément de la liste
        boolean hasListElementViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().matches("sslProtocols\\[\\d+\\]"));
        
        assertTrue(hasListElementViolation, 
            "Should have violation on list element (e.g., 'sslProtocols[1]')");
        
        // Afficher les violations pour debugging
        violations.forEach(v -> 
            System.out.println("Violation: " + v.getPropertyPath() + " = " + v.getMessage())
        );
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
