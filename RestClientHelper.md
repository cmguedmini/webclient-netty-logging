package com.example.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.ClientHttpRequestInterceptor;

import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

// Classe utilitaire pour la conversion de type (similaire à NumberHelper de l'original)
class NumberHelper {
    public static int intValue(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}

/**
 * Helper class for building and configuring RestClient instances,
 * supporting custom timeouts, SSL context, ObjectMapper, and Retry logic via interceptor.
 */
public final class RestClientHelper {

    // --- Public Build Methods avec Retry (utilisant Duration) ---

    public static RestClient build(final Duration connectionTimeout, final Duration responseTimeout, final SSLContext sslContext, final ObjectMapper objectMapper, final int maxRetries, final long retryDelayMs) {
        int finalConnectionTimeout = NumberHelper.intValue(TimeUnit.MILLISECONDS.convert(connectionTimeout));
        long finalResponseTimeout = TimeUnit.MILLISECONDS.convert(responseTimeout);
        return build(finalConnectionTimeout, finalResponseTimeout, sslContext, objectMapper, maxRetries, retryDelayMs);
    }

    // --- Public Build Methods avec Retry (utilisant des ms) ---

    public static RestClient build(final int connectionTimeout, final long responseTimeout, final SSLContext sslContext, final ObjectMapper objectMapper, final int maxRetries, final long retryDelayMs) {
        // Crée la liste des intercepteurs
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        // Ajout conditionnel de l'intercepteur de réessai
        if (maxRetries > 0) {
            // Utilise l'intercepteur défini pour gérer la logique de retry
            interceptors.add(new RetryClientHttpRequestInterceptor(maxRetries, retryDelayMs));
        }

        // Appel à la méthode de construction principale
        return build(() -> createRequestFactory(sslContext, connectionTimeout, responseTimeout), objectMapper, interceptors);
    }

    // --- Core Build Method (gère l'injection de Factory, ObjectMapper et Intercepteurs) ---

    public static RestClient build(final Supplier<ClientHttpRequestFactory> requestFactorySupplier, final ObjectMapper objectMapper, final List<ClientHttpRequestInterceptor> interceptors) {
        RestClient.Builder builder = RestClient.builder()
                // Applique la ClientHttpRequestFactory pour la config bas niveau (timeouts, SSL)
                .requestFactory(requestFactorySupplier.get())
                // Applique les intercepteurs (y compris le Retry Interceptor)
                .requestInterceptors(interceptorList -> interceptorList.addAll(interceptors));

        // Configure l'ObjectMapper dans le message converter Jackson
        if (objectMapper != null) {
            builder.messageConverters(converters -> {
                converters.stream()
                        .filter(MappingJackson2HttpMessageConverter.class::isInstance)
                        .map(MappingJackson2HttpMessageConverter.class::cast)
                        .forEach(m -> m.setObjectMapper(objectMapper));
            });
        }

        return builder.build();
    }

    // --- ClientHttpRequestFactory Creation Methods (Transférés de RestTemplateHelper) ---

    public static ClientHttpRequestFactory createRequestFactory(final SSLContext sslContext, final Duration connectionTimeout, final Duration responseTimeout) {
        int finalConnectionTimeout = NumberHelper.intValue(TimeUnit.MILLISECONDS.convert(connectionTimeout));
        long finalResponseTimeout = TimeUnit.MILLISECONDS.convert(responseTimeout);
        return createRequestFactory(sslContext, finalConnectionTimeout, finalResponseTimeout);
    }

    public static ClientHttpRequestFactory createRequestFactory(final SSLContext sslContext, final int connectionTimeout, final long responseTimeout) {

        final PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create();

        // 1. Configure SSL Context
        if (sslContext != null) {
            final DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);
            connectionManagerBuilder.setTlsSocketStrategy(tlsStrategy);
        }

        // 2. Configure Response/Socket Timeout
        if (responseTimeout > 0) {
            connectionManagerBuilder.setDefaultSocketConfig(
                    SocketConfig.custom().setSoTimeout(Timeout.ofMilliseconds(responseTimeout)).build());
        }

        // 3. Build the underlying Apache HttpClient
        final HttpClient httpClient = HttpClients.custom()
                .disableAutomaticRetries() // Désactivation des retries HTTP natifs pour laisser l'intercepteur Spring gérer la logique
                .setConnectionManager(connectionManagerBuilder.build())
                .build();

        // 4. Wrap the HttpClient in Spring's factory
        final HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(httpClient);

        // 5. Configure Connection Timeout
        if (connectionTimeout > 0) {
            rf.setConnectTimeout(Duration.ofMillis(connectionTimeout));
        }

        return rf;
    }
}
