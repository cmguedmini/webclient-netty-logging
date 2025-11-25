import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;

// 1. Configuration avec RequestFactory et Interceptor
@Configuration
public class RestClientConfig {
    
    @Bean
    public RestClient restClient() {
        // Configuration de la RequestFactory avec timeouts
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        
        return RestClient.builder()
            .baseUrl("https://api.example.com")
            .requestFactory(requestFactory)
            .requestInterceptor(new RetryInterceptor(3, 1000))
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}

// 2. Interceptor personnalisé pour le retry
public class RetryInterceptor implements ClientHttpRequestInterceptor {
    
    private final int maxAttempts;
    private final long delayMillis;
    
    public RetryInterceptor(int maxAttempts, long delayMillis) {
        this.maxAttempts = maxAttempts;
        this.delayMillis = delayMillis;
    }
    
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, 
            byte[] body, 
            ClientHttpRequestExecution execution) throws IOException {
        
        IOException lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ClientHttpResponse response = execution.execute(request, body);
                
                // Retry sur erreurs 5xx ou 429 (rate limit)
                if (shouldRetry(response, attempt)) {
                    response.close();
                    waitBeforeRetry(attempt);
                    continue;
                }
                
                return response;
                
            } catch (IOException e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    waitBeforeRetry(attempt);
                }
            }
        }
        
        throw lastException != null ? lastException : 
            new IOException("Échec après " + maxAttempts + " tentatives");
    }
    
    private boolean shouldRetry(ClientHttpResponse response, int attempt) throws IOException {
        int statusCode = response.getStatusCode().value();
        return attempt < maxAttempts && (statusCode >= 500 || statusCode == 429);
    }
    
    private void waitBeforeRetry(int attempt) {
        try {
            // Backoff exponentiel
            long delay = delayMillis * (long) Math.pow(2, attempt - 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// 3. Version avancée avec logs et métriques
public class AdvancedRetryInterceptor implements ClientHttpRequestInterceptor {
    
    private final int maxAttempts;
    private final long initialDelay;
    private final long maxDelay;
    
    public AdvancedRetryInterceptor(int maxAttempts, long initialDelay, long maxDelay) {
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
    }
    
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, 
            byte[] body, 
            ClientHttpRequestExecution execution) throws IOException {
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ClientHttpResponse response = execution.execute(request, body);
                int statusCode = response.getStatusCode().value();
                
                if (isRetryableStatus(statusCode) && attempt < maxAttempts) {
                    System.out.println("Tentative " + attempt + " échouée avec status " + 
                                     statusCode + ", retry...");
                    response.close();
                    sleep(calculateDelay(attempt));
                    continue;
                }
                
                return response;
                
            } catch (IOException e) {
                if (attempt >= maxAttempts) {
                    System.err.println("Échec définitif après " + maxAttempts + " tentatives");
                    throw e;
                }
                System.out.println("Tentative " + attempt + " échouée: " + e.getMessage());
                sleep(calculateDelay(attempt));
            }
        }
        
        throw new IOException("Échec inattendu");
    }
    
    private boolean isRetryableStatus(int statusCode) {
        return statusCode >= 500 || statusCode == 429 || statusCode == 408;
    }
    
    private long calculateDelay(int attempt) {
        long delay = initialDelay * (long) Math.pow(2, attempt - 1);
        return Math.min(delay, maxDelay);
    }
    
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

// 4. Service utilisant le RestClient configuré
@Service
public class ApiService {
    
    private final RestClient restClient;
    
    public ApiService(RestClient restClient) {
        this.restClient = restClient;
    }
    
    public String getData(String endpoint) {
        // Le retry est géré automatiquement par l'interceptor
        return restClient.get()
            .uri(endpoint)
            .retrieve()
            .body(String.class);
    }
    
    public UserDto getUser(Long userId) {
        return restClient.get()
            .uri("/users/{id}", userId)
            .retrieve()
            .body(UserDto.class);
    }
}

// 5. Configuration alternative avec plusieurs interceptors
@Configuration
public class MultiInterceptorConfig {
    
    @Bean
    public RestClient restClientWithMultipleInterceptors() {
        return RestClient.builder()
            .baseUrl("https://api.example.com")
            .requestFactory(new SimpleClientHttpRequestFactory())
            .requestInterceptor(new LoggingInterceptor())
            .requestInterceptor(new RetryInterceptor(3, 1000))
            .requestInterceptor(new AuthInterceptor("Bearer token123"))
            .build();
    }
}

// Interceptor pour logging
class LoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                       ClientHttpRequestExecution execution) throws IOException {
        System.out.println("Request: " + request.getMethod() + " " + request.getURI());
        return execution.execute(request, body);
    }
}

// Interceptor pour authentification
class AuthInterceptor implements ClientHttpRequestInterceptor {
    private final String token;
    
    public AuthInterceptor(String token) {
        this.token = token;
    }
    
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, 
                                       ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set("Authorization", token);
        return execution.execute(request, body);
    }
}

record UserDto(Long id, String name, String email) {}
