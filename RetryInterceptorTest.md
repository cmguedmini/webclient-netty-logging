import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitaire pour RetryInterceptor
 * Option 1: Utiliser MockitoAnnotations.openMocks() - PAS d'extension
 */
class RetryInterceptorTest {

    private HttpRequest request;
    private ClientHttpRequestExecution execution;
    private ClientHttpResponse response;
    private RetryInterceptor interceptor;
    private byte[] body;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        // Initialiser les mocks manuellement
        mocks = org.mockito.MockitoAnnotations.openMocks(this);
        
        request = mock(HttpRequest.class);
        execution = mock(ClientHttpRequestExecution.class);
        response = mock(ClientHttpResponse.class);
        
        interceptor = new RetryInterceptor(3, 100, 1000);
        body = new byte[0];
        
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://api.example.com/test"));
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void shouldSucceedOnFirstAttempt() throws IOException {
        // Given
        when(execution.execute(request, body)).thenReturn(response);
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        // When
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isEqualTo(response);
        verify(execution, times(1)).execute(request, body);
        verify(response, never()).close();
    }

    @Test
    void shouldRetryOn500AndSucceed() throws IOException {
        // Given
        ClientHttpResponse errorResponse = mock(ClientHttpResponse.class);
        when(errorResponse.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        when(execution.execute(request, body))
            .thenReturn(errorResponse)
            .thenReturn(response);
        
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        // When
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isEqualTo(response);
        verify(execution, times(2)).execute(request, body);
        verify(errorResponse).close();
    }

    @Test
    void shouldRetryOn503ServiceUnavailable() throws IOException {
        // Given
        ClientHttpResponse errorResponse = mock(ClientHttpResponse.class);
        when(errorResponse.getStatusCode()).thenReturn(HttpStatus.SERVICE_UNAVAILABLE);

        when(execution.execute(request, body))
            .thenReturn(errorResponse)
            .thenReturn(response);
        
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        // When
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isEqualTo(response);
        verify(execution, times(2)).execute(request, body);
    }

    @Test
    void shouldRetryOn429RateLimit() throws IOException {
        // Given
        ClientHttpResponse rateLimitResponse = mock(ClientHttpResponse.class);
        when(rateLimitResponse.getStatusCode()).thenReturn(HttpStatus.TOO_MANY_REQUESTS);

        when(execution.execute(request, body))
            .thenReturn(rateLimitResponse)
            .thenReturn(response);
        
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        // When
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isEqualTo(response);
        verify(execution, times(2)).execute(request, body);
    }

    @Test
    void shouldFailAfterMaxAttempts() throws IOException {
        // Given
        when(execution.execute(request, body)).thenThrow(new IOException("Connection refused"));

        // When & Then
        assertThatThrownBy(() -> interceptor.intercept(request, body, execution))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Connection refused");

        verify(execution, times(3)).execute(request, body);
    }

    @Test
    void shouldFailAfterMaxAttemptsWithServerError() throws IOException {
        // Given
        when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(execution.execute(request, body)).thenReturn(response);

        // When
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isEqualTo(response);
        verify(execution, times(3)).execute(request, body);
        verify(response, times(2)).close();
    }

    @Test
    void shouldNotRetryOn400BadRequest() throws IOException {
        // Given
        when(response.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(execution.execute(request, body)).thenReturn(response);

        // When
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isEqualTo(response);
        verify(execution, times(1)).execute(request, body);
        verify(response, never()).close();
    }

    @Test
    void shouldNotRetryOn404NotFound() throws IOException {
        // Given
        when(response.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        when(execution.execute(request, body)).thenReturn(response);

        // When
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isEqualTo(response);
        verify(execution, times(1)).execute(request, body);
    }

    @Test
    void shouldRetryOnIOException() throws IOException {
        // Given
        when(execution.execute(request, body))
            .thenThrow(new IOException("Network error"))
            .thenReturn(response);
        
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);

        // When
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isEqualTo(response);
        verify(execution, times(2)).execute(request, body);
    }

    @Test
    void shouldRespectMaxAttemptsConfiguration() throws IOException {
        // Given
        RetryInterceptor customInterceptor = new RetryInterceptor(5, 50, 500);
        when(execution.execute(request, body)).thenThrow(new IOException("Error"));

        // When & Then
        assertThatThrownBy(() -> customInterceptor.intercept(request, body, execution))
            .isInstanceOf(IOException.class);

        verify(execution, times(5)).execute(request, body);
    }

    @Test
    void shouldHandleInterruptedException() throws IOException {
        // Given
        when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        when(execution.execute(request, body)).thenReturn(response);
        
        // Interrompre le thread avant l'exécution
        Thread.currentThread().interrupt();

        // When & Then
        assertThatThrownBy(() -> interceptor.intercept(request, body, execution))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Retry interrompu");

        // Vérifier que le flag d'interruption est maintenu
        assertThat(Thread.interrupted()).isTrue();
    }
}

// ============================================
// OPTION 2: Sans Mockito - Avec des implémentations de test
// ============================================

/**
 * Alternative SANS Mockito - Utilise des implémentations de test
 */
class RetryInterceptorWithoutMockitoTest {

    private RetryInterceptor interceptor;
    private TestHttpRequest request;
    private byte[] body;

    @BeforeEach
    void setUp() {
        interceptor = new RetryInterceptor(3, 50, 500);
        request = new TestHttpRequest("http://api.example.com/test");
        body = new byte[0];
    }

    @Test
    void shouldSucceedOnFirstAttempt() throws IOException {
        // Given
        TestExecution execution = new TestExecution(HttpStatus.OK);

        // When
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isNotNull();
        assertThat(execution.getCallCount()).isEqualTo(1);
    }

    @Test
    void shouldRetryOn500() throws IOException {
        // Given
        TestExecution execution = new TestExecution(
            HttpStatus.INTERNAL_SERVER_ERROR,
            HttpStatus.OK
        );

        // When
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isNotNull();
        assertThat(execution.getCallCount()).isEqualTo(2);
    }

    @Test
    void shouldFailAfterMaxAttempts() {
        // Given
        TestExecution execution = new TestExecution(new IOException("Connection error"));

        // When & Then
        assertThatThrownBy(() -> interceptor.intercept(request, body, execution))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Connection error");

        assertThat(execution.getCallCount()).isEqualTo(3);
    }

    @Test
    void shouldNotRetryOn404() throws IOException {
        // Given
        TestExecution execution = new TestExecution(HttpStatus.NOT_FOUND);

        // When
        ClientHttpResponse result = interceptor.intercept(request, body, execution);

        // Then
        assertThat(result).isNotNull();
        assertThat(execution.getCallCount()).isEqualTo(1);
    }

    // Classes de test internes
    static class TestHttpRequest implements HttpRequest {
        private final URI uri;

        public TestHttpRequest(String url) {
            this.uri = URI.create(url);
        }

        @Override
        public HttpMethod getMethod() {
            return HttpMethod.GET;
        }

        @Override
        public URI getURI() {
            return uri;
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return new org.springframework.http.HttpHeaders();
        }
    }

    static class TestExecution implements ClientHttpRequestExecution {
        private final HttpStatus[] statusSequence;
        private final IOException exception;
        private int callCount = 0;

        public TestExecution(HttpStatus... statusSequence) {
            this.statusSequence = statusSequence;
            this.exception = null;
        }

        public TestExecution(IOException exception) {
            this.statusSequence = null;
            this.exception = exception;
        }

        @Override
        public ClientHttpResponse execute(HttpRequest request, byte[] body) throws IOException {
            callCount++;
            
            if (exception != null) {
                throw exception;
            }
            
            HttpStatus status = statusSequence[Math.min(callCount - 1, statusSequence.length - 1)];
            return new TestClientHttpResponse(status);
        }

        public int getCallCount() {
            return callCount;
        }
    }

    static class TestClientHttpResponse implements ClientHttpResponse {
        private final HttpStatus status;

        public TestClientHttpResponse(HttpStatus status) {
            this.status = status;
        }

        @Override
        public HttpStatus getStatusCode() {
            return status;
        }

        @Override
        public String getStatusText() {
            return status.getReasonPhrase();
        }

        @Override
        public void close() {
            // No-op
        }

        @Override
        public java.io.InputStream getBody() {
            return new java.io.ByteArrayInputStream(new byte[0]);
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return new org.springframework.http.HttpHeaders();
        }
    }
}

/* 
Dépendances Maven minimales:
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
*/
