## Dynamic Logging Control via Actuator

### Built-in Actuator Endpoints

**Get Current Logging Configuration:**
```bash
curl http://localhost:8080/actuator/webclient-logging
```

**Update Logging Configuration:**
```bash
# Disable all WebClient logging
curl -X POST http://localhost:8080/actuator/webclient-logging \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'

# Enable logging with specific settings
curl -X POST http://localhost:8080/actuator/webclient-logging \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "includeHeaders": true,
    "includeBody": false,
    "maxBodySize": 500,
    "includeParameters": true,
    "maskSensitiveData": true
  }'
```

### REST API for Logging Management

**Check Current Status:**
```bash
curl http://localhost:8080/api/logging/status
```

**Toggle Logging On/Off:**
```bash
# Toggle logging state
curl -X POST http://localhost:8080/api/logging/toggle

# Explicitly enable logging
curl -X POST "http://localhost:8080/api/logging/toggle?enabled=true"

# Explicitly disable logging
curl -X POST "http://localhost:8080/api/logging/toggle?enabled=false"
```

**Control Specific Features:**
```bash
# Enable/disable header logging
curl -X POST "http://localhost:8080/api/logging/headers?enabled=true"

# Enable/disable body logging
curl -X POST "http://localhost:8080/api/logging/body?enabled=false"

# Set maximum body size for logging
curl -X POST "http://localhost:8080/api/logging/body-size?size=2000"
```

**Test with Current Settings:**
```bash
curl http://localhost:8080/api/logging/test-with-current-settings
```

### Standard Actuator Logger Control

You can also control logging levels using the standard loggers endpoint:

```bash
### Standard Actuator Logger Control

You can also control logging levels using the standard loggers endpoint:

```bash
# Get current logger levels
curl http://localhost:8080/actuator/loggers

# Get specific logger level
curl http://localhost:8080/actuator/loggers/com.example.webclientlogging.logging.WebClientLoggingFilter

# Change logger level to DEBUG
curl -X POST http://localhost:8080/actuator/loggers/com.example.webclientlogging.logging.WebClientLoggingFilter \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'

# Disable specific logger
curl -X POST http://localhost:8080/actuator/loggers/com.example.webclientlogging.logging.WebClientLoggingFilter \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "OFF"}'

# Reset logger to default (remove explicit configuration)
curl -X POST http://localhost:8080/actuator/loggers/com.example.webclientlogging.logging.WebClientLoggingFilter \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": null}'
```

### Monitoring Endpoints
- `http://localhost:8080/actuator/health` - Health check
- `http://localhost:8080/actuator/metrics` - Metrics
- `http://localhost:8080/actuator/env` - Environment properties
- `http://localhost:8080/actuator/loggers` - Logger configuration
- `http://localhost:8080/actuator/webclient-logging` - Custom WebClient logging endpoint

## Configuration Options

### WebClient Timeouts
```yaml
webclient:
  timeout:
    connection: 5000    # Connection timeout in ms
    response: 10000     # Response timeout in ms
```

### Dynamic Logging Configuration
```yaml
webclient:
  logging:
    enabled: true               # Enable/disable custom logging
    include-headers: true       # Include headers in logs
    include-body: true          # Include body in logs
    max-body-size: 1000        # Max body size to log
    include-parameters: true    # Include URL parameters in logs
    mask-sensitive-data: true   # Mask sensitive headers/data
```

### Memory Configuration
```yaml
webclient:
  max-in-memory-size: 1048576  # Max in-memory buffer size (1MB)
```

## Real-time Logging Control Examples

### Scenario 1: Debugging Mode
```bash
# Enable full logging for debugging
curl -X POST http://localhost:8080/actuator/webclient-logging \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "includeHeaders": true,
    "includeBody": true,
    "includeParameters": true,
    "maxBodySize": 5000,
    "maskSensitiveData": false
  }'

# Test the configuration
curl http://localhost:8080/api/test/complex-body-demo
```

### Scenario 2: Production Mode (Minimal Logging)
```bash
# Reduce logging for production
curl -X POST http://localhost:8080/actuator/webclient-logging \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "includeHeaders": false,
    "includeBody": false,
    "includeParameters": true,
    "maxBodySize": 100,
    "maskSensitiveData": true
  }'
```

### Scenario 3: Complete Disable
```bash
# Completely disable WebClient logging
curl -X POST "http://localhost:8080/api/logging/toggle?enabled=false"

# Verify no logs are generated
curl http://localhost:8080/api/test/demo
```

