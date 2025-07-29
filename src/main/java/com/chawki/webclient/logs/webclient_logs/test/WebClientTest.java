package com.chawki.webclient.logs.webclient_logs.test;

import com.example.webclientloggingtest.service.WebClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebClientLoggingTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebClientService webClientService;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        System.out.println("=".repeat(80));
        System.out.println("Serveur de test démarré sur: " + baseUrl);
        System.out.println("=".repeat(80));
    }

    @Test
    void testSuccessEndpoint_ShouldLogRequestAndResponse() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST SUCCESS - GET");
        System.out.println("=".repeat(50));

        Map response = webClientService.callSuccessEndpoint(baseUrl);

        System.out.println("Réponse reçue: " + response);
        assertNotNull(response);
        assertEquals("success", response.get("status"));
        assertTrue(response.containsKey("receivedHeaders"));
        assertTrue(response.containsKey("timestamp"));

        System.out.println("✅ Test SUCCESS GET terminé");
    }

    @Test
    void testSuccessEndpointPost_ShouldLogRequestBodyAndResponse() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST SUCCESS - POST");
        System.out.println("=".repeat(50));

        Map<String, Object> requestBody = Map.of(
                "testData", "valeur de test",
                "number", 42,
                "nested", Map.of("key", "value")
        );

        Map response = webClientService.callSuccessEndpointPost(baseUrl, requestBody);

        System.out.println("Réponse reçue: " + response);
        assertNotNull(response);
        assertEquals("success", response.get("status"));
        assertTrue(response.containsKey("receivedHeaders"));
        assertTrue(response.containsKey("receivedBody"));
        assertTrue(response.containsKey("timestamp"));

        System.out.println("✅ Test SUCCESS POST terminé");
    }

    @Test
    void testErrorEndpoint_ShouldLogRequestAndErrorResponse() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST RUNTIME EXCEPTION - GET");
        System.out.println("=".repeat(50));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            webClientService.callErrorEndpoint(baseUrl);
        });

        System.out.println("Exception capturée: " + exception.getClass().getSimpleName());
        System.out.println("Message: " + exception.getMessage());
        
        // Vérifier que c'est bien une RuntimeException avec le bon message
        assertTrue(exception.getMessage().contains("Erreur WebClient"));

        System.out.println("✅ Test RUNTIME EXCEPTION GET terminé");
    }

    @Test
    void testErrorEndpointPost_ShouldLogRequestBodyAndErrorResponse() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST RUNTIME EXCEPTION - POST");
        System.out.println("=".repeat(50));

        Map<String, Object> requestBody = Map.of(
                "errorTest", true,
                "data", "données qui vont provoquer une erreur"
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            webClientService.callErrorEndpointPost(baseUrl, requestBody);
        });

        System.out.println("Exception capturée: " + exception.getClass().getSimpleName());
        System.out.println("Message: " + exception.getMessage());
        
        assertTrue(exception.getMessage().contains("Erreur WebClient POST"));

        System.out.println("✅ Test RUNTIME EXCEPTION POST terminé");
    }

    @Test
    void testTimeoutEndpoint_ShouldLogRequestAndTimeoutError() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST TIMEOUT - GET");
        System.out.println("=".repeat(50));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            webClientService.callTimeoutEndpoint(baseUrl);
        });

        System.out.println("Exception de timeout capturée: " + exception.getClass().getSimpleName());
        System.out.println("Message: " + exception.getMessage());
        
        // Vérifier que c'est bien une exception liée au timeout
        assertTrue(exception.getMessage().toLowerCase().contains("timeout"));

        System.out.println("✅ Test TIMEOUT GET terminé");
    }

    @Test
    void testTimeoutEndpointPost_ShouldLogRequestBodyAndTimeoutError() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("TEST TIMEOUT - POST");
        System.out.println("=".repeat(50));

        Map<String, Object> requestBody = Map.of(
                "timeoutTest", true,
                "data", "données pour test timeout",
                "expectedBehavior", "timeout après 2 secondes"
        );

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            webClientService.callTimeoutEndpointPost(baseUrl, requestBody);
        });

        System.out.println("Exception de timeout capturée: " + exception.getClass().getSimpleName());
        System.out.println("Message: " + exception.getMessage());
        
        assertTrue(exception.getMessage().toLowerCase().contains("timeout"));

        System.out.println("✅ Test TIMEOUT POST terminé");
    }

    @Test
    void testAllScenariosSequentially() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEST SÉQUENTIEL DE TOUS LES SCÉNARIOS");
        System.out.println("=".repeat(60));

        // Test 1: Success GET
        testSuccessEndpoint_ShouldLogRequestAndResponse();
        
        // Pause entre les tests
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        // Test 2: Success POST
        testSuccessEndpointPost_ShouldLogRequestBodyAndResponse();
        
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        // Test 3: Error GET
        testErrorEndpoint_ShouldLogRequestAndErrorResponse();
        
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        // Test 4: Error POST
        testErrorEndpointPost_ShouldLogRequestBodyAndErrorResponse();
        
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        // Test 5: Timeout GET
        testTimeoutEndpoint_ShouldLogRequestAndTimeoutError();
        
        try { Thread.sleep(1000); } catch (InterruptedException e) {}
        
        // Test 6: Timeout POST
        testTimeoutEndpointPost_ShouldLogRequestBodyAndTimeoutError();

        System.out.println("\n🎉 TOUS LES TESTS ONT ÉTÉ EXÉCUTÉS AVEC SUCCÈS!");
        System.out.println("Vérifiez les logs pour voir les détails des requêtes/réponses capturées par wiretap.");
    }
}