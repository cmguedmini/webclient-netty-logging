package com.chawki.webclient.logs.webclient_logs.test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/success")
    public ResponseEntity<Map<String, Object>> success(@RequestHeader Map<String, String> headers) {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Requête traitée avec succès",
                "receivedHeaders", headers.size(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    @PostMapping("/success")
    public ResponseEntity<Map<String, Object>> successPost(
            @RequestHeader Map<String, String> headers,
            @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "POST traité avec succès",
                "receivedHeaders", headers.size(),
                "receivedBody", body != null ? body : Map.of(),
                "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/error")
    public ResponseEntity<Map<String, Object>> error() {
        throw new RuntimeException("Erreur simulée pour test");
    }

    @PostMapping("/error")
    public ResponseEntity<Map<String, Object>> errorPost(@RequestBody(required = false) Map<String, Object> body) {
        throw new RuntimeException("Erreur simulée pour test POST avec body: " + body);
    }

    @GetMapping("/timeout")
    public ResponseEntity<Map<String, Object>> timeout() throws InterruptedException {
        // Simulate a long operation that will cause timeout
        TimeUnit.SECONDS.sleep(10);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cette réponse ne devrait jamais arriver"
        ));
    }

    @PostMapping("/timeout")
    public ResponseEntity<Map<String, Object>> timeoutPost(@RequestBody(required = false) Map<String, Object> body) 
            throws InterruptedException {
        // Simulate a long operation that will cause timeout
        TimeUnit.SECONDS.sleep(10);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cette réponse ne devrait jamais arriver",
                "body", body
        ));
    }
}
