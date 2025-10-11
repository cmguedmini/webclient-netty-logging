# 🚀 The End of the RestTemplate Era: Migrate to RestClient for Centralized Security in Spring Boot 3.x

By **[Your Name]** | **[Your Professional Title]**

For years, **`RestTemplate`** was that trusty, slightly grumpy old sedan in the Spring family. It got us where we needed to go, but without any modern flair. Now, with Spring Boot 3.2 and Framework 6.1, it's time to upgrade!

Enter **`RestClient`** 🦸. It's the smooth, synchronous successor built on the powerful infrastructure of `WebClient`. This article breaks down why moving to `RestClient` is essential—not just for cleaner code, but for **saving your application from itself**. We'll cover architecture, security, and why forcing a reactive client to block is like making a race car haul gravel.

---

## ⚠️ Why Ditch RestTemplate? The Retirement Party

Let's be frank: **`RestTemplate` is officially on the retirement list.** It's in maintenance mode, won't get any cool new features, and is scheduled to be removed in a future major Spring release. Sticking with it is choosing a path toward legacy debt.

Migrating to **`RestClient`** is your **future-proofing insurance**. It's the officially supported, actively developed synchronous client that aligns perfectly with modern Spring Boot 3.x observability standards. Time to say *au revoir* to the old sedan and embrace the new sports car!

---

## 📊 Quick Comparison: Choose Your Weapon

| Characteristic | RestTemplate | WebClient.block() | RestClient |
|----------------|--------------|-------------------|------------|
| Maintenance | ❌ Deprecated | ⚠️ Anti-pattern | ✅ Active |
| Threads per request | 1 | 2 | 1 |
| Fluent API | ❌ | ✅ | ✅ |
| HTTP Interfaces | ❌ | ✅ | ✅ |
| Native Observability | ⚠️ Limited | ✅ | ✅ |
| Configuration complexity | Medium | High | Low |
| Performance | Good | Poor | Excellent |

---

## 🛑 The Anti-Pattern to Avoid: WebClient.block() — Why Two Threads Are Worse Than One

This is where things get serious... and a little ridiculous. If you're using **`WebClient`** and ending your call with **`.block()`** inside a traditional Spring MVC app, stop. Seriously.

### Why WebClient.block() Is Problematic

When you call `.block()` on a WebClient in a servlet-based application:

1. **Servlet Thread**: Occupies a thread from the Tomcat pool, waiting for the response
2. **Reactor Thread**: Creates an additional reactive thread to handle the I/O operation
3. **Result**: You consume **TWO threads** instead of one, drastically limiting scalability

**Real-World Impact Example:**
- Tomcat thread pool: 200 threads max
- With RestTemplate/RestClient: **200 concurrent requests** possible
- With WebClient.block(): **~100 concurrent requests** (because 2 threads per request)
- Under load: Thread starvation, increased latency, potential application hang

**RestClient** solves this problem by using a single thread with native non-blocking I/O, providing you with the elegant, modern, fluent API of `WebClient` while strictly enforcing the efficient **one-thread-per-request** model for synchronous operations.

---

## 🔄 Quick Migration Guide: From RestTemplate to RestClient

Before diving into advanced features, let's see how easy the migration is:

### RestTemplate Pattern

```java
// Old way - verbose and limited
ResponseEntity<Product> response = restTemplate.getForEntity(
    "https://api.example.com/products/{id}", 
    Product.class, 
    productId
);
Product product = response.getBody();

// POST with error handling
try {
    ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
        "https://api.example.com/process",
        request,
        ApiResponse.class
    );
    return response.getBody();
} catch (HttpClientErrorException e) {
    // Manual error handling
    log.error("Client error: {}", e.getStatusCode());
    throw new CustomException();
}
```

### RestClient Pattern

```java
// New way - fluent and declarative
Product product = restClient.get()
    .uri("/products/{id}", productId)
    .retrieve()
    .body(Product.class);

// POST with declarative error handling
return restClient.post()
    .uri("/process")
    .body(request)
    .retrieve()
    .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
        throw new CustomException("Client error: " + resp.getStatusCode());
    })
    .body(ApiResponse.class);
```

**Notice**: Cleaner syntax, better readability, and integrated error handling!

---

## 🗝️ RestClient Architecture: A Modern Bridge to Glory

The true elegance of **`RestClient`** is that it reuses the robust, pluggable infrastructure of **`WebClient`** for serialization, deserialization, and request processing, ensuring you get all the modern features without the reactive thread overhead.

