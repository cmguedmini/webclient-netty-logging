package com.chawki.webclient.logs.webclient_logs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chawki.webclient.logs.webclient_logs.dto.User;
import com.chawki.webclient.logs.webclient_logs.service.UserService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    private final UserService userService;

    @Autowired
    public TestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/demo")
    public Mono<String> runDemo() {
        log.info("Starting WebClient demo");

        return userService.getAllUsers()
                .take(3) // Take only first 3 users
                .doOnNext(user -> log.info("Demo - Processing user: {}", user.getName()))
                .collectList()
                .flatMap(users -> {
                    // Test creating a new user
                    User newUser = new User("John Doe", "johndoe", "john@example.com");
                    return userService.createUser(newUser)
                            .doOnNext(createdUser -> log.info("Demo - Created user: {}", createdUser))
                            .then(Mono.just("Demo completed successfully! Check logs for detailed request/response information."));
                })
                .onErrorReturn("Demo failed! Check logs for error details.");
    }

    @GetMapping("/params-demo")
    public Mono<String> runParametersDemo() {
        log.info("Starting parameters demo");

        // This will demonstrate URL parameter logging
        return userService.getUserById(1L)
                .map(user -> "Parameters demo completed! User found: " + user.getName())
                .onErrorReturn("Parameters demo failed! Check logs for error details.");
    }

    @GetMapping("/body-demo")
    public Mono<String> runBodyDemo() {
        log.info("Starting request body demo");

        // Create a user with detailed information to show body logging
        User newUser = new User();
        newUser.setName("Jane Doe");
        newUser.setUsername("janedoe");
        newUser.setEmail("jane.doe@example.com");
        newUser.setPhone("555-1234");
        newUser.setWebsite("www.janedoe.com");

        return userService.createUser(newUser)
                .map(createdUser -> "Body demo completed! Created user: " + createdUser.getName())
                .onErrorReturn("Body demo failed! Check logs for detailed request body information.");
    }

    @GetMapping("/error-demo")
    public Mono<String> runErrorDemo() {
        log.info("Starting error demo");

        // This will trigger a 404 error to demonstrate error logging
        return userService.getUserById(99999L)
                .map(user -> "This shouldn't happen")
                .onErrorReturn("Error demo completed! Check logs for error handling.");
    }

    @GetMapping("/health")
    public Mono<String> healthCheck() {
        return Mono.just("WebClient logging service is running!");
    }
}
