package com.jobera.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobera.entity.User;
import com.jobera.repository.UserRepository;
import com.jobera.service.AuthenticationService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    // Constants for repeated strings
    private static final String EMAIL = "email";
    private static final String MESSAGE = "message";
    private static final String ERROR = "error";
    private static final String USER = "user";
    private static final String TOKEN = "token";
    
    private final AuthenticationService authenticationService;
    private final UserRepository userRepository;
    
    public AuthController(AuthenticationService authenticationService, UserRepository userRepository) {
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
    }
    
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        try {
            String email = request.get(EMAIL);
            String password = request.get("password");
            String fullName = request.get("fullName");
            String phone = request.get("phone");
            
            User user = authenticationService.registerUser(email, password, fullName, phone);
            String authToken = authenticationService.generateAuthToken(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE, "User registered successfully");
            response.put(USER, createUserResponse(user));
            response.put(TOKEN, authToken);
            
            return ResponseEntity.ok(response);
            
        } catch (AuthenticationService.AuthenticationException e) {
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR, e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        try {
            String email = request.get(EMAIL);
            String password = request.get("password");
            
            User user = authenticationService.loginUser(email, password);
            String authToken = authenticationService.generateAuthToken(user);
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE, "Login successful");
            response.put(USER, createUserResponse(user));
            response.put(TOKEN, authToken);
            
            return ResponseEntity.ok(response);
            
        } catch (AuthenticationService.AuthenticationException e) {
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR, e.getMessage()));
        }
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authenticationService.logout(token);
        }
        
        return ResponseEntity.ok(Map.of(MESSAGE, "Logout successful"));
    }
    
    // Add this endpoint to debug login issues
    @PostMapping("/debug-login")
    public ResponseEntity<Map<String, Object>> debugLogin(@RequestBody Map<String, String> request) {
        try {
            String email = request.get(EMAIL);
            String password = request.get("password");
            
            System.out.println("=== DEBUG LOGIN ===");
            System.out.println("Email: " + email);
            System.out.println("Password: " + password);
            
            // Check if user exists
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                System.out.println("❌ User not found");
                return ResponseEntity.badRequest().body(Map.of(ERROR, "User not found"));
            }
            
            User user = userOpt.get();
            System.out.println("✅ User found: " + user.getEmail());
            System.out.println("User role: " + user.getRole());
            System.out.println("User roleId: " + user.getRoleId());
            System.out.println("User active: " + user.getIsActive());
            
            // Try to login
            User loggedInUser = authenticationService.loginUser(email, password);
            System.out.println("✅ Login successful");
            
            String authToken = authenticationService.generateAuthToken(loggedInUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE, "Login successful");
            response.put(USER, createUserResponse(loggedInUser));
            response.put(TOKEN, authToken);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Login error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(ERROR, "Login failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(ERROR, "Authentication required");
            return ResponseEntity.status(401).body(errorResponse);
        }
        
        String token = authHeader.substring(7);
        Optional<User> userOptional = authenticationService.validateToken(token);
        
        if (userOptional.isPresent()) {
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put(USER, createUserResponse(userOptional.get()));
            return ResponseEntity.ok(successResponse);
        } else {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(ERROR, "Invalid or expired token");
            return ResponseEntity.status(401).body(errorResponse);
        }
    }
    
    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put(EMAIL, user.getEmail());
        userResponse.put("fullName", user.getFullName());
        userResponse.put("phone", user.getPhone());
        userResponse.put("role", user.getRole());
        userResponse.put("lastLogin", user.getLastLogin());
        userResponse.put("createdAt", user.getCreatedAt());
        
        return userResponse;
    }
}