### Architecture Diagram: RestClient in the Spring Ecosystem

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT 3.x APPLICATION                       │
├─────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  ┌────────────────────┐         ┌────────────────────┐              │
│  │   @RestController  │         │     @Service       │              │
│  │   (Your API)       │         │  (Business Logic)  │              │
│  └─────────┬──────────┘         └──────────┬─────────┘              │
│            │                               │                         │
│            │                               │ Uses                    │
│            │                               ▼                         │
│  ┌─────────▼──────────────────────────────────────────┐             │
│  │         HTTP CLIENT LAYER (Your Choice)            │             │
│  ├────────────────────────────────────────────────────┤             │
│  │                                                     │             │
│  │  ┌──────────────┐  ┌──────────────┐  ┌─────────┐  │             │
│  │  │ RestTemplate │  │  RestClient  │  │WebClient│  │             │
│  │  │  (Legacy)    │  │  ⭐ NEW ⭐   │  │(Reactive)│ │             │
│  │  └──────┬───────┘  └──────┬───────┘  └────┬────┘  │             │
│  │         │                 │                │       │             │
│  │         │ ❌ Deprecated   │ ✅ Recommended │       │             │
│  │         │                 │                │       │             │
│  └─────────┼─────────────────┼────────────────┼───────┘             │
│            │                 │                │                     │
│            │                 │                │                     │
│  ┌─────────▼─────────────────▼────────────────▼───────────────────┐ │
│  │              SHARED INFRASTRUCTURE LAYER                        │ │
│  ├─────────────────────────────────────────────────────────────────┤ │
│  │                                                                  │ │
│  │  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │ │
│  │  │ ExchangeFunction │  │  HTTP Codecs     │  │   Filters    │  │ │
│  │  │  (Core Engine)   │  │(Jackson/JSON/XML)│  │ (Interceptor)│  │ │
│  │  └──────────────────┘  └──────────────────┘  └──────────────┘  │ │
│  │                                                                  │ │
│  │  ┌──────────────────────────────────────────────────────────┐  │ │
│  │  │           HTTP Client Implementation                      │  │ │
│  │  │  • JDK HttpClient (Java 11+)                             │  │ │
│  │  │  • Apache HttpComponents                                 │  │ │
│  │  │  • Netty (for WebClient)                                 │  │ │
│  │  └──────────────────────────────────────────────────────────┘  │ │
│  │                                                                  │ │
│  └──────────────────────────────┬───────────────────────────────────┘ │
│                                 │                                   │
│  ┌──────────────────────────────▼───────────────────────────────┐  │
│  │              OBSERVABILITY LAYER (Micrometer)                 │  │
│  ├───────────────────────────────────────────────────────────────┤  │
│  │  • Automatic Metrics (http.client.requests)                   │  │
│  │  • Distributed Tracing (Trace ID, Span ID)                    │  │
│  │  • Spring Boot Actuator Integration                           │  │
│  └───────────────────────────────────────────────────────────────┘  │
│                                                                       │
└───────────────────────────────┬───────────────────────────────────────┘
                                │
                    ┌───────────▼────────────┐
                    │   External REST API    │
                    │  (Third-party Service) │
                    └────────────────────────┘


KEY INSIGHTS:
═══════════════════════════════════════════════════════════════════

┌─────────────────────────────────────────────────────────────────┐
│  RestTemplate          →  Standalone, legacy implementation     │
│  (Maintenance Mode)       ❌ Different codecs                   │
│                           ❌ No shared infrastructure            │
│                           ❌ Will be removed                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  RestClient            →  Synchronous wrapper over WebClient    │
│  (Recommended)            ✅ Reuses WebClient infrastructure    │
│                           ✅ One thread per request              │
│                           ✅ Modern API + Performance            │
│                           ✅ Native observability                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│  WebClient             →  For truly reactive applications       │
│  (Reactive)               ✅ Non-blocking, reactive streams     │
│                           ⚠️  .block() = anti-pattern in MVC    │
│                           ✅ Use only with WebFlux               │
└─────────────────────────────────────────────────────────────────┘
```

### Thread Model Comparison

```
┌──────────────────────────────────────────────────────────────────────┐
│                    REQUEST PROCESSING MODELS                          │
└──────────────────────────────────────────────────────────────────────┘

