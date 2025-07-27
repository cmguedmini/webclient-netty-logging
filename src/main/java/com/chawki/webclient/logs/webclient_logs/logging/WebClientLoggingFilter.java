package com.chawki.webclient.logs.webclient_logs.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ClientHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import com.chawki.webclient.logs.webclient_logs.config.WebClientLoggingConfiguration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class WebClientLoggingFilter implements ExchangeFilterFunction {

	private static final Logger log = LoggerFactory.getLogger(WebClientLoggingFilter.class);
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

	private final WebClientLoggingConfiguration loggingConfig;

	@Autowired
	public WebClientLoggingFilter(WebClientLoggingConfiguration loggingConfig) {
		this.loggingConfig = loggingConfig;
	}

	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		if (!loggingConfig.isEnabled()) {
			return next.exchange(request);
		}

		String requestId = UUID.randomUUID().toString().substring(0, 8);
		long startTime = System.currentTimeMillis();

		return next.exchange(logRequest(request, requestId))
				.doOnNext(response -> logResponse(response, requestId, startTime))
				.doOnError(error -> logError(request, error, requestId, startTime))
				.map(response -> logResponseBody(response, requestId));
	}

	private ClientRequest logRequest(ClientRequest request, String requestId) {
		log.info("=== REQUEST {} [{}] ===", requestId, FORMATTER.format(LocalDateTime.now()));
		log.info("Method: {} {}", request.method(), request.url());

		// Log URL parameters
		logUrlParameters(request, requestId);

		if (loggingConfig.isIncludeHeaders()) {
			logHeaders("Request Headers", request.headers());
		}

		if (loggingConfig.isIncludeBody() && hasBody(request.method())) {
			return ClientRequest.from(request).body((outputMessage, context) -> {
				return request.body().insert(new LoggingClientHttpRequestDecorator(outputMessage, requestId), context);
			}).build();
		}

		return request;
	}

	private void logUrlParameters(ClientRequest request, String requestId) {
		if (!loggingConfig.isIncludeParameters()) {
			return;
		}

		String query = request.url().getQuery();
		if (query != null && !query.isEmpty()) {
			log.info("Request Parameters [{}]: {}", requestId, query);
		}

		// Also log path variables if any
		String path = request.url().getPath();
		if (path != null) {
			log.info("Request Path [{}]: {}", requestId, path);
		}
	}

	// Inner class to properly handle request body logging
	private class LoggingClientHttpRequestDecorator extends ClientHttpRequestDecorator {
		private final String requestId;

		public LoggingClientHttpRequestDecorator(org.springframework.http.client.reactive.ClientHttpRequest delegate,
				String requestId) {
			super(delegate);
			this.requestId = requestId;
		}

		@Override
		public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
			return DataBufferUtils.join(body).doOnNext(dataBuffer -> {
				try {
					// Create a copy of the buffer for logging
					byte[] bytes = new byte[dataBuffer.readableByteCount()];
					// Spring 6.0+ approach: Save current position, read bytes, then restore
					// position
					int originalReadPosition = dataBuffer.readPosition();
					dataBuffer.read(bytes);
					dataBuffer.readPosition(originalReadPosition);
					String bodyContent = new String(bytes, StandardCharsets.UTF_8);
					log.info("Request Body [{}]: {}", requestId, truncateBody(bodyContent));
				} catch (Exception e) {
					log.warn("Failed to log request body [{}]: {}", requestId, e.getMessage());
				}
			}).flatMap(dataBuffer -> {
				// Write the original buffer to the actual request
				return super.writeWith(Mono.just(dataBuffer));
			});
		}

		@Override
		public Mono<Void> writeAndFlushWith(
				org.reactivestreams.Publisher<? extends org.reactivestreams.Publisher<? extends DataBuffer>> body) {
			return writeWith(Flux.from(body).flatMap(Flux::from));
		}
	}

	private void logResponse(ClientResponse response, String requestId, long startTime) {
		long duration = System.currentTimeMillis() - startTime;
		log.info("=== RESPONSE {} [{}] ({} ms) ===", requestId, FORMATTER.format(LocalDateTime.now()), duration);
		// Fix: Handle HttpStatusCode properly for Spring 6.0+
		HttpStatusCode statusCode = response.statusCode();
		String reasonPhrase = statusCode instanceof HttpStatus ? ((HttpStatus) statusCode).getReasonPhrase()
				: "Unknown";

		log.info("Status: {} {}", statusCode.value(), reasonPhrase);

		if (loggingConfig.isIncludeHeaders()) {
			logHeaders("Response Headers", response.headers().asHttpHeaders());
		}
	}

	private ClientResponse logResponseBody(ClientResponse response, String requestId) {
		if (!loggingConfig.isIncludeBody()) {
			return response;
		}

		return response.mutate().body(body -> body.map(dataBuffer -> {
			byte[] bytes = new byte[dataBuffer.readableByteCount()];
			dataBuffer.read(bytes);
			String bodyContent = new String(bytes, StandardCharsets.UTF_8);
			log.info("Response Body [{}]: {}", requestId, truncateBody(bodyContent));
			return dataBuffer.factory().wrap(bytes);
		})).build();
	}

	private void logError(ClientRequest request, Throwable error, String requestId, long startTime) {
		long duration = System.currentTimeMillis() - startTime;
		log.error("=== ERROR {} [{}] ({} ms) ===", requestId, FORMATTER.format(LocalDateTime.now()), duration);
		log.error("Request: {} {}", request.method(), request.url());
		log.error("Error Type: {}", error.getClass().getSimpleName());
		log.error("Error Message: {}", error.getMessage());

		if (log.isDebugEnabled()) {
			log.debug("Full Stack Trace:", error);
		}
	}

	private void logHeaders(String title, HttpHeaders headers) {
		log.info("{}:", title);
		headers.forEach((name, values) -> values
				.forEach(value -> log.info("  {}: {}", name, maskSensitiveHeaders(name, value))));
	}

	private String maskSensitiveHeaders(String headerName, String headerValue) {
		if (!loggingConfig.isMaskSensitiveData()) {
			return headerValue;
		}

		if (headerName.equalsIgnoreCase("Authorization") || headerName.equalsIgnoreCase("Cookie")
				|| headerName.equalsIgnoreCase("Set-Cookie") || headerName.toLowerCase().contains("token")
				|| headerName.toLowerCase().contains("key")) {
			return "***MASKED***";
		}
		return headerValue;
	}

	private String truncateBody(String body) {
		if (body == null) {
			return "null";
		}
		int maxSize = loggingConfig.getMaxBodySize();
		if (body.length() <= maxSize) {
			return body;
		}
		return body.substring(0, maxSize) + "... (truncated)";
	}

	private boolean hasBody(HttpMethod method) {
		return method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH;
	}
}