### Scenario 4: Headers Only
```bash
# Log only headers and parameters
curl -X POST http://localhost:8080/actuator/webclient-logging \
  -H "Content-Type: application/json" \
  -d '{
    "enabled": true,
    "includeHeaders": true,
    "includeBody": false,
    "includeParameters": true,
    "maskSensitiveData": true
  }'
```

## Testing the Application

### Prerequisites
- Java 17+
- Maven 3.6+

### Build and Run

```bash
# Clone the repository
git clone <repository-url>
cd webclient-logging-example

# Build the project
mvn clean compile

# Run tests
mvn test

# Run the application
mvn spring-boot:run
```

### Testing Dynamic Configuration

```bash
# 1. Check initial logging status
curl http://localhost:8080/api/logging/status

# 2. Test with default settings
curl http://localhost:8080/api/test/body-demo

# 3. Disable body logging
curl -X POST "http://localhost:8080/api/logging/body?enabled=false"

# 4. Test again (should not see body in logs)
curl http://localhost:8080/api/test/body-demo

# 5. Completely disable logging
curl -X POST "http://localhost:8080/api/logging/toggle?enabled=false"

# 6. Test (should see no WebClient logs)
curl http://localhost:8080/api/test/demo

# 7. Re-enable with custom settings via Actuator
curl -X POST http://localhost:8080/actuator/webclient-logging \
  -H "Content-Type: application/json" \
  -d '{"enabled": true, "includeHeaders": true, "includeBody": true, "maxBodySize": 2000}'

# 8. Test with new settings
curl http://localhost:8080/api/logging/test-with-current-settings
```

### Testing Different Scenarios

```bash
# Get all users
curl http://localhost:8080/api/users

# Get specific user
curl http://localhost:8080/api/users/1

# Create a new user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","username":"johndoe","email":"john@example.com"}'

# Run demo with request body logging
curl http://localhost:8080/api/test/body-demo

# Run demo with complex request body
curl http://localhost:8080/api/test/complex-body-demo

# Run demo with URL parameters
curl http://localhost:8080/api/test/params-advanced-demo

# Run error demo
curl http://localhost:8080/api/test/error-demo
```

## Monitoring and Logs

### Log Files
- `logs/webclient.log` - WebClient specific logs
- `logs/error.log` - Error logs only
- Console output with colored formatting

### Actuator Endpoints
- `http://localhost:8080/actuator/health` - Health check
- `http://localhost:8080/actuator/metrics` - Metrics
- `http://localhost:8080/actuator/webclient-logging` - Custom logging control
- `http://localhost:8080/actuator/loggers` - Standard logger control

## Benefits of Dynamic Logging Control

1. **Runtime Configuration**: Change logging behavior without restart
2. **Performance Optimization**: Disable expensive logging in production
3. **Debugging Support**: Enable detailed logging when troubleshooting
4. **Selective Logging**: Choose what to log (headers, body, parameters)
5. **Security Control**: Enable/disable sensitive data masking
6. **Resource Management**: Control log size and memory usage

## Best Practices Implemented

1. **Reactive Programming**: Full reactive stack with Mono/Flux
2. **Resource Management**: Proper connection pooling and timeouts
3. **Security**: Sensitive data masking in logs (configurable)
4. **Performance**: Async logging and memory limits
5. **Resilience**: Retry mechanisms and circuit breaker patterns
6. **Observability**: Comprehensive logging and monitoring with runtime control
7. **Testing**: Unit tests with MockWebServer
8. **Configuration**: Externalized configuration with runtime updates
9. **Management**: Actuator endpoints for operational control

## Dependencies

Key dependencies used:
- `spring-boot-starter-webflux` - WebFlux and WebClient
- `reactor-netty-http` - Netty HTTP client
- `spring-boot-starter-actuator` - Monitoring and management
- `logback-classic` - Logging framework
- `mockwebserver` (test) - HTTP client testing

This example provides a production-ready template for using Spring WebClient with comprehensive logging and error handling capabilities, including full runtime control over logging behavior through Actuator endpoints.# Spring Boot 3 WebClient with Netty Logging Example

This project demonstrates a comprehensive Spring Boot 3 application using WebClient with Netty for HTTP client operations, featuring detailed request/response logging and error handling.

## Features

