import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RestClientLoggingTest {

    private RestClient restClient;
    private ClientHttpRequestFactory mockFactory;
    private ClientHttpRequest mockRequest;
    private ClientHttpResponse mockResponse;

    @BeforeEach
    void setUp() throws IOException {
        // 1. On mock la couche basse
        mockFactory = mock(ClientHttpRequestFactory.class);
        mockRequest = mock(ClientHttpRequest.class);
        mockResponse = mock(ClientHttpResponse.class);

        // Configuration des mocks pour simuler un échange HTTP
        when(mockFactory.createRequest(any(), any())).thenReturn(mockRequest);
        when(mockRequest.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
        when(mockRequest.execute()).thenReturn(mockResponse);
        
        // Simulation d'une réponse JSON
        String jsonResponse = "{\"status\":\"success\"}";
        when(mockResponse.getStatusCode()).thenReturn(HttpStatus.OK);
        when(mockResponse.getHeaders()).thenReturn(new org.springframework.http.HttpHeaders());
        when(mockResponse.getBody()).thenReturn(new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8)));

        // 2. Création du RestClient avec l'intercepteur et la factory de buffering
        // C'est ici qu'on teste la configuration réelle du Helper
        restClient = RestClient.builder()
                .requestFactory(new BufferingClientHttpRequestFactory(mockFactory))
                .requestInterceptor(new RestClientLoggingInterceptor())
                .build();
    }

    @Test
    void shouldLogAndReturnResponseWhenCallingRestClient() {
        // GIVEN
        String bodyToSend = "{\"message\":\"hello\"}";

        // WHEN
        // On effectue l'appel via le RestClient
        String result = restClient.post()
                .uri("http://api.test.com")
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyToSend)
                .retrieve()
                .body(String.class);

        // THEN
        // On vérifie que le résultat est correct
        assertThat(result).contains("success");

        // On vérifie que la factory a bien été sollicitée
        try {
            verify(mockFactory, times(1)).createRequest(any(), any());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RestClientLoggingInterceptorTest {

    private RestClient restClient;
    private ClientHttpRequestFactory mockFactory;

    @BeforeEach
    void setUp() {
        // 1. On mock la factory pour qu'elle retourne nos objets de test
        mockFactory = mock(ClientHttpRequestFactory.class);

        // 2. Initialisation du RestClient avec la configuration réelle du Helper
        restClient = RestClient.builder()
                .requestFactory(new BufferingClientHttpRequestFactory(mockFactory))
                .requestInterceptor(new RestClientLoggingInterceptor())
                .build();
    }

    @Test
    void shouldLogAndReadBodySuccessfully() throws IOException {
        // GIVEN
        String url = "http://api.test.com";
        String bodyToSend = "{\"message\":\"hello\"}";
        String jsonResponse = "{\"status\":\"success\"}";

        // Création d'une requête de test (gère l'OutputStream interne)
        MockClientHttpRequest mockRequest = new MockClientHttpRequest();
        
        // Création d'une réponse de test
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(
                jsonResponse.getBytes(StandardCharsets.UTF_8), 
                HttpStatus.OK
        );
        mockResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // Liaison des mocks à la factory
        when(mockFactory.createRequest(any(), any())).thenReturn(mockRequest);
        // On simule l'exécution : quand le client appelle execute(), on renvoie la réponse
        mockRequest.setResponse(mockResponse);

        // WHEN
        // On effectue l'appel. Le RestClient va :
        // 1. Créer la requête via la factory
        // 2. Passer dans l'intercepteur (Log TRACE)
        // 3. Écrire le bodyToSend dans le mockRequest
        String result = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(bodyToSend)
                .retrieve()
                .body(String.class);

        // THEN
        // Vérification que le corps de la réponse a été correctement lu après le log
        assertThat(result).isEqualTo(jsonResponse);
        
        // Vérification que le corps de la requête a bien été écrit dans la requête
        assertThat(mockRequest.getBodyAsString()).isEqualTo(bodyToSend);
        
        // Vérification que la factory a été appelée
        verify(mockFactory, times(1)).createRequest(any(), any());
    }
}
