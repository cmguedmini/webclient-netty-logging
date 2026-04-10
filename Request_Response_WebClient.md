---

## 1. L'implémentation du Filtre Complet
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Filtre WebClient pour logguer Request/Response et leurs Body complets.
 * Nécessite le niveau de log TRACE pour s'activer.
 */
@Slf4j
public class FullLoggingFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        // Condition d'activation : seulement si TRACE est activé
        if (!log.isTraceEnabled()) {
            return next.exchange(request);
        }

        // Conteneur pour le body de la requête (capturé lors de l'écriture réseau)
        AtomicReference<String> requestBodyRef = new AtomicReference<>("[EMPTY]");

        // 1. Décoration de la requête pour intercepter le flux de sortie
        ClientRequest decoratedRequest = ClientRequest.from(request)
                .body((outputMessage, context) -> {
                    ClientHttpRequestDecorator decorator = new ClientHttpRequestDecorator(outputMessage) {
                        @Override
                        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                            return DataBufferUtils.join(body).flatMap(buffer -> {
                                // Lecture du contenu pour le log
                                byte[] bytes = new byte[buffer.readableByteCount()];
                                buffer.read(bytes);
                                
                                // Libération immédiate du buffer original (Netty)
                                DataBufferUtils.release(buffer);
                                
                                // Stockage du contenu dans la référence
                                requestBodyRef.set(new String(bytes, StandardCharsets.UTF_8));
                                
                                // Re-création d'un buffer pour le flux réseau réel
                                return super.writeWith(Mono.just(new DefaultDataBufferFactory().wrap(bytes)));
                            });
                        }
                    };
                    return request.body().insert(decorator, context);
                })
                .build();

        // 2. Exécution et capture de la réponse
        return next.exchange(decoratedRequest)
                .flatMap(response -> {
                    // Agrégation du body de la réponse
                    return response.bodyToMono(DataBuffer.class)
                            .defaultIfEmpty(new DefaultDataBufferFactory().allocateBuffer(0))
                            .flatMap(resBuffer -> {
                                // Lecture des octets de la réponse
                                byte[] resBytes = new byte[resBuffer.readableByteCount()];
                                resBuffer.read(resBytes);
                                DataBufferUtils.release(resBuffer);
                                
                                String responseBody = new String(resBytes, StandardCharsets.UTF_8);

                                // LOG CONSOLIDÉ (Request + Response)
                                logExchange(request, requestBodyRef.get(), response, responseBody);

                                // Re-packaging de la réponse pour la suite de la chaîne réactive
                                return Mono.just(response.mutate()
                                        .body(Flux.just(new DefaultDataBufferFactory().wrap(resBytes)))
                                        .build());
                            });
                });
    }

    private void logExchange(ClientRequest req, String reqBody, ClientResponse res, String resBody) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- WEBCLIENT EXCHANGE ---");
        sb.append("\nURL         : ").append(req.method()).append(" ").append(req.url());
        sb.append("\nREQ BODY    : ").append(reqBody);
        sb.append("\nRES STATUS  : ").append(res.statusCode());
        sb.append("\nRES BODY    : ").append(resBody);
        sb.append("\n--------------------------");
        
        log.trace(sb.toString());
    }
}
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

