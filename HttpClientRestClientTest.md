
// src/test/java/com/example/http/TestRestClientFactoryConfig.java
package com.example.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration de test injectée via @Import :
 * - Démarre MockWebServer (HTTP)
 * - Fournit SSLContext par défaut (utilisé uniquement si baseUrl HTTPS)
 * - Fournit ObjectMapper
 * - Construit HttpClient5Properties avec baseUrl obligatoire = URL du mock
 */
@TestConfiguration
@Slf4j
public class TestRestClientFactoryConfig {

    @Bean
    public MockWebServer mockWebServer() throws Exception {
        MockWebServer server = new MockWebServer();
        server.start(); // port aléatoire
        log.info("MockWebServer started on {}", server.url("/"));
        return server;
    }

    @Bean(destroyMethod = "shutdown")
    public AutoCloseable mockWebServerCloser(MockWebServer server) {
        return server::shutdown;
    }

    @Bean
    public SSLContext testSslContext() throws Exception {
        // Contexte TLS par défaut (utile pour HTTPS; ignoré en HTTP)
        return SSLContexts.createDefault();
    }

    @Bean
    public ObjectMapper testObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public HttpClient5Properties httpClient5Properties(MockWebServer server) {
        HttpClient5Properties props = new HttpClient5Properties();

        // ----- REST -----
        HttpClient5Properties.Rest rest = new HttpClient5Properties.Rest();
        // baseUrl obligatoire : on met l'URL du MockWebServer (sans slash final)
        String baseUrl = server.url("/").toString().replaceAll("/$", "");
        rest.setBaseUrl(baseUrl);
        rest.setDefaultHeaders(Map.of("Accept", "application/json"));
        props.setRest(rest);

        // ----- TIMEOUTS (courts pour tests rapides) -----
        HttpClient5Properties.Timeouts timeouts = new HttpClient5Properties.Timeouts();
        timeouts.setConnect(Duration.ofMillis(200));
        timeouts.setResponse(Duration.ofMillis(300));
        timeouts.setConnectionRequest(Duration.ofMillis(200));
        props.setTimeouts(timeouts);

        // ----- RETRY -----
        HttpClient5Properties.Backoff backoff = new HttpClient5Properties.Backoff();
        backoff.setInitialInterval(Duration.ofMillis(100));
        backoff.setMaxInterval(Duration.ofMillis(200));
        backoff.setMultiplier(1.5);

        HttpClient5Properties.Retry retry = new HttpClient5Properties.Retry();
        retry.setMaxAttempts(3); // 1 essai + 2 retries
        retry.setRetryNonIdempotent(false);
        retry.setBackoff(backoff);
        retry.setRetryOnStatus(List.of(500, 502, 503, 504, 429));
        retry.setRetryOnExceptions(List.of(
                               "java.io.IOException",
                "org.apache.hc.core5.http.NoHttpResponseException",
                "org.apache.hc.client5.http.ConnectTimeoutException",
                "org.apache.hc.client5.http.ConnectException"
        ));
        props.setRetry(retry);

        return props;
    }
