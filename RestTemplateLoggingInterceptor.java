import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // On ne loggue que si TRACE est activé pour garder la cohérence avec le WebClient
        if (!log.isTraceEnabled()) {
            return execution.execute(request, body);
        }

        // 1. Log de la requête
        String requestBody = new String(body, StandardCharsets.UTF_8);
        
        // 2. Exécution de l'appel
        ClientHttpResponse response = execution.execute(request, body);

        // 3. Log de la réponse (nécessite un BufferingClientHttpRequestFactory)
        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);

        logExchange(request, requestBody, response, responseBody);

        return response;
    }

    private void logExchange(HttpRequest req, String reqBody, ClientHttpResponse res, String resBody) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- RESTTEMPLATE EXCHANGE ---");
        sb.append("\nURL         : ").append(req.getMethod()).append(" ").append(req.getURI());
        sb.append("\nREQ BODY    : ").append(reqBody.isEmpty() ? "[EMPTY]" : reqBody);
        sb.append("\nRES STATUS  : ").append(res.getStatusCode());
        sb.append("\nRES BODY    : ").append(resBody.isEmpty() ? "[EMPTY]" : resBody);
        sb.append("\n-----------------------------");
        
        log.trace(sb.toString());
    }
}
