package com.example.util;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * Intercepteur de requête pour implémenter la logique de réessai (retry)
 * pour le RestClient.
 * Il capture les erreurs de type IOException (connexion, timeout) et réexécute la requête.
 */
public class RetryClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final int maxRetries;
    private final long delayMs;

    /**
     * Constructeur.
     * @param maxRetries Nombre maximal de tentatives de réessai après l'échec initial.
     * @param delayMs Délai en millisecondes entre les tentatives.
     */
    public RetryClientHttpRequestInterceptor(int maxRetries, long delayMs) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be non-negative");
        }
        this.maxRetries = maxRetries;
        this.delayMs = delayMs;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        // Logique de réessai
        IOException lastException = null;

        // La boucle exécute la tentative initiale (attempt=0) plus 'maxRetries'
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                // Délai avant de réessayer
                System.out.println("Retry attempt " + attempt + "/" + maxRetries + " after " + delayMs + "ms delay.");
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Retry interrupted", e);
                }
            }

            try {
                // Exécuter la requête
                ClientHttpResponse response = execution.execute(request, body);

                // Succès : retourner la réponse immédiatement.
                return response;

            } catch (IOException e) {
                // Échec de connexion, timeout, ou autre erreur I/O
                lastException = e;
                System.err.println("Attempt failed with: " + e.getMessage());
                // Continuer la boucle pour la prochaine tentative
            }
        }

        // Si toutes les tentatives ont échoué, relancer la dernière exception capturée
        throw Optional.ofNullable(lastException)
                .orElseThrow(() -> new IOException("All retry attempts failed."));
    }
}
