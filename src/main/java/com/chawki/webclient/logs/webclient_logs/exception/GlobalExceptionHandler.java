package com.chawki.webclient.logs.webclient_logs.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(UserNotFoundException.class)
	public Mono<ResponseEntity<Map<String, Object>>> handleUserNotFoundException(UserNotFoundException ex) {
		log.error("User not found: {}", ex.getMessage());

		Map<String, Object> errorResponse = createErrorResponse(HttpStatus.NOT_FOUND.value(), "User Not Found",
				ex.getMessage());

		return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
	}

	private Map<String, Object> createErrorResponse(int status, String error, String message) {
		Map<String, Object> errorResponse = new HashMap<>();
		errorResponse.put("timestamp", Instant.now().toString());
		errorResponse.put("status", status);
		errorResponse.put("error", error);
		errorResponse.put("message", message);

		return errorResponse;
	}

	@ExceptionHandler(WebClientException.class)
	public Mono<ResponseEntity<Map<String, Object>>> handleWebClientException(WebClientException ex) {
		log.error("WebClient error: {}", ex.getMessage(), ex);

		Map<String, Object> errorResponse = createErrorResponse(HttpStatus.UNAVAILABLE_FOR_LEGAL_REASONS.value(),
				"User Not Found", ex.getMessage());

		return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse));
	}
}