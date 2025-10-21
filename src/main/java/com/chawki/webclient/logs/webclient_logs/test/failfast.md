package com.example.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.validation.ValidationErrors;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.boot.context.properties.ConfigurationPropertiesBindTester.bind;

class MyAppPropertiesTest {

    // Le binder est utilisé pour tester l'injection et la validation des propriétés
    private final org.springframework.boot.context.properties.ConfigurationPropertiesBindTester<MyAppProperties> binder = 
        bind(MyAppProperties.class);

    /**
     * Teste le scénario où l'ancienne propriété EST DÉFINIE.
     * Le démarrage DOIT échouer à cause de l'échec de la validation @Null.
     */
    @Test
    void shouldFailWhenLegacyPropertyIsPresent() {
        
        // GIVEN: La propriété 'monapp.ancienne.cle' est définie dans la configuration
        ConfigurationPropertySource source = new MapConfigurationPropertySource(
            "monapp.ancienne.cle", "ceci-est-une-valeur"
        );
        
        // WHEN / THEN: Tenter de lier les propriétés doit lancer une BindException
        assertThatExceptionOfType(BindException.class)
            .isThrownBy(() -> this.binder.bind(source))
            .withMessageContaining("ERRORE_CLE_SUPPRIMEE") // Vérifie la présence du payload dans l'erreur
            .withMessageContaining("Propriété Obsolete Détectée");
    }

    /**
     * Teste le scénario où l'ancienne propriété EST ABSENTE.
     * Le démarrage DOIT réussir.
     */
    @Test
    void shouldSucceedWhenLegacyPropertyIsMissing() {
        
        // GIVEN: Aucune propriété spécifique n'est définie (ou seulement la nouvelle)
        ConfigurationPropertySource source = new MapConfigurationPropertySource(
            "monapp.nouvelle.cle", "valeurOK"
        );
        
        // WHEN / THEN: Tenter de lier les propriétés ne doit lancer AUCUNE exception
        assertThatNoException()
            .isThrownBy(() -> this.binder.bind(source));
    }
}


Explication du Test

    ConfigurationPropertiesBindTester : Il s'agit d'une utilité de test de Spring Boot qui simule le processus d'injection et de validation sans démarrer l'intégralité du contexte Spring.

    shouldFailWhenLegacyPropertyIsPresent :

        Nous créons une source de propriété qui contient l'ancienne clé avec une valeur.

        Nous utilisons assertThatExceptionOfType(BindException.class).isThrownBy(...) pour affirmer que la tentative de liaison des propriétés échoue.

        Nous vérifions ensuite le message de l'exception pour nous assurer qu'il contient votre message personnalisé et votre payload (ERRORE_CLE_SUPPRIMEE).

    shouldSucceedWhenLegacyPropertyIsMissing :

        Nous créons une source de propriété qui ne contient pas l'ancienne clé.

        Nous utilisons assertThatNoException().isThrownBy(...) pour affirmer que la tentative de liaison des propriétés réussit.

Ce test valide que votre stratégie de validation est correctement appliquée : la présence de la propriété obsolète bloque l'application avec un message d'erreur spécifique.

Voulez-vous que je vous montre comment intégrer ce mécanisme dans un test d'intégration complet qui démarre un contexte Spring Boot réel (plutôt qu'un simple test unitaire) ?




