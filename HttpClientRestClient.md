
package com.example.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import org.apache.hc.client5.http.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.Method;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.TimeValue;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * RestClientFactory adapté à l'Option 2 :
 * - Timeouts uniquement via HttpComponentsClientHttpRequestFactory
 * - Retry transparent via HttpClient5 (exceptions I/O + codes HTTP)
 * - SSLContext supporté
 * - ObjectMapper injecté dans les converters du RestClient
 */
public final class RestClientFactory {

    private RestClientFactory() { }

    /**
     * ✅ Nouvelle signature principale :
     * Construit un RestClient à partir des HttpClient5Properties + SSLContext + ObjectMapper.
     * - baseUrl OBLIGATOIRE (validation)
     * - retries transparents (exceptions + codes), backoff exponentiel
     */
    public static RestClient build(final HttpClient5Properties props,
                                   final SSLContext sslContext,
                                   final ObjectMapper objectMapper) {

        Objects.requireNonNull(props, "props ne doit pas être null");
        Objects.requireNonNull(props.getRest(), "props.rest ne doit pas être null");

        @NotBlank String baseUrl = props.getRest().getBaseUrl();
        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("httpclient5.rest.base-url est obligatoire et ne doit pas être vide");
        }

        Supplier<ClientHttpRequestFactory> supplier =
                () -> buildClientHttpRequestFactory(
                        sslContext != null ? sslContext : defaultSslContext(),
                        props.getTimeouts().getConnect(),
                        props.getTimeouts().getResponse(),
                        props.getTimeouts().getConnectionRequest(),
                        props.getRetry().getMaxAttempts(),
                        props.getRetry().getBackoff().getInitialInterval(),
                        props.getRetry().getBackoff().getMultiplier(),
                        props.getRetry().getBackoff().getMaxInterval(),
                        props.getRetry().isRetryNonIdempotent(),
                        props.getRetry().getRetryOnStatus()
                );

