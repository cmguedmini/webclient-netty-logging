package com.chawki.webclient.logs.webclient_logs.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Service
public class WebClientService {

    @Autowired
    private WebClient webClient;

    public Map callSuccessEndpoint(String baseUrl) {
        try {
            return webClient.get()
                    .uri(baseUrl + "/api/test/success")
                    .header("X-Test-Header", "success-test")
                    .header("X-Request-ID", "req-" + System.currentTimeMillis())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block(); // Mode synchrone
        } catch (Exception ex) {
            throw new RuntimeException("Erreur lors de l'appel success endpoint: " + ex.getMessage(), ex);
        }
    }

    public Map callSuccessEndpointPost(String baseUrl, Map<String, Object> requestBody) {
        try {
            return webClient.post()
                    .uri(baseUrl + "/api/test/success")
                    .header("X-Test-Header", "success-post-test")
                    .header("X-Request-ID", "req-post-" + System.currentTimeMillis())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block(); // Mode synchrone
        } catch (Exception ex) {
            throw new RuntimeException("Erreur lors de l'appel success POST endpoint: " + ex.getMessage(), ex);
        }
    }

    public Map callErrorEndpoint(String baseUrl) {
        try {
            return webClient.get()
                    .uri(baseUrl + "/api/test/error")
                    .header("X-Test-Header", "error-test")
                    .header("X-Request-ID", "req-error-" + System.currentTimeMillis())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block(); // Mode synchrone
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Erreur WebClient: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erreur lors de l'appel error endpoint: " + ex.getMessage(), ex);
        }
    }

    public Map callErrorEndpointPost(String baseUrl, Map<String, Object> requestBody) {
        try {
            return webClient.post()
                    .uri(baseUrl + "/api/test/error")
                    .header("X-Test-Header", "error-post-test")
                    .header("X-Request-ID", "req-error-post-" + System.currentTimeMillis())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block(); // Mode synchrone
        } catch (WebClientResponseException ex) {
            throw new RuntimeException("Erreur WebClient POST: " + ex.getStatusCode() + " - " + ex.getResponseBodyAsString(), ex);
        } catch (Exception ex) {
            throw new RuntimeException("Erreur lors de l'appel error POST endpoint: " + ex.getMessage(), ex);
        }
    }

    public Map callTimeoutEndpoint(String baseUrl) {
        try {
            return webClient.get()
                    .uri(baseUrl + "/api/test/timeout")
                    .header("X-Test-Header", "timeout-test")
                    .header("X-Request-ID", "req-timeout-" + System.currentTimeMillis())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(2)) // Timeout plus court que le délai du serveur
                    .block(); // Mode synchrone
        } catch (Exception ex) {
            if (ex.getCause() instanceof TimeoutException || ex.getMessage().toLowerCase().contains("timeout")) {
                throw new RuntimeException("Timeout lors de l'appel timeout endpoint: " + ex.getMessage(), ex);
            }
            throw new RuntimeException("Erreur lors de l'appel timeout endpoint: " + ex.getMessage(), ex);
        }
    }

    public Map callTimeoutEndpointPost(String baseUrl, Map<String, Object> requestBody) {
        try {
            return webClient.post()
                    .uri(baseUrl + "/api/test/timeout")
                    .header("X-Test-Header", "timeout-post-test")
                    .header("X-Request-ID", "req-timeout-post-" + System.currentTimeMillis())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(2)) // Timeout plus court que le délai du serveur
                    .block(); // Mode synchrone
        } catch (Exception ex) {
            if (ex.getCause() instanceof TimeoutException || ex.getMessage().toLowerCase().contains("timeout")) {
                throw new RuntimeException("Timeout lors de l'appel timeout POST endpoint: " + ex.getMessage(), ex);
            }
            throw new RuntimeException("Erreur lors de l'appel timeout POST endpoint: " + ex.getMessage(), ex);
        }
    }
}