RestTemplate / RestClient (Efficient - 1 Thread):
─────────────────────────────────────────────────
┌─────────┐
│ Servlet │ ──┐
│ Thread  │   │ Makes HTTP call
│  Pool   │   │ (non-blocking I/O at network level)
└─────────┘   │ Waits synchronously
     ▲        │
     │        ▼
     └────────┘
   Single thread handling entire request lifecycle


WebClient.block() in MVC (Inefficient - 2 Threads):
───────────────────────────────────────────────────
┌─────────┐         ┌─────────┐
│ Servlet │ ──────► │ Reactor │
│ Thread  │  blocks │ Thread  │ ──► HTTP call
│  Pool   │ waiting │  Pool   │
└─────────┘         └─────────┘
     ▲                   │
     │                   │
     └───────────────────┘
   Servlet thread BLOCKED while Reactor thread works
   = Wasted resources, thread starvation under load


WebClient in WebFlux (Truly Reactive - Event Loop):
───────────────────────────────────────────────────
┌─────────┐
│  Event  │ ──► Non-blocking I/O ──► Event
│  Loop   │     (Callbacks/Reactive Chain)
│ Threads │
└─────────┘
   Small thread pool handles thousands of concurrent requests
   (Only use if your ENTIRE stack is reactive)
```

### Under the Hood: What RestClient Reuses from WebClient

RestClient leverages the mature WebClient infrastructure:

- **Codecs**: Same serialization/deserialization mechanisms (Jackson, JAXB, etc.)
- **ExchangeFunction**: Shared request/response processing pipeline
- **Filters**: Same `ExchangeFilterFunction` interface for interceptors
- **HTTP Client Connectors**: Pluggable HTTP client implementations
- **Error Handling**: Unified status code handling mechanisms
- **Efficient I/O**: Non-blocking network operations, synchronous API surface

---

## 🧙 Advantage #1: Declarative HTTP Interfaces — Write Less Code

This is the most powerful feature inherited from the WebFlux ecosystem. By using **Declarative HTTP Interfaces** (or **`@HttpExchange`**), you define a simple Java interface, and Spring automatically generates the underlying `RestClient` implementation. This is the **end of repetitive client implementation code**.

📗 **Further Reading:**
- [HTTP Interface Documentation](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface)

### Code Example: Declaring Your API Client

```java
// 1. Define the Interface (The Contract)
public interface ExternalProductService {

    @GetExchange("/products/{id}")
    Product findProductById(@PathVariable String id);
    
    @PostExchange("/products")
    Product createProduct(@RequestBody Product product);
    
    @PutExchange("/products/{id}")
    void updateProduct(@PathVariable String id, @RequestBody Product product);
    
    @DeleteExchange("/products/{id}")
    void deleteProduct(@PathVariable String id);
}

// 2. Instantiate the Client in Configuration
@Configuration
class HttpClientConfig {
    
    @Bean
    ExternalProductService externalProductService(RestClient.Builder builder) {
        RestClient restClient = builder
            .baseUrl("https://api.external.com")
            .build();
            
        // HttpServiceProxyFactory creates the proxy implementation
        RestClientAdapter adapter = RestClientAdapter.create(restClient);
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
            .builderFor(adapter)
            .build();
            
        return factory.createClient(ExternalProductService.class);
    }
}

// 3. Use it like any Spring Bean
@Service
public class ProductService {
    
    private final ExternalProductService externalProductService;
    
    // Constructor injection
    public ProductService(ExternalProductService externalProductService) {
        this.externalProductService = externalProductService;
    }
    
    public Product getProduct(String id) {
        // No implementation needed - Spring handles everything!
        return externalProductService.findProductById(id);
    }
}
```

---

## 🔐 Advantage #2: Elegant and Centralized Security

Using **`RestClient.Builder`** allows you to define core policies like authentication headers in a single location (your configuration file), instead of scattering them throughout your service code or hiding them in complex interceptors.

### Code Example: Configuring the Secured RestClient

```java
@Configuration
public class RestClientConfig {
    
    @Value("${external.api.token}")
    private String apiToken;
    
    @Bean
    public RestClient securedApiRestClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder
            .baseUrl("https://api.external-service.com/v1")
            
            // --- Core of Centralized Security ---
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            
            // Add common headers for all requests
            .defaultHeader("X-API-Version", "1.0")
            .defaultHeader("X-Client-Id", "my-spring-app")
            