- **Spring Boot 3** with WebFlux
- **WebClient** with Netty HTTP client
- **Custom Logging Filter** for requests/responses with headers and body
- **Comprehensive Error Handling** with custom exceptions
- **Retry Mechanism** for resilient HTTP calls
- **Wire Logging** at Netty level
- **Security-aware** header masking
- **Unit Tests** with MockWebServer
- **Configurable Logging** levels and formats

## Project Structure

```
src/
├── main/
│   ├── java/com/example/webclientlogging/
│   │   ├── WebClientLoggingApplication.java      # Main application
│   │   ├── config/
│   │   │   └── WebClientConfig.java              # WebClient configuration
│   │   ├── controller/
│   │   │   ├── UserController.java               # REST endpoints
│   │   │   └── TestController.java               # Demo endpoints
│   │   ├── dto/
│   │   │   └── User.java                         # User DTO
│   │   ├── exception/
│   │   │   ├── UserNotFoundException.java        # Custom exceptions
│   │   │   ├── WebClientException.java
│   │   │   └── GlobalExceptionHandler.java       # Global error handler
│   │   ├── logging/
│   │   │   └── WebClientLoggingFilter.java       # Custom logging filter
│   │   └── service/
│   │       └── UserService.java                  # Business logic
│   └── resources/
│       ├── application.yml                       # Configuration
│       └── logback-spring.xml                    # Logging configuration
└── test/
    └── java/com/example/webclientlogging/
        └── service/
            └── UserServiceTest.java               # Unit tests
```

## Configuration

### WebClient Configuration (`WebClientConfig.java`)

- **Netty HTTP Client** with custom timeouts
- **Connection pooling** and timeout handlers
- **Wire tap logging** enabled
- **Memory buffer size** configuration
- **Custom logging filter** integration

### Logging Configuration (`application.yml`)

```yaml
webclient:
  base-url: https://jsonplaceholder.typicode.com
  timeout:
    connection: 5000
    response: 10000
  max-in-memory-size: 1048576
  logging:
    enabled: true
    include-headers: true
    include-body: true
    max-body-size: 1000
```

## Custom Logging Features

### WebClientLoggingFilter

The custom logging filter provides:

- **Request ID** generation for tracing
- **Timestamp** tracking with execution duration
- **Headers logging** with sensitive data masking
- **Body logging** with size truncation
- **Error logging** with detailed stack traces
- **Configurable** logging levels and content

### Security Features

- **Sensitive headers masking**: Authorization, Cookie, tokens
- **Body size limitation** to prevent memory issues
- **Configurable logging levels** for different environments

### Log Output Example

```
2024-01-15 10:30:15.123 [reactor-http-nio-2] INFO  WebClientLoggingFilter - === REQUEST a1b2c3d4 [2024-01-15 10:30:15.123] ===
2024-01-15 10:30:15.124 [reactor-http-nio-2] INFO  WebClientLoggingFilter - Method: POST https://jsonplaceholder.typicode.com/users
2024-01-15 10:30:15.124 [reactor-http-nio-2] INFO  WebClientLoggingFilter - Request Path [a1b2c3d4]: /users
2024-01-15 10:30:15.124 [reactor-http-nio-2] INFO  WebClientLoggingFilter - Request Headers:
2024-01-15 10:30:15.124 [reactor-http-nio-2] INFO  WebClientLoggingFilter -   Accept: application/json
2024-01-15 10:30:15.124 [reactor-http-nio-2] INFO  WebClientLoggingFilter -   Content-Type: application/json
2024-01-15 10:30:15.124 [reactor-http-nio-2] INFO  WebClientLoggingFilter -   User-Agent: ReactorNetty/1.1.13
2024-01-15 10:30:15.125 [reactor-http-nio-2] INFO  WebClientLoggingFilter - Request Body [a1b2c3d4]: {"name":"Jane Doe","username":"janedoe","email":"jane.doe@example.com","phone":"555-1234","website":"www.janedoe.com"}
2024-01-15 10:30:15.345 [reactor-http-nio-2] INFO  WebClientLoggingFilter - === RESPONSE a1b2c3d4 [2024-01-15 10:30:15.345] (221 ms) ===
2024-01-15 10:30:15.345 [reactor-http-nio-2] INFO  WebClientLoggingFilter - Status: 201 Created
2024-01-15 10:30:15.345 [reactor-http-nio-2] INFO  WebClientLoggingFilter - Response Headers:
2024-01-15 10:30:15.346 [reactor-http-nio-2] INFO  WebClientLoggingFilter -   Content-Type: application/json; charset=utf-8
2024-01-15 10:30:15.346 [reactor-http-nio-2] INFO  WebClientLoggingFilter - Response Body [a1b2c3d4]: {"id":11,"name":"Jane Doe","username":"janedoe"...}

# URL Parameters Example
2024-01-15 10:35:20.123 [reactor-http-nio-3] INFO  WebClientLoggingFilter - === REQUEST b2c3d4e5 [2024-01-15 10:35:20.123] ===
2024-01-15 10:35:20.124 [reactor-http-nio-3] INFO  WebClientLoggingFilter - Method: GET https://jsonplaceholder.typicode.com/users?_page=1&_limit=3
2024-01-15 10:35:20.124 [reactor-http-nio-3] INFO  WebClientLoggingFilter - Request Parameters [b2c3d4e5]: _page=1&_limit=3
2024-01-15 10:35:20.124 [reactor-http-nio-3] INFO  WebClientLoggingFilter - Request Path [b2c3d4e5]: /users
```

