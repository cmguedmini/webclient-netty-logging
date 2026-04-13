import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RestTemplateLoggingInterceptorTest {

    private RestTemplateLoggingInterceptor interceptor;
    private HttpRequest request;
    private ClientHttpRequestExecution execution;
    private ClientHttpResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new RestTemplateLoggingInterceptor();
        request = mock(HttpRequest.class);
        execution = mock(ClientHttpRequestExecution.class);
        response = mock(ClientHttpResponse.class);
    }

    @Test
    void shouldInterceptAndLog() throws IOException {
        // GIVEN
        byte[] requestBody = "{\"name\":\"Gemini\"}".getBytes(StandardCharsets.UTF_8);
        String responseBody = "{\"status\":\"ok\"}";
        
        // Mock de la requête
        when(request.getURI()).thenReturn(URI.create("http://localhost/test"));
        when(request.getMethodValue()).thenReturn("POST");
        
        // Mock de la réponse
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        when(response.getBody()).thenReturn(new ByteArrayInputStream(responseBody.getBytes(StandardCharsets.UTF_8)));
        
        // Mock de l'exécution : quand on appelle execution.execute, on renvoie notre réponse
        when(execution.execute(any(), any())).thenReturn(response);

        // WHEN
        ClientHttpResponse result = interceptor.intercept(request, requestBody, execution);

        // THEN
        // 1. On vérifie que le résultat est bien la réponse attendue
        assertThat(result).isEqualTo(response);
        
        // 2. On vérifie que la chaîne d'exécution a bien été appelée une fois
        verify(execution, times(1)).execute(request, requestBody);
    }
}
