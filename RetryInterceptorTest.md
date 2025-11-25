import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class RetryInterceptorTest {

    @Mock
    private HttpRequest request;

    @Mock
    private ClientHttpRequestExecution execution;

    @Mock
    private ClientHttpResponse response;

    private RetryInterceptor interceptor;
    private byte[] body;

    @BeforeEach
    void setUp() {
        interceptor = new RetryInterceptor(3, 100, 1000);
        body = new byte[0];
        
        when(request.getMethod()).thenReturn(HttpMethod.GET);
        when(request.getURI()).thenReturn(URI.create("http://api.example.com/test"));
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
        verify(response, times(2)).close(); // Fermé 2 fois (tentatives 1 et 2)
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

// Dépendances Maven nécessaires (pom.xml)
/*
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
*/