        return build(supplier, objectMapper, baseUrl, props.getRest().getDefaultHeaders());
    }

    /**
     * ♻️ Rétro‑compatibilité : ancienne signature (facultative).
     * Construit un RestClient en utilisant uniquement les timeouts factory + retry (exceptions)
     * et un backoff constant (delay) pour les retries "status-based".
     */
    public static RestClient build(final Duration connectionTimeout,
                                   final Duration responseTimeout,
                                   final SSLContext sslContext,
                                   final ObjectMapper objectMapper,
                                   final int maxRetries,
                                   final Duration delay,
                                   final @NotBlank String baseUrl) {

        if (!StringUtils.hasText(baseUrl)) {
            throw new IllegalArgumentException("baseUrl est obligatoire et ne doit pas être vide");
        }

        Supplier<ClientHttpRequestFactory> supplier =
                () -> buildClientHttpRequestFactory(
                        sslContext != null ? sslContext : defaultSslContext(),
                        connectionTimeout,
                        responseTimeout,
                        Duration.ofSeconds(2),          // default pour connection-request
                        maxRetries,
                        delay != null ? delay : Duration.ofMillis(200), // backoff initial
                        1.0,                            // multiplier (constant)
                        delay != null ? delay : Duration.ofMillis(200), // max = initial
                        false,                          // retry non-idempotent désactivé par défaut
                        Set.of(500, 502, 503, 504, 429) // retry-on-status par défaut
                );

        return build(supplier, objectMapper, baseUrl, null);
    }

    /**
     * ⚙️ Méthode "noyau" : construit le RestClient depuis une factory HTTP + ObjectMapper + baseUrl.
     * Permet d’injecter des headers par défaut si fournis.
     */
    public static RestClient build(final Supplier<ClientHttpRequestFactory> requestFactorySupplier,
                                   final ObjectMapper objectMapper,
                                   final @NotBlank String baseUrl,
                                   final java.util.Map<String, String> defaultHeaders) {

        Objects.requireNonNull(requestFactorySupplier, "requestFactorySupplier ne doit pas être null");
        Objects.requireNonNull(objectMapper, "objectMapper ne doit pas être null");

        ClientHttpRequestFactory factory = requestFactorySupplier.get();

        RestClient.Builder builder = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                // Injecter l'ObjectMapper dans le converter Jackson
                .messageConverters(converters -> {
                    converters.removeIf(c -> c instanceof MappingJackson2HttpMessageConverter);
                    MappingJackson2HttpMessageConverter jackson = new MappingJackson2HttpMessageConverter(objectMapper);
                    converters.add(jackson);
                });

        if (defaultHeaders != null && !defaultHeaders.isEmpty()) {
            defaultHeaders.forEach(builder::defaultHeader);
        }

        return builder.build();
    }

    /**
     * 💡 Construit la ClientHttpRequestFactory (Option 2):
     * - HttpClient5 sans RequestConfig par défaut
     * - Retry exceptions I/O (DefaultHttpRequestRetryStrategy)
     * - Retry codes HTTP (ServiceUnavailableRetryStrategy) avec backoff exponentiel
     * - Timeouts fixés uniquement via les setters de la factory Spring
     */
    public static ClientHttpRequestFactory buildClientHttpRequestFactory(final SSLContext sslContext,
                                                                         final Duration connectTimeout,
                                                                         final Duration readTimeout,
                                                                         final Duration connectionRequestTimeout,
                                                                         final int maxAttempts,
                                                                         final Duration initialBackoff,
                                                                         final double backoffMultiplier,
                                                                         final Duration maxBackoff,
                                                                         final boolean retryNonIdempotent,
                                                                         final Iterable<Integer> retryOnStatus) {

        // SSL
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                sslContext,
                null, // protocols (null = defaults)
                null, // cipher suites (null = defaults)
                // Hostname verification par défaut (STRICT) via DefaultHostnameVerifier dans SSLConnectionSocketFactory
                SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        );

        // Retry exceptions (I/O) — transparent
        int maxRetries = Math.max(0, maxAttempts - 1); // HttpClient compte les "retries", pas le 1er essai
        DefaultHttpRequestRetryStrategy exceptionRetry = new DefaultHttpRequestRetryStrategy(
                maxRetries,
                TimeValue.ofMilliseconds(safeMillis(initialBackoff))
        ) {
            @Override
            protected boolean handleAsIdempotent(final String method) {
                if (retryNonIdempotent) return true;
                return Method.isIdempotent(method); // GET/HEAD/OPTIONS/DELETE
            }
        };

        // Retry "status-based" (5xx/429) — transparent avec backoff exponentiel
        var statusStrategy = new ConfigurableStatusRetryStrategy(
                retryOnStatus,
                maxAttempts,
                safeMillis(initialBackoff),
                backoffMultiplier <= 0 ? 1.0 : backoffMultiplier,
                safeMillis(maxBackoff)
        );

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .setRetryStrategy(exceptionRetry)
                .setServiceUnavailableRetryStrategy(statusStrategy)
                .disableCookieManagement()
                .build();

        // Timeouts uniquement via la factory
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(nonNullOr(connectTimeout, Duration.ofSeconds(3)));
        factory.setReadTimeout(nonNullOr(readTimeout, Duration.ofSeconds(5)));
        factory.setConnectionRequestTimeout(nonNullOr(connectionRequestTimeout, Duration.ofSeconds(2)));
        return factory;
    }

    // --- Helpers internes ---

    private static long safeMillis(Duration d) {
        return d == null ? 0L : d.toMillis();
    }

    private static Duration nonNullOr(Duration value, Duration fallback) {
        return value != null ? value : fallback;
    }

    private static SSLContext defaultSslContext() {
        try {
            return SSLContexts.createDefault();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'initialiser l'SSLContext par défaut", e);
        }
    }

    /**
     * Stratégie de retry basée sur les codes HTTP (ex: 5xx, 429).
     * Applique un backoff exponentiel: initial * multiplier^(attempt-1), plafonné à max.
     */
    static class ConfigurableStatusRetryStrategy implements org.apache.hc.client5.http.impl.ServiceUnavailableRetryStrategy {
        private final Set<Integer> codes;
        private final int maxAttempts;
        private final long initialMs;
        private final double multiplier;
        private final long maxMs;

        ConfigurableStatusRetryStrategy(Iterable<Integer> retryOnStatus,
                                        int maxAttempts,
                                        long initialMs,
                                        double multiplier,
                                        long maxMs) {
            this.codes = new HashSet<>();
            if (retryOnStatus != null) retryOnStatus.forEach(this.codes::add);
            this.maxAttempts = Math.max(1, maxAttempts);
            this.initialMs = Math.max(0, initialMs);
            this.multiplier = Math.max(1.0, multiplier);
            this.maxMs = Math.max(initialMs, maxMs);
        }

        @Override
        public boolean retryRequest(HttpResponse response, int executionCount, HttpClientContext context) {
            // executionCount: 1 = 1er essai déjà fait; autoriser jusqu’à maxAttempts-1 retries            // executionCount: 1 = 1er essai déjà fait; autoriser jusqu’à maxAttempts-1 retries
            if (executionCount >= maxAttempts) return false;
            return codes.contains(response.getCode());
        }

        @Override
        public TimeValue getRetryInterval(HttpResponse response, int executionCount, HttpClientContext context) {
            long delay = (long) Math.min(maxMs, initialMs * Math.pow(multiplier, Math.max(0, executionCount - 1)));
            return TimeValue.ofMilliseconds(delay);
        }
    }
