package com.ecommerce.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/core-api")
    public Mono<ResponseEntity<Object>> coreApiFallback() {
        return buildFallbackResponse("Core E-Commerce Service is currently unavailable. Please try again later.");
    }

    @RequestMapping("/notifications")
    public Mono<ResponseEntity<Object>> notificationFallback() {
        return buildFallbackResponse("Notification Service is currently unavailable. Please try again later.");
    }

    private Mono<ResponseEntity<Object>> buildFallbackResponse(String message) {
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", message
        );
        ResponseEntity<Object> response = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        return Mono.just(response);
    }
}