## API Endpoints

### User Management
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Demo Endpoints
- `GET /api/test/demo` - Run a demo showing successful requests
- `GET /api/test/error-demo` - Run error demo (404 handling)
- `GET /api/test/params-demo` - Demo URL parameter logging
- `GET /api/test/body-demo` - Demo request body logging
- `GET /api/test/complex-body-demo` - Demo complex request body with metadata
- `GET /api/test/params-advanced-demo` - Demo advanced URL parameters with pagination
- `GET /api/test/health` - Health check

## Error Handling

### Custom Exceptions
- **UserNotFoundException**: For 404 errors
- **WebClientException**: For HTTP client errors

### Global Exception Handler
- Centralized error handling with consistent response format
- Detailed error logging
- Different HTTP status codes for different error types

### Retry Mechanism
- **Exponential backoff** retry strategy
- **Retryable conditions**: 5xx errors, timeouts, connection errors
- **Configurable retry attempts** (default: 3)

## Running the Application

### Prerequisites
- Java 17+
- Maven 3.6+

### Build and Run

```bash
# Clone the repository
git clone <repository-url>
cd webclient-logging-example

# Build the project
mvn clean compile

# Run tests
mvn test

# Run the application
mvn spring-boot:run
```

### Testing the Application

```bash
# Get all users
curl http://localhost:8080/api/users

# Get specific user
curl http://localhost:8080/api/users/1

# Create a new user
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","username":"johndoe","email":"john@example.com"}'

# Run demo with request body logging
curl http://localhost:8080/api/test/body-demo

# Run demo with complex request body
curl http://localhost:8080/api/test/complex-body-demo

# Run demo with URL parameters
curl http://localhost:8080/api/test/params-advanced-demo

# Run error demo
curl http://localhost:8080/api/test/error-demo
```

## Monitoring and Logs

### Log Files
- `logs/webclient.log` - WebClient specific logs
- `logs/error.log` - Error logs only
- Console output with colored formatting

### Actuator Endpoints
- `http://localhost:8080/actuator/health` - Health check
- `http://localhost:8080/actuator/metrics` - Metrics

## Configuration Options

### WebClient Timeouts
```yaml
webclient:
  timeout:
    connection: 5000    # Connection timeout in ms
    response: 10000     # Response timeout in ms
```

### Logging Configuration
```yaml
webclient:
  logging:
    enabled: true           # Enable/disable custom logging
    include-headers: true   # Include headers in logs
    include-body: true      # Include body in logs
    max-body-size: 1000    # Max body size to log
```

### Memory Configuration
```yaml
webclient:
  max-in-memory-size: 1048576  # Max in-memory buffer size (1MB)
```

## Best Practices Implemented

1. **Reactive Programming**: Full reactive stack with Mono/Flux
2. **Resource Management**: Proper connection pooling and timeouts
3. **Security**: Sensitive data masking in logs
4. **Performance**: Async logging and memory limits
5. **Resilience**: Retry mechanisms and circuit breaker patterns
6. **Observability**: Comprehensive logging and monitoring
7. **Testing**: Unit tests with MockWebServer
8. **Configuration**: Externalized configuration with profiles

## Dependencies

Key dependencies used:
- `spring-boot-starter-webflux` - WebFlux and WebClient
- `reactor-netty-http` - Netty HTTP client
- `spring-boot-starter-actuator` - Monitoring
- `logback-classic` - Logging framework
- `mockwebserver` (test) - HTTP client testing

This example provides a production-ready template for using Spring WebClient with comprehensive logging and error handling capabilities.