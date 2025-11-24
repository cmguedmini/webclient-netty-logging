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

import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Helper class for building and configuring RestClient instances,
 * supporting custom timeouts, SSL context, and ObjectMapper configuration.
 *
 * NOTE: This class assumes the presence of Apache HttpClient 5.x dependencies.
 * The internal methods are designed to mimic the original RestTemplateHelper structure.
 */
public final class RestClientHelper {

    // --- Utility assumptions (to replace internal helper methods) ---

    // Assuming a simplified version of NumberHelper.intValue for demonstration
    private static int intValue(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    // --- Public Build Methods (Mirroring Original RestTemplateHelper) ---

    /**
     * Builds a configured RestClient using Duration for timeouts.
     */
    public static RestClient build(final Duration connectionTimeout, final Duration responseTimeout, final SSLContext sslContext, final ObjectMapper objectMapper) {
        int finalConnectionTimeout = intValue(TimeUnit.MILLISECONDS.convert(connectionTimeout));
        long finalResponseTimeout = TimeUnit.MILLISECONDS.convert(responseTimeout);
        return build(finalConnectionTimeout, finalResponseTimeout, sslContext, objectMapper);
    }

    /**
     * Builds a configured RestClient using raw int/long for timeouts.
     */
    public static RestClient build(final int connectionTimeout, final long responseTimeout, final SSLContext sslContext, final ObjectMapper objectMapper) {
        // The core logic delegates the factory creation
        return build(() -> createRequestFactory(sslContext, connectionTimeout, responseTimeout), objectMapper);
    }

    /**
     * Core method to build the RestClient using a supplied ClientHttpRequestFactory and configuring the ObjectMapper.
     */
    public static RestClient build(final Supplier<ClientHttpRequestFactory> requestFactorySupplier, final ObjectMapper objectMapper) {
        RestClient.Builder builder = RestClient.builder()
                // Use the configured request factory
                .requestFactory(requestFactorySupplier.get());

        // Configure the ObjectMapper within the message converters
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

    // --- ClientHttpRequestFactory Creation Methods ---

    /**
     * Creates a ClientHttpRequestFactory configured with Duration timeouts.
     */
    public static ClientHttpRequestFactory createRequestFactory(final SSLContext sslContext, final Duration connectionTimeout, final Duration responseTimeout) {
        int finalConnectionTimeout = intValue(TimeUnit.MILLISECONDS.convert(connectionTimeout));
        long finalResponseTimeout = TimeUnit.MILLISECONDS.convert(responseTimeout);
        return createRequestFactory(sslContext, finalConnectionTimeout, finalResponseTimeout);
    }

    /**
     * Creates the HttpComponentsClientHttpRequestFactory by building a custom Apache HttpClient.
     */
    public static ClientHttpRequestFactory createRequestFactory(final SSLContext sslContext, final int connectionTimeout, final long responseTimeout) {

        final PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder.create();

        // 1. Configure SSL Context
        if (sslContext != null) {
            final DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);
            connectionManagerBuilder.setTlsSocketStrategy(tlsStrategy);
        }

        // 2. Configure Response/Socket Timeout (for read/data transfer timeout)
        if (responseTimeout > 0) {
            connectionManagerBuilder.setDefaultSocketConfig(
                    SocketConfig.custom().setSoTimeout(Timeout.ofMilliseconds(responseTimeout)).build());
        }

        // 3. Build the underlying Apache HttpClient
        final HttpClient httpClient = HttpClients.custom()
                .disableAutomaticRetries() // Matching the original implementation
                .setConnectionManager(connectionManagerBuilder.build())
                .build();

        // 4. Wrap the HttpClient in Spring's factory
        final HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(httpClient);

        // 5. Configure Connection Timeout (for establishing the connection)
        if (connectionTimeout > 0) {
            // Using Duration-based setter, which is preferred, converting the int back to Duration.
            rf.setConnectTimeout(Duration.ofMillis(connectionTimeout));
        }

        return rf;
    }
}
