package com.jobera.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jobera.entity.Application;
import com.jobera.service.JobPostingService;

@RestController
@RequestMapping("/api/resume-matching")
@CrossOrigin(origins = "http://localhost:3000")
public class ResumeMatchingController {
    
    private final JobPostingService jobPostingService;
    
    public ResumeMatchingController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }
    
    @GetMapping("/job/{jobId}/applications/sorted")
    public ResponseEntity<Map<String, Object>> getApplicationsSortedByMatch(
            @PathVariable Long jobId,
            @RequestParam Long recruiterId) {
        try {
            System.out.println("=== GET APPLICATIONS SORTED BY MATCH PERCENTAGE ===");
            System.out.println("Job ID: " + jobId);
            System.out.println("Recruiter ID: " + recruiterId);
            
            List<Application> applications = jobPostingService.getApplicationsForJob(jobId, recruiterId);
            
            // Sort applications by match percentage (descending)
            List<Application> sortedApplications = applications.stream()
                .sorted((a1, a2) -> {
                    Double score1 = a1.getMatchPercentage() != null ? a1.getMatchPercentage() : 0.0;
                    Double score2 = a2.getMatchPercentage() != null ? a2.getMatchPercentage() : 0.0;
                    return Double.compare(score2, score1);
                })
                .collect(Collectors.toList());
            
            System.out.println("✅ Found " + sortedApplications.size() + " applications, sorted by match percentage");
            
            Map<String, Object> response = new HashMap<>();
            response.put("applications", sortedApplications.stream()
                .map(this::createEnhancedApplicationResponse)
                .collect(Collectors.toList()));
            response.put("totalApplications", sortedApplications.size());
            response.put("jobId", jobId);
            response.put("recruiterId", recruiterId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ ERROR fetching sorted applications: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to fetch applications: " + e.getMessage()));
        }
    }
    
    @GetMapping("/application/{applicationId}/resume-analysis")
    public ResponseEntity<Map<String, Object>> getResumeAnalysis(@PathVariable Long applicationId) {
        try {
            // This would typically fetch from the application entity
            // For now, return a mock analysis
            Map<String, Object> analysis = new HashMap<>();
            analysis.put("matchPercentage", 85.5);
            analysis.put("strengths", List.of("Java", "Spring Boot", "MySQL", "REST API"));
            analysis.put("improvements", List.of("Docker", "Kubernetes", "AWS"));
            analysis.put("experienceMatch", "Good");
            analysis.put("educationMatch", "Excellent");
            
            return ResponseEntity.ok(analysis);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Failed to analyze resume: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> createEnhancedApplicationResponse(Application application) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", application.getId());
        response.put("applicationDate", application.getApplicationDate());
        response.put("status", application.getStatus());
        response.put("coverLetter", application.getCoverLetter());
        response.put("applicantSkills", application.getApplicantSkills());
        response.put("applicantExperience", application.getApplicantExperience());
        response.put("applicantEducation", application.getApplicantEducation());
        
        // Resume matching data
        response.put("matchPercentage", application.getMatchPercentage() != null ? 
            Math.round(application.getMatchPercentage() * 100.0) / 100.0 : 0.0);
        response.put("matchedSkills", application.getMatchedSkills());
        response.put("missingSkills", application.getMissingSkills());
        response.put("resumeFilePath", application.getResumeFilePath());
        response.put("hasResume", application.getResumeFilePath() != null && !application.getResumeFilePath().isEmpty());
        
        // Applicant information
        if (application.getUser() != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", application.getUser().getId());
            userInfo.put("fullName", application.getUser().getFullName());
            userInfo.put("email", application.getUser().getEmail());
            userInfo.put("phone", application.getUser().getPhone());
            response.put("applicant", userInfo);
            
            response.put("applicantName", application.getUser().getFullName());
            response.put("applicantEmail", application.getUser().getEmail());
            response.put("applicantPhone", application.getUser().getPhone());
        } else {
            response.put("applicantName", application.getApplicantName());
            response.put("applicantEmail", application.getApplicantEmail());
            response.put("applicantPhone", application.getApplicantPhone());
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("fullName", application.getApplicantName());
            userInfo.put("email", application.getApplicantEmail());
            userInfo.put("phone", application.getApplicantPhone());
            response.put("applicant", userInfo);
        }
        
        // Job information
        if (application.getJobPosting() != null) {
            Map<String, Object> jobInfo = new HashMap<>();
            jobInfo.put("id", application.getJobPosting().getId());
            jobInfo.put("title", application.getJobPosting().getTitle());
            jobInfo.put("company", application.getJobPosting().getCompany());
            response.put("job", jobInfo);
        }
        
        return response;
    }
}