# Proposition Copilot
package com.example.webclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public class ZeroCopyLoggingFilterCompatible implements ExchangeFilterFunction {

    private static final Logger log = LoggerFactory.getLogger("webclient.chase");
    private static final DefaultDataBufferFactory BUFFER_FACTORY = new DefaultDataBufferFactory();

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {

        if (!log.isTraceEnabled()) {
            return next.exchange(request);
        }

        StringBuilder sb = new StringBuilder("\n--- WEBCLIENT TRACE ---\n");

        sb.append("➡️ REQUEST: ").append(request.method()).append(" ").append(request.url()).append("\n");
        request.headers().forEach((k, v) -> sb.append("REQ-HEADER: ").append(k).append("=").append(v).append("\n"));

        ClientRequest decoratedRequest = decorateRequest(request, sb);

        return next.exchange(decoratedRequest)
                .flatMap(response -> logAndBufferResponse(response, sb));
    }

    private ClientRequest decorateRequest(ClientRequest request, StringBuilder sb) {

        return ClientRequest.from(request)
                .body((outputMessage, context) -> {

                    ClientHttpRequestDecorator decorator = new ClientHttpRequestDecorator(outputMessage) {

                        @Override
                        public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {

                            Flux<DataBuffer> loggingBody = Flux.from(body)
                                    .map(buffer -> {
                                        byte[] bytes = extract(buffer);
                                        sb.append("REQ-BODY: ").append(new String(bytes, StandardCharsets.UTF_8)).append("\n");
                                        return buffer;
                                    });

                            return super.writeWith(loggingBody);
                        }
                    };

                    return request.body().insert(decorator, context);
                })
                .build();
    }

    private Mono<ClientResponse> logAndBufferResponse(ClientResponse response, StringBuilder sb) {

        sb.append("⬅️ RESPONSE-STATUS: ").append(response.statusCode()).append("\n");
        response.headers().asHttpHeaders()
                .forEach((k, v) -> sb.append("RES-HEADER: ").append(k).append("=").append(v).append("\n"));

        return DataBufferUtils.join(response.bodyToFlux(DataBuffer.class))
                .defaultIfEmpty(BUFFER_FACTORY.wrap(new byte[0]))
                .flatMap(buffer -> {

                    byte[] bytes = extract(buffer);
                    sb.append("RES-BODY: ").append(new String(bytes, StandardCharsets.UTF_8)).append("\n");
                    sb.append("--- END TRACE ---");

                    log.trace(sb.toString());

                    ClientResponse rebuilt = ClientResponse.create(response.statusCode())
                            .headers(h -> h.addAll(response.headers().asHttpHeaders()))
                            .body(Flux.just(BUFFER_FACTORY.wrap(bytes)))
                            .build();

                    return Mono.just(rebuilt);
                });
    }

    private byte[] extract(DataBuffer buffer) {
        try {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            return bytes;
        } finally {
            DataBufferUtils.release(buffer);
        }
    }
}

## Test