            .build();
    }
}
```

**Benefits:**
- ✅ Security configuration in one place
- ✅ Easy to update credentials
- ✅ Testable (mock the RestClient bean)
- ✅ No security logic scattered in services

---

## 📊 Advantage #3: Native Observability (Logs & Metrics)

For serious production systems, this is the killer feature. **`RestClient`** is natively designed for the modern **Micrometer Tracing and Metrics** ecosystem of Spring Boot 3.x.

When you include `spring-boot-starter-actuator` and a tracing bridge (e.g., Brave for Zipkin or OpenTelemetry), every single `RestClient` call is **automatically instrumented** with **Tracing** (Trace ID, Span ID) and **Time-series Metrics** (latency, HTTP status, URI). This eliminates the manual work of writing custom interceptors just to get basic metrics.

### What You Get Automatically

With Actuator + Micrometer configured, RestClient provides:

**Metrics (Prometheus format):**
```properties
# Request count by method, URI, status
http_client_requests_seconds_count{method="GET",uri="/products/{id}",status="200",outcome="SUCCESS"} 42

# Total request duration
http_client_requests_seconds_sum{method="GET",uri="/products/{id}",status="200",outcome="SUCCESS"} 1.234

# Max request duration
http_client_requests_seconds_max{method="GET",uri="/products/{id}",status="200",outcome="SUCCESS"} 0.156
```

**Distributed Tracing:**
- Automatic span creation: `HTTP GET /products/{id}`
- Trace ID and Span ID propagation via headers
- Parent-child relationship with incoming requests
- Full request/response timing

**Example Log Output with Tracing:**
```
2024-10-11 10:15:23.456 INFO [my-app,a1b2c3d4e5f6,a1b2c3d4e5f6] ExternalService : Calling external API
2024-10-11 10:15:23.789 INFO [my-app,a1b2c3d4e5f6,f7e8d9c0b1a2] RestClient : HTTP GET /products/123
2024-10-11 10:15:23.945 INFO [my-app,a1b2c3d4e5f6,f7e8d9c0b1a2] RestClient : Response: 200 OK (156ms)
```

**Configuration Example:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
  tracing:
    sampling:
      probability: 1.0  # 100% sampling for demo (use 0.1 in prod)
```

---

## 🛡️ Advantage #4: Advanced Configuration — Client Resilience

In a microservices world, you must protect your service from slow or failing external services. `RestClient` makes configuring resilience much cleaner than `RestTemplate`.

