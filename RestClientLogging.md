C'est une excellente suite. Avec l'arrivée de Spring Framework 6 (et son backport partiel ou l'usage de bibliothèques similaires dans l'écosystème Spring Boot 2.7), le **`RestClient`** est devenu le successeur moderne et synchrone de `WebClient`.

Pour `RestClient`, le mécanisme est très proche de `RestTemplate` : on utilise un **`ClientHttpRequestInterceptor`**. Cependant, la gestion du body doit rester prudente pour ne pas consommer le flux de données prématurément.

---

## 1. L'intercepteur pour RestClient

Voici l'implémentation. Comme pour `RestTemplate`, la clé est de s'assurer que le body est lisible deux fois si on ne veut pas bloquer l'application.

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class RestClientLoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        if (!log.isTraceEnabled()) {
            return execution.execute(request, body);
        }

        // 1. Capture du corps de la requête
        String reqBody = new String(body, StandardCharsets.UTF_8);
        
        // 2. Exécution
        ClientHttpResponse response = execution.execute(request, body);

        // 3. Capture du corps de la réponse
        // Note : Nécessite une configuration de buffering sur le RestClient
        byte[] resBytes = StreamUtils.copyToByteArray(response.getBody());
        String resBody = new String(resBytes, StandardCharsets.UTF_8);

        logExchange(request, reqBody, response, resBody);

        // On doit renvoyer une réponse dont le stream peut être relu 
        // ou s'assurer que le RestClient utilise une BufferingFactory.
        return response;
    }

    private void logExchange(HttpRequest req, String reqBody, ClientHttpResponse res, String resBody) throws IOException {
        log.trace("\n--- RESTCLIENT EXCHANGE ---\nURL: {} {}\nREQ: {}\nRES STATUS: {}\nRES: {}\n---------------------------",
                req.getMethod(), req.getURI(), 
                reqBody.isEmpty() ? "[EMPTY]" : reqBody, 
                res.getStatusCode(), 
                resBody.isEmpty() ? "[EMPTY]" : resBody);
    }
}
```

---

## 2. Configuration du RestClient (Le Helper)

Pour que cet intercepteur fonctionne sans "tuer" le flux de réponse, il est impératif d'utiliser la **`BufferingClientHttpRequestFactory`**. Sans elle, votre application recevra une erreur "Stream closed" après le passage de l'intercepteur.

```java
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public RestClient restClient(RestClient.Builder builder) {
    return builder
            .requestFactory(new BufferingClientHttpRequestFactory(new SimpleClientHttpRequestFactory()))
            .requestInterceptor(new RestClientLoggingInterceptor())
            .build();
}
```

---

## 3. Test Unitaire (Sans MockServer)

Puisque `RestClient` utilise les mêmes interfaces que `RestTemplate` pour ses intercepteurs (`ClientHttpRequestInterceptor`), tu peux réutiliser la logique de test Mockito que nous avons vue.

```java
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RestClientLoggingInterceptorTest {

    @Test
    void shouldLogRestClientExchange() throws IOException {
        // GIVEN
        RestClientLoggingInterceptor interceptor = new RestClientLoggingInterceptor();
        HttpRequest request = mock(HttpRequest.class);
        ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);
        ClientHttpResponse response = mock(ClientHttpResponse.class);

        byte[] reqBody = "{\"ping\":\"pong\"}".getBytes(StandardCharsets.UTF_8);
        String resBody = "{\"status\":\"alive\"}";

        when(request.getURI()).thenReturn(java.net.URI.create("/test"));
        when(request.getMethod()).thenReturn(org.springframework.http.HttpMethod.POST);
        when(response.getBody()).thenReturn(new ByteArrayInputStream(resBody.getBytes(StandardCharsets.UTF_8)));
        when(response.getStatusCode()).thenReturn(org.springframework.http.HttpStatus.OK);
        when(execution.execute(any(), any())).thenReturn(response);

        // WHEN
        interceptor.intercept(request, reqBody, execution);

        // THEN
        verify(execution).execute(request, reqBody);
    }
}
```

---

## Pourquoi c'est cohérent ?

Le concept reste identique sur tes trois clients :
1.  **Vérification du niveau TRACE** (Performance).
2.  **Lecture du body en tableau d'octets** (Sécurité).
3.  **Usage de Buffering** (Replayability).

**Attention particulière :** Si tu es sur une version très ancienne de Spring Boot 2.7, `RestClient` n'est pas disponible nativement (il est apparu avec Spring 6 / Boot 3). Si tu l'utilises via une dépendance backportée, vérifie bien que les packages des intercepteurs sont les mêmes.

Est-ce que cette approche unifiée pour tes trois "Helpers" (Web, Template, Rest) te convient ?
