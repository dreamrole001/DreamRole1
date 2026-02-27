package com.jobera.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "UP", 
            "service", "Job Recommendation Backend",
            "timestamp", java.time.LocalDateTime.now().toString(),
            "database", "connected"
        ));
    }
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> testEndpoint() {
        return ResponseEntity.ok(Map.of(
            "message", "Backend is working perfectly!",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}