package com.example.webclient;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ZeroCopyLoggingFilterCompatibleTest {

    private ZeroCopyLoggingFilterCompatible filter;
    private ExchangeFunction next;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setup() {
        filter = new ZeroCopyLoggingFilterCompatible();
        next = mock(ExchangeFunction.class);

        Logger logger = (Logger) LoggerFactory.getLogger("webclient.chase");
        logger.setLevel(Level.TRACE);

        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    // ------------------------------------------------------------
    // 1. Test standard request/response
    // ------------------------------------------------------------
    @Test
    void testFilterLogsRequestAndResponse() {
        ClientRequest request = ClientRequest.create()
                .method("POST")
                .url("http://localhost/test")
                .bodyValue("{\"hello\":\"world\"}")
                .build();

        byte[] responseBytes = "{\"status\":\"ok\"}".getBytes(StandardCharsets.UTF_8);

        ClientResponse mockResponse = ClientResponse
                .create(200)
                .body(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(responseBytes)))
                .build();

        when(next.exchange(any())).thenReturn(Mono.just(mockResponse));

        Mono<String> result = filter.filter(request, next)
                .flatMap(resp -> resp.bodyToMono(String.class));

        StepVerifier.create(result)
                .expectNext("{\"status\":\"ok\"}")
                .verifyComplete();

        String log = logAppender.list.get(0).getFormattedMessage();
        assert log.contains("REQ-BODY: {\"hello\":\"world\"}");
        assert log.contains("RES-BODY: {\"status\":\"ok\"}");
    }

    // ------------------------------------------------------------
    // 2. Body vide
    // ------------------------------------------------------------
    @Test
    void testEmptyBody() {
        ClientRequest request = ClientRequest.create()
                .method("GET")
                .url("http://localhost/empty")
                .build();

        ClientResponse mockResponse = ClientResponse
                .create(200)
                .body(Flux.empty())
                .build();

        when(next.exchange(any())).thenReturn(Mono.just(mockResponse));

        Mono<String> result = filter.filter(request, next)
                .flatMap(resp -> resp.bodyToMono(String.class));

        StepVerifier.create(result)
                .expectComplete()
                .verify();

        String log = logAppender.list.get(0).getFormattedMessage();
        assert log.contains("REQ-BODY:");
        assert log.contains("RES-BODY:");
    }

    // ------------------------------------------------------------
    // 3. Multi-chunks
    // ------------------------------------------------------------
    @Test
    void testMultiChunksResponse() {
        ClientRequest request = ClientRequest.create()
                .method("GET")
                .url("http://localhost/chunks")
                .build();

        byte[] chunk1 = "Hello ".getBytes(StandardCharsets.UTF_8);
        byte[] chunk2 = "World".getBytes(StandardCharsets.UTF_8);

        ClientResponse mockResponse = ClientResponse
                .create(200)
                .body(Flux.just(
                        DefaultDataBufferFactory.sharedInstance.wrap(chunk1),
                        DefaultDataBufferFactory.sharedInstance.wrap(chunk2)
                ))
                .build();

        when(next.exchange(any())).thenReturn(Mono.just(mockResponse));

        Mono<String> result = filter.filter(request, next)
                .flatMap(resp -> resp.bodyToMono(String.class));

        StepVerifier.create(result)
                .expectNext("Hello World")
                .verifyComplete();

        String log = logAppender.list.get(0).getFormattedMessage();
        assert log.contains("RES-BODY: Hello World");
    }

    // ------------------------------------------------------------
    // 4. Erreur HTTP
    // ------------------------------------------------------------
    @Test
    void testErrorResponse() {
        ClientRequest request = ClientRequest.create()
                .method("GET")
                .url("http://localhost/error")
                .build();

        byte[] errorBody = "{\"error\":\"bad\"}".getBytes(StandardCharsets.UTF_8);

        ClientResponse mockResponse = ClientResponse
                .create(500)
                .body(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(errorBody)))
                .build();

        when(next.exchange(any())).thenReturn(Mono.just(mockResponse));

        Mono<String> result = filter.filter(request, next)
                .flatMap(resp -> resp.bodyToMono(String.class));

        StepVerifier.create(result)
                .expectNext("{\"error\":\"bad\"}")
                .verifyComplete();

        String log = logAppender.list.get(0).getFormattedMessage();
        assert log.contains("RESPONSE-STATUS: 500");
        assert log.contains("RES-BODY: {\"error\":\"bad\"}");
    }

    // ------------------------------------------------------------
    // 5. Payload volumineux
    // ------------------------------------------------------------
    @Test
    void testLargePayload() {
        String large = "A".repeat(50_000);
        byte[] bytes = large.getBytes(StandardCharsets.UTF_8);

        ClientRequest request = ClientRequest.create()
                .method("POST")
                .url("http://localhost/large")
                .bodyValue(large)
                .build();

        ClientResponse mockResponse = ClientResponse
                .create(200)
                .body(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes)))
                .build();

        when(next.exchange(any())).thenReturn(Mono.just(mockResponse));

        Mono<String> result = filter.filter(request, next)
                .flatMap(resp -> resp.bodyToMono(String.class));

        StepVerifier.create(result)
                .expectNext(large)
                .verifyComplete();

        String log = logAppender.list.get(0).getFormattedMessage();
        assert log.contains("REQ-BODY:");
        assert log.contains("RES-BODY:");
    }

    // ------------------------------------------------------------
    // 6. TRACE désactivé → aucun log
    // ------------------------------------------------------------
    @Test
    void testNoLogWhenNotTrace() {
        Logger logger = (Logger) LoggerFactory.getLogger("webclient.chase");
        logger.setLevel(Level.DEBUG); // TRACE désactivé

        ClientRequest request = ClientRequest.create()
                .method("GET")
                .url("http://localhost/nolog")
                .build();

        ClientResponse mockResponse = ClientResponse
                .create(200)
                .body(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap("OK".getBytes())))
                .build();

        when(next.exchange(any())).thenReturn(Mono.just(mockResponse));

        Mono<String> result = filter.filter(request, next)
                .flatMap(resp -> resp.bodyToMono(String.class));

        StepVerifier.create(result)
                .expectNext("OK")
                .verifyComplete();

        assert logAppender.list.isEmpty();
    }
}
