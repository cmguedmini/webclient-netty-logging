package com.chawki.webclient.logs.webclient_logs.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.chawki.webclient.logs.webclient_logs.config.WebClientLoggingConfiguration;
import com.chawki.webclient.logs.webclient_logs.service.UserService;

import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/logging")
public class LoggingManagementController {
	

    private static final Logger log = LoggerFactory.getLogger(LoggingManagementController.class);

    private final WebClientLoggingConfiguration loggingConfig;
    private final UserService userService;

    @Autowired
    public LoggingManagementController(WebClientLoggingConfiguration loggingConfig, UserService userService) {
        this.loggingConfig = loggingConfig;
        this.userService = userService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getLoggingStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", loggingConfig.isEnabled());
        status.put("includeHeaders", loggingConfig.isIncludeHeaders());
        status.put("includeBody", loggingConfig.isIncludeBody());
        status.put("includeParameters", loggingConfig.isIncludeParameters());
        status.put("maxBodySize", loggingConfig.getMaxBodySize());
        status.put("maskSensitiveData", loggingConfig.isMaskSensitiveData());
        
        return ResponseEntity.ok(status);
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleLogging(@RequestParam(required = false) Boolean enabled) {
        boolean newState = enabled != null ? enabled : !loggingConfig.isEnabled();
        boolean oldState = loggingConfig.isEnabled();
        
        loggingConfig.setEnabled(newState);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "WebClient logging " + (newState ? "enabled" : "disabled"));
        response.put("previousState", oldState);
        response.put("currentState", newState);
        
        log.info("WebClient logging toggled from {} to {} via REST endpoint", oldState, newState);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/headers")
    public ResponseEntity<Map<String, Object>> toggleHeaders(@RequestParam boolean enabled) {
        boolean oldState = loggingConfig.isIncludeHeaders();
        loggingConfig.setIncludeHeaders(enabled);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Header logging " + (enabled ? "enabled" : "disabled"));
        response.put("previousState", oldState);
        response.put("currentState", enabled);
        
        log.info("WebClient header logging changed from {} to {}", oldState, enabled);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/body")
    public ResponseEntity<Map<String, Object>> toggleBody(@RequestParam boolean enabled) {
        boolean oldState = loggingConfig.isIncludeBody();
        loggingConfig.setIncludeBody(enabled);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Body logging " + (enabled ? "enabled" : "disabled"));
        response.put("previousState", oldState);
        response.put("currentState", enabled);
        
        log.info("WebClient body logging changed from {} to {}", oldState, enabled);
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/body-size")
    public ResponseEntity<Map<String, Object>> setBodySize(@RequestParam int size) {
        if (size < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Body size must be non-negative"));
        }
        
        int oldSize = loggingConfig.getMaxBodySize();
        loggingConfig.setMaxBodySize(size);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Max body size updated");
        response.put("previousSize", oldSize);
        response.put("currentSize", size);
        
        log.info("WebClient max body size changed from {} to {}", oldSize, size);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-with-current-settings")
    public Mono<ResponseEntity<Map<String, Object>>> testWithCurrentSettings() {
        log.info("Testing WebClient with current logging settings: {}", loggingConfig);
        
        return userService.getUserById(1L)
                .map(user -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("message", "Test completed successfully");
                    response.put("user", user.getName());
                    response.put("currentLoggingSettings", getCurrentSettings());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.ok(Map.of(
                    "message", "Test completed with error (check logs)",
                    "currentLoggingSettings", getCurrentSettings()
                )));
    }

    private Map<String, Object> getCurrentSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("enabled", loggingConfig.isEnabled());
        settings.put("includeHeaders", loggingConfig.isIncludeHeaders());
        settings.put("includeBody", loggingConfig.isIncludeBody());
        settings.put("includeParameters", loggingConfig.isIncludeParameters());
        settings.put("maxBodySize", loggingConfig.getMaxBodySize());
        settings.put("maskSensitiveData", loggingConfig.isMaskSensitiveData());
        return settings;
    }
}