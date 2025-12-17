
// src/main/java/com/example/http/HttpClient5Properties.java
package com.example.http;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@Validated
@ConfigurationProperties(prefix = "httpclient5")
public class HttpClient5Properties {

    @NotNull
    private Rest rest = new Rest();

    @NotNull
    private Timeouts timeouts = new Timeouts();

    @NotNull
    private Retry retry = new Retry();

    @Data
    @NoArgsConstructor
    public static class Rest {
        /** ⚠️ Obligatoire */
        @NotBlank(message = "httpclient5.rest.base-url est obligatoire")
        private String baseUrl;

        /** Optionnel */
        private Map<String, String> defaultHeaders;
    }

    @Data
    @NoArgsConstructor
    public static class Timeouts {
        /** defaults */
        @NotNull private Duration connect = Duration.ofSeconds(3);
        @NotNull private Duration response = Duration.ofSeconds(5);
        @NotNull private Duration connectionRequest = Duration.ofSeconds(2);
    }

    @Data
    @NoArgsConstructor
    public static class Retry {
        /** defaults */
        @Min(1) private int maxAttempts = 3;           // total (1 essai + n-1 retries)
        private boolean retryNonIdempotent = false;    // ré-essayer POST/PUT si true

        @NotNull private Backoff backoff = new Backoff();

        @NotEmpty private List<@Min(100) @Max(599) Integer> retryOnStatus =
                List.of(500, 502, 503, 504, 429);

        @NotEmpty private List<@NotBlank String> retryOnExceptions =
                List.of(
                    "java.io.IOException",
                    "org.apache.hc.core5.http.NoHttpResponseException",
                    "org.apache.hc.client5.http.ConnectTimeoutException",
                    "org.apache.hc.client5.http.ConnectException"
                );
    }

    @Data
    @NoArgsConstructor
    public static class Backoff {
        @NotNull private Duration initialInterval = Duration.ofMillis(200);
        @NotNull private Duration maxInterval = Duration.ofSeconds(2);
        @DecimalMin("1.0") private double multiplier = 2.0;
    }
}
