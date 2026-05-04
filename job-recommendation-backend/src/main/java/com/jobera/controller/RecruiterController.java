package com.jobera.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobera.entity.Recruiter;
import com.jobera.service.RecruiterService;

@RestController
@RequestMapping("/api/recruiters")
public class RecruiterController {
    
    private final RecruiterService recruiterService;
    
    public RecruiterController(RecruiterService recruiterService) {
        this.recruiterService = recruiterService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerRecruiter(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String companyName = (String) request.get("companyName");
            String companyDescription = (String) request.get("companyDescription");
            String companyWebsite = (String) request.get("companyWebsite");
            String companySize = (String) request.get("companySize");
            String industry = (String) request.get("industry");
            String contactEmail = (String) request.get("contactEmail");
            String contactPhone = (String) request.get("contactPhone");
            
            Recruiter recruiter = recruiterService.registerRecruiter(
                userId, companyName, companyDescription, companyWebsite, 
                companySize, industry, contactEmail, contactPhone
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Recruiter registered successfully");
            response.put("recruiter", createRecruiterResponse(recruiter));
            
            // Include user info with updated role
            if (recruiter.getUser() != null) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", recruiter.getUser().getId());
                userInfo.put("email", recruiter.getUser().getEmail());
                userInfo.put("fullName", recruiter.getUser().getFullName());
                userInfo.put("role", recruiter.getUser().getRole());
                userInfo.put("roleId", recruiter.getUser().getRoleId());
                response.put("user", userInfo);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (RecruiterService.RecruiterException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // Add logging for debugging
            return ResponseEntity.badRequest().body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getRecruiterByUser(@PathVariable Long userId) {
        try {
            Optional<Recruiter> recruiter = recruiterService.getRecruiterByUserId(userId);
            if (recruiter.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("recruiter", createRecruiterResponse(recruiter.get()));
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.ok(Map.of("recruiter", null, "message", "No recruiter profile found"));
            }
        } catch (Exception e) {
            e.printStackTrace(); // Add logging for debugging
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch recruiter profile: " + e.getMessage()));
        }
    }
    
    @PutMapping("/user/{userId}/profile")
    public ResponseEntity<Map<String, Object>> updateRecruiterProfile(
            @PathVariable Long userId, 
            @RequestBody Map<String, String> request) {
        try {
            Recruiter recruiter = recruiterService.updateRecruiterProfile(
                userId,
                request.get("companyDescription"),
                request.get("companyWebsite"),
                request.get("companySize"),
                request.get("industry"),
                request.get("contactEmail"),
                request.get("contactPhone")
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("recruiter", createRecruiterResponse(recruiter));
            
            return ResponseEntity.ok(response);
            
        } catch (RecruiterService.RecruiterException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace(); // Add logging for debugging
            return ResponseEntity.badRequest().body(Map.of("error", "Profile update failed: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> createRecruiterResponse(Recruiter recruiter) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", recruiter.getId());
        response.put("companyName", recruiter.getCompanyName());
        response.put("companyDescription", recruiter.getCompanyDescription());
        response.put("companyWebsite", recruiter.getCompanyWebsite());
        response.put("companySize", recruiter.getCompanySize());
        response.put("industry", recruiter.getIndustry());
        response.put("contactEmail", recruiter.getContactEmail());
        response.put("contactPhone", recruiter.getContactPhone());
        response.put("createdAt", recruiter.getCreatedAt());
        response.put("updatedAt", recruiter.getUpdatedAt());
        
        // Include user info
        if (recruiter.getUser() != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", recruiter.getUser().getId());
            userInfo.put("email", recruiter.getUser().getEmail());
            userInfo.put("fullName", recruiter.getUser().getFullName());
            userInfo.put("phone", recruiter.getUser().getPhone());
            userInfo.put("role", recruiter.getUser().getRole());
            userInfo.put("roleId", recruiter.getUser().getRoleId());
            response.put("user", userInfo);
        }
        
        return response;
    }
}