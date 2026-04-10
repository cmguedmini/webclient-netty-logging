Pour obtenir une solution complète (Requête + Réponse) avec le corps complet (Full Body) tout en restant "safe", nous allons utiliser une approche par **décoration de flux**.

En Spring Boot 2.7 (Java 11), la difficulté est que le corps de la requête est souvent écrit *après* que le filtre a passé la main au filtre suivant. Pour capturer le JSON sortant, nous devons donc intercepter l'écriture des octets.

Voici le composant complet et robuste :

---

## 1. L'implémentation du Filtre Complet

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class FullLoggingFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        if (!log.isTraceEnabled()) {
            return next.exchange(request);
        }

        StringBuilder logMessage = new StringBuilder("\n--- WEBCLIENT CHASE ---\n");
        
        // 1. Intercepter le corps de la REQUÊTE
        ClientRequest decoratedRequest = ClientRequest.from(request)
                .body(new BodyInserterDecorator(request.body(), logMessage))
                .build();

        logMessage.append(String.format("REQ: %s %s\n", request.method(), request.url()));
        request.headers().forEach((name, values) -> logMessage.append(String.format("REQ-HEADER: %s=%s\n", name, values)));

        return next.exchange(decoratedRequest).flatMap(response -> {
            // 2. Intercepter le corps de la RÉPONSE
            return response.bodyToMono(DataBuffer.class)
                    .defaultIfEmpty(new DefaultDataBufferFactory().allocateBuffer(0))
                    .flatMap(buffer -> {
                        byte[] resBytes = new byte[buffer.readableByteCount()];
                        buffer.toByteBuffer().get(resBytes);
                        String resBody = new String(resBytes, StandardCharsets.UTF_8);

                        logMessage.append(String.format("RES-STATUS: %s\n", response.statusCode()));
                        logMessage.append(String.format("RES-BODY: %s\n", resBody));
                        logMessage.append("-----------------------");
                        
                        log.trace(logMessage.toString());

                        // Re-packaging pour que le client puisse lire la réponse
                        return Mono.just(response.mutate()
                                .body(Flux.just(new DefaultDataBufferFactory().wrap(resBytes)))
                                .build());
                    });
        });
    }

    // Classe interne pour capturer le body de la requête sans le bloquer
    private static class BodyInserterDecorator implements BodyInserter<Object, ClientHttpRequest> {
        private final BodyInserter<?, ? super ClientHttpRequest> delegate;
        private final StringBuilder logMessage;

        BodyInserterDecorator(BodyInserter<?, ? super ClientHttpRequest> delegate, StringBuilder logMessage) {
            this.delegate = delegate;
            this.logMessage = logMessage;
        }

        @Override
        public Mono<Void> insert(ClientHttpRequest outputMessage, Context context) {
            return delegate.insert(new ClientHttpRequestDecorator(outputMessage) {
                @Override
                public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                    return super.writeWith(Flux.from(body).map(buffer -> {
                        byte[] bytes = new byte[buffer.readableByteCount()];
                        buffer.slice(0, buffer.readableByteCount()).read(bytes);
                        logMessage.append(String.format("REQ-BODY: %s\n", new String(bytes, StandardCharsets.UTF_8)));
                        return buffer;
                    }));
                }
            }, context);
        }
    }
}
```

---

## 2. Pourquoi cette solution est la plus robuste ?

* **Gestion des Headers :** Elle liste tous les headers sortants pour un debug complet.
* **Atomicité du Log :** On construit le message dans un `StringBuilder` pour que la requête et la réponse apparaissent (souvent) ensemble dans les logs, facilitant la lecture.
* **Non-Blocking Replay :** * Pour la **requête**, on utilise `buffer.slice()` pour lire les données sans modifier l'index de lecture original. Le serveur distant reçoit bien les données.
    * Pour la **réponse**, on utilise `response.mutate().body()` pour réinjecter les octets lus dans un nouveau flux. Ton service métier recevra le JSON comme si de rien n'était.
* **Sécurité des Buffers :** En utilisant `DefaultDataBufferFactory().wrap(resBytes)`, on évite les problèmes de fuites de mémoire liés au comptage de références (`ReferenceCounted`) de Netty, car on travaille sur un tableau d'octets managé par la JVM.

---

## 3. Mise en œuvre

### Configuration Java
```java
@Bean
public WebClient webClient() {
    return WebClient.builder()
            .filter(new FullLoggingFilter())
            .build();
}
```

### Configuration application.yml
```yaml
logging:
  level:
    com.votrepaquet.FullLoggingFilter: TRACE
```

---

## ⚠️ Point d'attention final
Cette solution est parfaite pour des API REST standard. Cependant, si vous transférez des **fichiers binaires** (PDF, Images) via WebClient, le `new String(bytes)` va tenter de transformer le binaire en texte et le `log.trace` va saturer votre console/fichier de log. 

Si vous avez ce cas, je vous conseille d'ajouter une vérification sur le `Content-Type` dans le filtre pour ne logguer le body que si c'est du `application/json` ou `text/plain`.
