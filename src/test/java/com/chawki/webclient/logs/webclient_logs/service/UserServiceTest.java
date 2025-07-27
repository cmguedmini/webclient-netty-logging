//package com.chawki.webclient.logs.webclient_logs.service;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import okhttp3.mockwebserver.MockResponse;
//import okhttp3.mockwebserver.MockWebServer;
//import okhttp3.mockwebserver.RecordedRequest;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Flux;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class UserServiceTest {
//
//    private MockWebServer mockWebServer;
//    private UserService userService;
//    private ObjectMapper objectMapper;
//
//    @BeforeEach
//    void setUp() throws IOException {
//        mockWebServer = new MockWebServer();
//        mockWebServer.start();
//
//        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());
//        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();
//        userService = new UserService(webClient);
//        objectMapper = new ObjectMapper();
//    }
//
//    @AfterEach
//    void tearDown() throws IOException {
//        mockWebServer.shutdown();
//    }
//
//    @Test
//    void getAllUsers_ShouldReturnUsers_WhenSuccessful() throws JsonProcessingException {
//        // Given
//        List<User> users = Arrays.asList(
//                createUser(1L, "John Doe", "johndoe", "john@example.com"),
//                createUser(2L, "Jane Smith", "janesmith", "jane@example.com")
//        );
//        
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                .setBody(objectMapper.writeValueAsString(users)));
//
//        // When
//        Flux<User> result = userService.getAllUsers();
//
//        // Then
//        StepVerifier.create(result)
//                .expectNextMatches(user -> user.getId().equals(1L) && "John Doe".equals(user.getName()))
//                .expectNextMatches(user -> user.getId().equals(2L) && "Jane Smith".equals(user.getName()))
//                .verifyComplete();
//    }
//
//    @Test
//    void getUserById_ShouldReturnUser_WhenUserExists() throws JsonProcessingException, InterruptedException {
//        // Given
//        User user = createUser(1L, "John Doe", "johndoe", "john@example.com");
//        
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                .setBody(objectMapper.writeValueAsString(user)));
//
//        // When
//        Mono<User> result = userService.getUserById(1L);
//
//        // Then
//        StepVerifier.create(result)
//                .expectNextMatches(u -> u.getId().equals(1L) && "John Doe".equals(u.getName()))
//                .verifyComplete();
//
//        // Verify request
//        RecordedRequest request = mockWebServer.takeRequest();
//        assertEquals("GET", request.getMethod());
//        assertEquals("/users/1", request.getPath());
//    }
//
//    @Test
//    void getUserById_ShouldThrowUserNotFoundException_WhenUserNotFound() {
//        // Given
//        mockWebServer.enqueue(new MockResponse().setResponseCode(404));
//
//        // When
//        Mono<User> result = userService.getUserById(999L);
//
//        // Then
//        StepVerifier.create(result)
//                .expectError(UserNotFoundException.class)
//                .verify();
//    }
//
//    @Test
//    void createUser_ShouldReturnCreatedUser_WhenSuccessful() throws JsonProcessingException, InterruptedException {
//        // Given
//        User inputUser = createUser(null, "New User", "newuser", "new@example.com");
//        User createdUser = createUser(1L, "New User", "newuser", "new@example.com");
//        
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(201)
//                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                .setBody(objectMapper.writeValueAsString(createdUser)));
//
//        // When
//        Mono<User> result = userService.createUser(inputUser);
//
//        // Then
//        StepVerifier.create(result)
//                .expectNextMatches(user -> user.getId().equals(1L) && "New User".equals(user.getName()))
//                .verifyComplete();
//
//        // Verify request
//        RecordedRequest request = mockWebServer.takeRequest();
//        assertEquals("POST", request.getMethod());
//        assertEquals("/users", request.getPath());
//        assertEquals(MediaType.APPLICATION_JSON_VALUE, request.getHeader(HttpHeaders.CONTENT_TYPE));
//    }
//
//    @Test
//    void updateUser_ShouldReturnUpdatedUser_WhenSuccessful() throws JsonProcessingException, InterruptedException {
//        // Given
//        User inputUser = createUser(null, "Updated User", "updateduser", "updated@example.com");
//        User updatedUser = createUser(1L, "Updated User", "updateduser", "updated@example.com");
//        
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                .setBody(objectMapper.writeValueAsString(updatedUser)));
//
//        // When
//        Mono<User> result = userService.updateUser(1L, inputUser);
//
//        // Then
//        StepVerifier.create(result)
//                .expectNextMatches(user -> user.getId().equals(1L) && "Updated User".equals(user.getName()))
//                .verifyComplete();
//
//        // Verify request
//        RecordedRequest request = mockWebServer.takeRequest();
//        assertEquals("PUT", request.getMethod());
//        assertEquals("/users/1", request.getPath());
//    }
//
//    @Test
//    void deleteUser_ShouldComplete_WhenSuccessful() throws InterruptedException {
//        // Given
//        mockWebServer.enqueue(new MockResponse().setResponseCode(204));
//
//        // When
//        Mono<Void> result = userService.deleteUser(1L);
//
//        // Then
//        StepVerifier.create(result)
//                .verifyComplete();
//
//        // Verify request
//        RecordedRequest request = mockWebServer.takeRequest();
//        assertEquals("DELETE", request.getMethod());
//        assertEquals("/users/1", request.getPath());
//    }
//
//    @Test
//    void getAllUsers_ShouldRetryOnServerError() {
//        // Given
//        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
//        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
//        mockWebServer.enqueue(new MockResponse()
//                .setResponseCode(200)
//                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//                .setBody("[]"));
//
//        // When
//        Flux<User> result = userService.getAllUsers();
//
//        // Then
//        StepVerifier.create(result)
//                .verifyComplete();
//
//        // Verify that 3 requests were made (original + 2 retries)
//        assertEquals(3, mockWebServer.getRequestCount());
//    }
//
//    private User createUser(Long id, String name, String username, String email) {
//        User user = new User();
//        user.setId(id);
//        user.setName(name);
//        user.setUsername(username);
//        user.setEmail(email);
//        return user;
//    }
//}