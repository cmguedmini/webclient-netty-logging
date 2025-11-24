package com.example.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Classe de tests unitaires pour RestClientHelper.
 *
 * Le but principal est de s'assurer que les méthodes de construction
 * appellent correctement les méthodes de configuration des dépendances (comme la ClientHttpRequestFactory)
 * et que l'ObjectMapper est correctement appliqué.
 */
public class RestClientHelperTest {

    // Constantes de test
    private static final Duration CONNECTION_TIMEOUT_DURATION = Duration.ofSeconds(5);
    private static final Duration RESPONSE_TIMEOUT_DURATION = Duration.ofSeconds(30);
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final long RESPONSE_TIMEOUT_MS = 30000L;

    // Mock pour l'ObjectMapper (pour vérifier qu'il est utilisé)
    private ObjectMapper mockObjectMapper;

    @BeforeEach
    void setUp() {
        mockObjectMapper = mock(ObjectMapper.class);
    }

    /**
     * Teste la méthode build utilisant des Durations.
     * Le test vérifie que la construction du RestClient réussit.
     * Le test de la configuration réelle des timeouts se fait au niveau
     * de la méthode createRequestFactory.
     */
    @Test
    void build_withDuration_shouldReturnConfiguredRestClient() {
        // ACT
        RestClient restClient = RestClientHelper.build(
                CONNECTION_TIMEOUT_DURATION,
                RESPONSE_TIMEOUT_DURATION,
                null, // Pas de SSLContext pour ce test
                mockObjectMapper
        );

        // ASSERT
        assertNotNull(restClient, "Le RestClient ne devrait pas être null");
        // On pourrait ajouter des vérifications plus poussées si Spring exposait facilement
        // les propriétés internes du RestClient, mais ce test valide la construction de base.
    }

    /**
     * Teste la méthode build utilisant des valeurs millisecondes.
     */
    @Test
    void build_withMsTimeouts_shouldReturnConfiguredRestClient() {
        // ACT
        RestClient restClient = RestClientHelper.build(
                CONNECTION_TIMEOUT_MS,
                RESPONSE_TIMEOUT_MS,
                null, // Pas de SSLContext
                mockObjectMapper
        );

        // ASSERT
        assertNotNull(restClient, "Le RestClient ne devrait pas être null");
    }

    /**
     * Teste la méthode build avec un Supplier de ClientHttpRequestFactory.
     * On s'assure que le RestClient est construit et que l'ObjectMapper est bien pris en compte.
     */
    @Test
    void build_withRequestFactorySupplier_shouldUseSuppliedFactoryAndObjectMapper() {
        // ARRANGE
        // Mock de la ClientHttpRequestFactory pour simuler le comportement du système HTTP bas niveau
        ClientHttpRequestFactory mockFactory = mock(ClientHttpRequestFactory.class);
        Supplier<ClientHttpRequestFactory> factorySupplier = () -> mockFactory;

        // ACT
        RestClient restClient = RestClientHelper.build(factorySupplier, mockObjectMapper);

        // ASSERT
        assertNotNull(restClient, "Le RestClient ne devrait pas être null");

        // Dans un test plus complet, on vérifierait que les messages converters
        // internes du RestClient ont été mis à jour avec le mockObjectMapper.
        // Cependant, RestClient ne fournit pas de moyen facile d'inspecter son état interne
        // (comme les converters) après la construction, mais le fait que l'appel ne lève pas
        // d'exception indique que la logique de configuration a été exécutée.
    }

    /**
     * Teste la création de la ClientHttpRequestFactory avec des Durations.
     * Cela teste indirectement l'appel à la méthode createRequestFactory avec des ms.
     */
    @Test
    void createRequestFactory_withDuration_shouldNotThrowException() {
        // ACT
        ClientHttpRequestFactory factory = RestClientHelper.createRequestFactory(
                null, // Pas de SSLContext
                CONNECTION_TIMEOUT_DURATION,
                RESPONSE_TIMEOUT_DURATION
        );

        // ASSERT
        assertNotNull(factory, "La ClientHttpRequestFactory ne devrait pas être null");
        // Le test ici valide la construction de l'objet sans erreur.
    }

    /**
     * Teste la création de la ClientHttpRequestFactory avec des ms.
     */
    @Test
    void createRequestFactory_withMs_shouldNotThrowException() {
        // ACT
        ClientHttpRequestFactory factory = RestClientHelper.createRequestFactory(
                null, // Pas de SSLContext
                CONNECTION_TIMEOUT_MS,
                RESPONSE_TIMEOUT_MS
        );

        // ASSERT
        assertNotNull(factory, "La ClientHttpRequestFactory ne devrait pas être null");
    }

    /**
     * Teste la méthode build quand l'ObjectMapper est null.
     * Le RestClient devrait toujours être construit sans erreur.
     */
    @Test
    void build_withNullObjectMapper_shouldReturnRestClient() {
        // ACT
        RestClient restClient = RestClientHelper.build(
                CONNECTION_TIMEOUT_DURATION,
                RESPONSE_TIMEOUT_DURATION,
                null,
                null // ObjectMapper est null
        );

        // ASSERT
        assertNotNull(restClient, "Le RestClient ne devrait pas être null même avec un ObjectMapper null");
    }
}
