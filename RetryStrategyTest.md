import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NoHttpResponseException;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.time.Duration;

import static com.example.util.RestClientHelper.CustomApacheRetryStrategy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test unitaire pour valider la logique de la stratégie de réessai CustomApacheRetryStrategy.
 */
class CustomApacheRetryStrategyTest {

    private CustomApacheRetryStrategy retryStrategy;
    private final int MAX_ATTEMPTS = 3;
    private final long DELAY_MS = 500;
    private final HttpClientContext context = mock(HttpClientContext.class);

    @BeforeEach
    void setUp() {
        // Initialisation avec 3 tentatives max et 500ms de délai
        retryStrategy = new CustomApacheRetryStrategy(MAX_ATTEMPTS, DELAY_MS);
    }

    // --- Tests retryRequest(HttpRequest, IOException, ...) ---

    @Test
    void shouldRetryOnIOExceptionForIdempotentRequest() {
        // GET est idempotent
        HttpRequest request = new BasicClassicHttpRequest("GET", "/test");
        IOException exception = new IOException("Réseau perdu");

        // Tentative 1 (executionCount = 1)
        assertTrue(retryStrategy.retryRequest(request, exception, 1, context), 
                   "Doit réessayer pour GET/IOException à la première tentative.");
    }

    @Test
    void shouldNotRetryIfMaxAttemptsReached() {
        HttpRequest request = new BasicClassicHttpRequest("GET", "/test");
        IOException exception = new IOException();

        // Tentative 3 (executionCount = 3), maxRetries est 3 (1 initiale + 2 réessais)
        // La condition est >= maxRetries. Si executionCount est 3, on arrête.
        assertFalse(retryStrategy.retryRequest(request, exception, 3, context), 
                    "Ne doit pas réessayer si le nombre max de tentatives est atteint.");
    }

    @Test
    void shouldNotRetryOnInterruptedIOException() {
        HttpRequest request = new BasicClassicHttpRequest("GET", "/test");
        // InterruptedIOException inclut les SocketTimeoutException côté client
        IOException exception = new InterruptedIOException("Timeout ou interruption");

        assertFalse(retryStrategy.retryRequest(request, exception, 1, context), 
                    "Ne doit pas réessayer sur InterruptedIOException.");
    }
    
    @Test
    void shouldNotRetryOnPOSTForGenericIOException() {
        // POST n'est pas idempotent
        HttpRequest request = new BasicClassicHttpRequest("POST", "/test");
        IOException exception = new IOException("Réseau perdu");

        assertFalse(retryStrategy.retryRequest(request, exception, 1, context), 
                    "Ne doit pas réessayer pour POST/IOException (sauf NoHttpResponseException).");
    }

    @Test
    void shouldRetryOnPOSTForNoHttpResponseException() {
        // POST n'est pas idempotent, mais NoHttpResponseException est une erreur de socket
        HttpRequest request = new BasicClassicHttpRequest("POST", "/test");
        IOException exception = new NoHttpResponseException("Socket fermé");

        assertTrue(retryStrategy.retryRequest(request, exception, 1, context), 
                   "Doit réessayer pour POST/NoHttpResponseException.");
    }
    
    // --- Tests retryRequest(HttpResponse, ...) ---

    @Test
    void shouldRetryOnInternalServerError500() {
        HttpResponse response = new BasicHttpResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR); // 500
        assertTrue(retryStrategy.retryRequest(response, 1, context), 
                   "Doit réessayer sur un statut 500.");
    }

    @Test
    void shouldRetryOnBadGateway502() {
        HttpResponse response = new BasicHttpResponse(HttpStatus.SC_BAD_GATEWAY); // 502
        assertTrue(retryStrategy.retryRequest(response, 1, context), 
                   "Doit réessayer sur un statut 502.");
    }

    @Test
    void shouldNotRetryOnNotFound404() {
        HttpResponse response = new BasicHttpResponse(HttpStatus.SC_NOT_FOUND); // 404
        assertFalse(retryStrategy.retryRequest(response, 1, context), 
                    "Ne doit pas réessayer sur un statut 404.");
    }

    @Test
    void shouldNotRetryOnNotImplemented501() {
        // 501 est explicitement exclu
        HttpResponse response = new BasicHttpResponse(HttpStatus.SC_NOT_IMPLEMENTED); // 501
        assertFalse(retryStrategy.retryRequest(response, 1, context), 
                    "Ne doit pas réessayer sur un statut 501.");
    }

    @Test
    void shouldNotRetryResponseIfMaxAttemptsReached() {
        HttpResponse response = new BasicHttpResponse(HttpStatus.SC_SERVICE_UNAVAILABLE); // 503
        
        // Tentative 3 (executionCount = 3), maxRetries est 3
        assertFalse(retryStrategy.retryRequest(response, 3, context), 
                    "Ne doit pas réessayer sur 5xx si le nombre max de tentatives est atteint.");
    }
    
    // --- Tests getDelayDuration(...) ---

    @Test
    void shouldReturnFixedDelayDuration() {
        HttpResponse response = new BasicHttpResponse(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        Duration expectedDuration = Duration.ofMillis(DELAY_MS);
        
        Duration actualDuration = retryStrategy.getDelayDuration(response, 1, context);
        
        assertEquals(expectedDuration, actualDuration, 
                     "Le délai doit correspondre à la valeur de configuration (500ms).");
    }
}