📗 **Further Reading:**
- [Integrating Resilience4j with Spring Boot](https://resilience4j.readme.io/docs/getting-started-3)

### Connection and Read Timeouts

Prevent threads from being permanently blocked by a hanging service by setting explicit timeouts.

#### Option 1: Using JDK HttpClient (Java 11+)

```java
@Bean
public RestClient resilientRestClient(RestClient.Builder builder) {
    JdkClientHttpRequestFactory requestFactory = 
        new JdkClientHttpRequestFactory(httpClient());
    
    return builder
        .requestFactory(requestFactory)
        .build();
}

private HttpClient httpClient() {
    return HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        // Note: JDK HttpClient doesn't have a global read timeout
        // Set it per-request or use Apache HttpComponents
        .build();
}
```

#### Option 2: Using Apache HttpComponents (Recommended for Full Control)

```java
@Bean
public RestClient resilientRestClient(RestClient.Builder builder) {
    HttpComponentsClientHttpRequestFactory requestFactory = 
        new HttpComponentsClientHttpRequestFactory();
    
    // Connection timeout: time to establish connection
    requestFactory.setConnectTimeout(3000);
    
    // Read timeout: time to wait for data after connection established
    requestFactory.setConnectionRequestTimeout(3000);
    
    return builder
        .baseUrl("https://api.external-service.com")
        .requestFactory(requestFactory)
        .build();
}
```

**Add this dependency for Apache HttpComponents:**
```xml
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
</dependency>
```

### Integration with Resilience4j

For advanced patterns like retry, circuit breaker, and rate limiting:

```java
@Service
public class ResilientExternalService {
    
    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    
    public ResilientExternalService(RestClient restClient, 
                                   CircuitBreakerRegistry circuitBreakerRegistry,
                                   RetryRegistry retryRegistry) {
        this.restClient = restClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("externalApi");
        this.retry = retryRegistry.retry("externalApi");
    }
    
    public Product getProduct(String id) {
        // Combine retry + circuit breaker
        return Retry.decorateSupplier(retry,
            CircuitBreaker.decorateSupplier(circuitBreaker,
                () -> restClient.get()
                    .uri("/products/{id}", id)
                    .retrieve()
                    .body(Product.class)
            )
        ).get();
    }
}
```

---

## 🧩 Advantage #5: Exchange Filters — Replacing Old Interceptors

The **`ExchangeFilterFunction`** is the modern replacement for the old `ClientHttpRequestInterceptor`. It gives you full control over both the **outgoing Request** and the **incoming Response**.

📗 **Further Reading:**
- [Exchange Filters Documentation](https://docs.spring.io/spring-framework/reference/web/webflux-webclient/client-filter.html)

### Code Example: Centralized Logging Filter

This filter can be applied once in the builder to log every single request and response, centralizing cross-cutting concerns.

```java
@Bean
public RestClient loggingRestClient(RestClient.Builder builder) {
    return builder
        .baseUrl("https://api.example.com")
        .filter((request, next) -> {
            log.info("→ Outgoing Request: {} {}", request.method(), request.url());
            long startTime = System.currentTimeMillis();
            
            // Execute the request
            ClientResponse response = next.exchange(request);
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("← Incoming Response: {} ({}ms)", 
                response.statusCode(), duration);
            
            return response;
        })
        .build();
}
```

### Advanced Filter: Request/Response Body Logging

```java
@Bean
public RestClient detailedLoggingRestClient(RestClient.Builder builder) {
    return builder
        .baseUrl("https://api.example.com")
        .filter((request, next) -> {
            // Log request details
            log.debug("Request Headers: {}", request.headers());
            
            // Execute request
            ClientResponse response = next.exchange(request);
            
            // Log response body (careful with large responses)
            if (log.isDebugEnabled() && response.statusCode().is2xxSuccessful()) {
                String body = response.bodyTo(String.class);
                log.debug("Response Body: {}", body);
                
                // Important: Recreate response with body for downstream processing
                return ClientResponse.create(response.statusCode())
                    .headers(headers -> headers.addAll(response.headers().asHttpHeaders()))
                    .body(body)
                    .build();
            }
            
            return response;
        })
        .build();
}
```

---

## ⚙️ Advantage #6: Flexible and Dynamic Header Configuration

Need to add a header based on runtime data? The fluent **`.headers()`** method accepts a **Lambda/Consumer**, giving you instant access to the `HttpHeaders` object to inject dynamic or conditional values.

### Dynamic and Conditional Headers Example

```java
@Service
public class ExternalService {
    
    private final RestClient securedApiRestClient;
    private final CorrelationIdService contextService;
    
    public ExternalService(RestClient securedApiRestClient, 
                          CorrelationIdService contextService) {
        this.securedApiRestClient = securedApiRestClient;
        this.contextService = contextService;
    }
    
    public ApiResponse processRequest(ApiRequest request) {
        String userId = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
        
        return securedApiRestClient.post()
            .uri("/process")
            .body(request)
            // Dynamic headers based on runtime context
            .headers(headers -> {
                headers.set("X-Correlation-ID", contextService.getCorrelationId());
                headers.set("X-User-ID", userId);
                headers.set("X-Request-Time", Instant.now().toString());
                
                // Conditional header
                if (request.isPriority()) {
                    headers.set("X-Priority", "HIGH");
                }
            })
            .retrieve()
            .body(ApiResponse.class);
    }
}
```

---

## 🚀 Advantage #7: Clean Code and Declarative Error Handling

The request chain is clean and readable, clearly stating *what* it is doing rather than *how*.

### Final Code Example: Complete Service Implementation

```java
@Service
@Slf4j
public class ExternalService {

    private final RestClient securedApiRestClient;
    
    public ExternalService(RestClient securedApiRestClient) {
        this.securedApiRestClient = securedApiRestClient;
    }

    public ApiResponse processRequest(ApiRequest request) {
        
        return securedApiRestClient.post()
            .uri("/process")
            .body(request)
            .retrieve()
            
            // --- Declarative Error Handling is Beautiful ---
            .onStatus(HttpStatusCode::is4xxClientError, (req, resp) -> {
                // IMPORTANT: bodyTo() consumes the response body
                // Store it if you need to use it multiple times
                String errorBody = resp.bodyTo(String.class);
                log.error("Client Error: {} | Body: {}", 
                    resp.statusCode(), errorBody);
                throw new CustomClientException(
                    "Invalid or unauthorized request: " + errorBody);
            })
            
            .onStatus(HttpStatusCode::is5xxServerError, (req, resp) -> {
                log.error("Server Error: {} | Message: {}", 
                    resp.statusCode(), 
                    resp.bodyTo(String.class));
                throw new RemoteServiceDownException(
                    "External service is unavailable");
            })
            
            // Specific status code handling
            .onStatus(status -> status.value() == 429, (req, resp) -> {
                log.warn("Rate limit exceeded");
                throw new RateLimitException("Too many requests");
            })
            
            .body(ApiResponse.class);
    }
    
    public List<Product> getAllProducts() {
        // Generic type handling with ParameterizedTypeReference
        return securedApiRestClient.get()
            .uri("/products")
            .retrieve()
            .body(new ParameterizedTypeReference<List<Product>>() {});
    }
    
    public Optional<Product> getProductSafely(String id) {
        try {
            Product product = securedApiRestClient.get()
                .uri("/products/{id}", id)
                .retrieve()
                .onStatus(status -> status.value() == 404, 
                    (req, resp) -> {
                        // Don't throw, just log
                        log.info("Product not found: {}", id);
                    })
                .body(Product.class);
            return Optional.ofNullable(product);
        } catch (Exception e) {
            log.error("Error fetching product: {}", id, e);
            return Optional.empty();
        }
    }
}
```

---

## 📦 Appendix: Required Maven Configuration (`pom.xml`)

To use **RestClient**, **Declarative HTTP Interfaces**, and **Actuator** for observability, you need the following dependencies in your Spring Boot 3.x or later project.

**IMPORTANT NOTE**: `spring-boot-starter-webflux` does **NOT** activate full reactive mode. It only provides the infrastructure (`WebClient`, codecs, `HttpServiceProxyFactory`) that `RestClient` is built upon. Your application remains MVC/Servlet-based as long as you have `spring-boot-starter-web`.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>restclient-demo</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <java.version>17</java.version>
        <resilience4j.version>2.2.0</resilience4j.version>
    </properties>

    <dependencies>

        <!-- Core Spring Boot Web (Servlet-based) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- WebFlux infrastructure for RestClient (does NOT enable reactive mode) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <!-- Observability: Metrics, Health, Tracing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <!-- Optional: Micrometer Tracing Bridge (choose one) -->
        <!-- For Zipkin/Brave -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
        </dependency>
        
        <!-- For OpenTelemetry (alternative to Brave) -->
        <!--
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-otel</artifactId>
        </dependency>
        -->

        <!-- Optional: Prometheus metrics endpoint -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>

        <!-- Optional: Apache HttpComponents for better timeout control -->
        <dependency>
            <groupId>org.apache.httpcomponents.client5</groupId>
            <artifactId>httpclient5</artifactId>
        </dependency>

        <!-- Optional: Resilience4j for circuit breaker, retry, etc. -->
        <dependency>
            <groupId>io.github.resilience4j</groupId>
            <artifactId>resilience4j-spring-boot3</artifactId>
            <version>${resilience4j.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>

        <!-- Lombok (optional, for cleaner code) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

### Minimal Configuration for Basic Usage

If you just want to use RestClient without observability or resilience:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
</dependencies>
```

---

## 🎯 Conclusion

The adoption of **`RestClient`** is not just about a "new tool"; it's a critical step toward a **cleaner, more secure, and more observable** codebase. It brings the power of the `WebClient` architecture and features (like **Declarative HTTP Interfaces** and **Exchange Filters**) to the synchronous world, all while strictly adhering to the efficient **one-thread-per-request** model.

### Key Takeaways

✅ **RestTemplate is deprecated** — migrate now to avoid technical debt  
✅ **WebClient.block() is an anti-pattern** — wastes threads and hurts scalability  
✅ **RestClient is the future** — modern, efficient, and fully supported  
✅ **Native observability** — get metrics and tracing without custom code  
✅ **Declarative HTTP Interfaces** — eliminate boilerplate client code  
✅ **Centralized configuration** — security, timeouts, and filters in one place  

If your project is on Spring Boot 3.x, the choice is clear: **`RestClient`** is the only logical choice. Stop wasting threads and start writing beautiful client code!

---

## 📚 Additional Resources

- [Official RestClient Documentation](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-restclient)
- [HTTP Interface Documentation](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html#rest-http-interface)
- [Spring Boot 3.2 Release Notes](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.2-Release-Notes)
- [Micrometer Observation Documentation](https://micrometer.io/docs/observation)
- [Resilience4j Documentation](https://resilience4j.readme.io/)

---

**Happy coding! 🚀**
