import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Service avec RestClient en mode synchrone
 * Remplace WebClient + block() sans bloquer de threads inutilement
 */
@Service
public class MyService {

    private final RestClient restClient;
    private final JefContextService jefContextService;

    public MyService(RestClient.Builder restClientBuilder, 
                     JefContextService jefContextService) {
        this.restClient = restClientBuilder
            .baseUrl("https://your-api-base-url.com")
            .defaultHeader("Content-Type", "application/json")
            .build();
        this.jefContextService = jefContextService;
    }

    /**
     * Méthode sendReceive en mode synchrone
     * Plus besoin de .block() !
     */
    public String sendReceive(Request request, String uri) {
        return restClient.post()
            .uri(uri)
            .body(buildBody(request))
            .headers(headers -> buildHeaders(request, headers, jefContextService.getMySelfPoint()))
            .retrieve()
            .body(String.class);  // ← Synchrone natif
    }

    /**
     * Construction du body (adaptez à votre logique)
     */
    private Object buildBody(Request request) {
        // Votre logique existante
        // Peut retourner String, Object, BodyInserter, etc.
        return request;
    }

    /**
     * Construction des headers (adaptez à votre logique)
     */
    private void buildHeaders(Request request, HttpHeaders headers, Object mySelfPoint) {
        // Votre logique existante
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Exemple d'ajout de headers personnalisés
        // headers.set("X-Custom-Header", request.getCustomValue());
        // headers.set("Authorization", "Bearer " + token);
    }
}

/**
 * Configuration RestClient
 */
@org.springframework.context.annotation.Configuration
class RestClientConfig {
    
    @org.springframework.context.annotation.Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder()
            .requestInterceptor((request, body, execution) -> {
                // Intercepteur optionnel pour logs, authentification, etc.
                System.out.println("Calling: " + request.getURI());
                return execution.execute(request, body);
            });
    }
}

/**
 * Classes de support (à adapter selon votre code)
 */
class Request {
    private String data;
    private String type;
    
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}

@Service
class JefContextService {
    public Object getMySelfPoint() {
        return new Object(); // Votre logique
    }
}

