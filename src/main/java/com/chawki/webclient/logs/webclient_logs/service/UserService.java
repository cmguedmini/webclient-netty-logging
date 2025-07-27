package com.chawki.webclient.logs.webclient_logs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.chawki.webclient.logs.webclient_logs.dto.User;
import com.chawki.webclient.logs.webclient_logs.exception.UserNotFoundException;
import com.chawki.webclient.logs.webclient_logs.exception.WebClientException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final WebClient webClient;

    @Autowired
    public UserService(WebClient webClient) {
        this.webClient = webClient;
    }

    public Flux<User> getAllUsers() {
        log.info("Fetching all users");
        
        return webClient.get()
                .uri("/users")
                .retrieve()
                .bodyToFlux(User.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapException)
                .doOnNext(user -> log.debug("Retrieved user: {}", user))
                .doOnComplete(() -> log.info("Successfully fetched all users"))
                .doOnError(error -> log.error("Error fetching all users: {}", error.getMessage()));
    }

    public Mono<User> getUserById(Long id) {
        log.info("Fetching user with id: {}", id);
        
        return webClient.get()
                .uri("/users/{id}", id)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, 
                    response -> Mono.error(new UserNotFoundException("User not found with id: " + id)))
                .bodyToMono(User.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapException)
                .doOnNext(user -> log.info("Successfully fetched user: {}", user))
                .doOnError(error -> log.error("Error fetching user with id {}: {}", id, error.getMessage()));
    }

    public Mono<User> createUser(User user) {
        log.info("Creating new user: {}", user.getName());
        
        return webClient.post()
                .uri("/users")
                .bodyValue(user)
                .retrieve()
                .bodyToMono(User.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapException)
                .doOnNext(createdUser -> log.info("Successfully created user with id: {}", createdUser.getId()))
                .doOnError(error -> log.error("Error creating user: {}", error.getMessage()));
    }

    public Mono<User> updateUser(Long id, User user) {
        log.info("Updating user with id: {}", id);
        
        return webClient.put()
                .uri("/users/{id}", id)
                .bodyValue(user)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                    response -> Mono.error(new UserNotFoundException("User not found with id: " + id)))
                .bodyToMono(User.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapException)
                .doOnNext(updatedUser -> log.info("Successfully updated user: {}", updatedUser))
                .doOnError(error -> log.error("Error updating user with id {}: {}", id, error.getMessage()));
    }

    public Mono<Void> deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);
        
        return webClient.delete()
                .uri("/users/{id}", id)
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals,
                    response -> Mono.error(new UserNotFoundException("User not found with id: " + id)))
                .bodyToMono(Void.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapException)
                .doOnSuccess(v -> log.info("Successfully deleted user with id: {}", id))
                .doOnError(error -> log.error("Error deleting user with id {}: {}", id, error.getMessage()));
    }

    // New method to demonstrate URL parameters logging
    public Flux<User> getUsersWithParameters(Integer page, Integer limit) {
        log.info("Fetching users with pagination - page: {}, limit: {}", page, limit);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/users")
                        .queryParam("_page", page)
                        .queryParam("_limit", limit)
                        .build())
                .retrieve()
                .bodyToFlux(User.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapException)
                .doOnNext(user -> log.debug("Retrieved user with params: {}", user))
                .doOnComplete(() -> log.info("Successfully fetched users with parameters"))
                .doOnError(error -> log.error("Error fetching users with parameters: {}", error.getMessage()));
    }

    // Method to demonstrate complex request body
    public Mono<User> createUserWithComplexBody(User user, String source, Boolean notify) {
        log.info("Creating user with complex body - source: {}, notify: {}", source, notify);
        
        // Create a request wrapper with additional metadata
        var requestBody = new java.util.HashMap<String, Object>();
        requestBody.put("user", user);
        requestBody.put("metadata", java.util.Map.of(
                "source", source != null ? source : "api",
                "notify", notify != null ? notify : false,
                "timestamp", java.time.Instant.now().toString()
        ));
        
        return webClient.post()
                .uri("/users")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(User.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .onErrorMap(this::mapException)
                .doOnNext(createdUser -> log.info("Successfully created user with complex body: {}", createdUser.getId()))
                .doOnError(error -> log.error("Error creating user with complex body: {}", error.getMessage()));
    }

    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            return ex.getStatusCode().is5xxServerError() || 
                   ex.getStatusCode() == HttpStatus.REQUEST_TIMEOUT;
        }
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.util.concurrent.TimeoutException;
    }

    private Throwable mapException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException ex = (WebClientResponseException) throwable;
            log.error("WebClient error - Status: {}, Body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                return new UserNotFoundException("Resource not found");
            }
            return new WebClientException("HTTP error: " + ex.getStatusCode(), ex);
        }
        
        if (throwable instanceof java.net.ConnectException) {
            return new WebClientException("Connection failed: " + throwable.getMessage(), throwable);
        }
        
        if (throwable instanceof java.util.concurrent.TimeoutException) {
            return new WebClientException("Request timeout: " + throwable.getMessage(), throwable);
        }
        
        return throwable;
    }
}