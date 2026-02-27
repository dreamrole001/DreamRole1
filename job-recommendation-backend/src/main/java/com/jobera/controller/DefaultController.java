package com.jobera.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefaultController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Job Recommendation Backend is running!");
        response.put("status", "active");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        response.put("endpoints", "/api/health, /api/jobs, /api/resumes");
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/api")
    public ResponseEntity<Map<String, Object>> apiRoot() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Job Recommendation API");
        response.put("version", "1.0.0");
        
        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/api/health");
        endpoints.put("jobs", "/api/jobs");
        endpoints.put("resumes", "/api/resumes");
        endpoints.put("upload", "/api/resumes/upload/{userId}");
        
        response.put("endpoints", endpoints);
        
        return ResponseEntity.ok(response);
    }
}