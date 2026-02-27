package com.jobera.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jobera.dto.JobPostingDTO;
import com.jobera.entity.Application;
import com.jobera.entity.JobPosting;
import com.jobera.repository.ApplicationRepository;
import com.jobera.service.JobPostingService;
import com.jobera.service.JobRecommendationService;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:3000")
public class JobController {
    
    private final JobPostingService jobPostingService;
    private final JobRecommendationService recommendationService;
    private final ApplicationRepository applicationRepository;
    
    // Constants for repeated strings
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String JOB_ID_KEY = "jobId";
    private static final String USER_ID_KEY = "userId";
    
    public JobController(JobPostingService jobPostingService, 
                        JobRecommendationService recommendationService,
                        ApplicationRepository applicationRepository) {
        this.jobPostingService = jobPostingService;
        this.recommendationService = recommendationService;
        this.applicationRepository = applicationRepository;
    }
    
    @GetMapping
    public ResponseEntity<List<JobPostingDTO>> getAllJobs() {
        try {
            List<JobPosting> jobs = jobPostingService.getActiveJobPostings();
            List<JobPostingDTO> jobDTOs = jobs.stream()
                .map(JobPostingDTO::new)
                .toList();
            return ResponseEntity.ok(jobDTOs);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<JobPosting>> searchJobs(@RequestParam String keyword) {
        try {
            List<JobPosting> jobs = jobPostingService.searchJobs(keyword);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }
    
    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<List<JobPosting>> getRecommendations(@PathVariable Long userId) {
        try {
            List<JobPosting> recommendations = recommendationService.recommendJobsForUser(userId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }
    
    @GetMapping("/recommendations/{userId}/detailed")
    public ResponseEntity<Map<String, Object>> getDetailedRecommendations(@PathVariable Long userId) {
        try {
            Map<String, Object> detailedAnalysis = recommendationService.getDetailedRecommendationAnalysis(userId);
            return ResponseEntity.ok(detailedAnalysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, "Failed to generate detailed recommendations: " + e.getMessage()));
        }
    }
    
    @GetMapping("/recommendations/skills")
    public ResponseEntity<List<JobPosting>> recommendJobsBySkills(@RequestParam List<String> skills) {
        try {
            List<JobPosting> recommendations = recommendationService.recommendJobsBySkills(new java.util.HashSet<>(skills));
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }
    
    @GetMapping("/{jobId}")
    public ResponseEntity<JobPosting> getJobById(@PathVariable Long jobId) {
        try {
            return jobPostingService.getJobPostingById(jobId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    // NEW DEBUG ENDPOINT - Check all applications in system
    @GetMapping("/debug/applications")
    public ResponseEntity<Map<String, Object>> debugAllApplications() {
        try {
            System.out.println("=== DEBUG ALL APPLICATIONS ===");
            
            // Get all applications
            List<Application> allApplications = applicationRepository.findAll();
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put("totalApplications", allApplications.size());
            
            List<Map<String, Object>> appDetails = allApplications.stream().map(app -> {
                Map<String, Object> appInfo = new HashMap<>();
                appInfo.put("id", app.getId());
                appInfo.put("applicantName", getSafeApplicantName(app));
                appInfo.put("applicantEmail", getSafeApplicantEmail(app));
                appInfo.put("applicationDate", app.getApplicationDate());
                appInfo.put("status", app.getStatus());
                appInfo.put("jobId", app.getJobPosting() != null ? app.getJobPosting().getId() : "No job");
                appInfo.put("jobTitle", app.getJobPosting() != null ? app.getJobPosting().getTitle() : "No job");
                appInfo.put("jobRecruiter", app.getJobPosting() != null && app.getJobPosting().getRecruiter() != null ? 
                    app.getJobPosting().getRecruiter().getId() : "No recruiter");
                appInfo.put("userId", app.getUser() != null ? app.getUser().getId() : "No user");
                appInfo.put("coverLetterLength", app.getCoverLetter() != null ? app.getCoverLetter().length() : 0);
                appInfo.put("hasSkills", app.getApplicantSkills() != null && !app.getApplicantSkills().isEmpty());
                return appInfo;
            }).toList();
            
            debugInfo.put("applications", appDetails);
            
            System.out.println("Total applications found: " + allApplications.size());
            for (Application app : allApplications) {
                System.out.println("App ID: " + app.getId() + 
                    ", Job: " + (app.getJobPosting() != null ? app.getJobPosting().getTitle() : "No job") +
                    ", Applicant: " + getSafeApplicantName(app));
            }
            
            return ResponseEntity.ok(debugInfo);
            
        } catch (Exception e) {
            System.out.println("DEBUG ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Debug failed: " + e.getMessage()));
        }
    }
    
    // NEW DEBUG ENDPOINT - Check applications for specific job
    @GetMapping("/{jobId}/applications/debug")
    public ResponseEntity<Map<String, Object>> debugJobApplications(@PathVariable Long jobId) {
        try {
            System.out.println("=== DEBUG JOB APPLICATIONS ===");
            System.out.println("Job ID: " + jobId);
            
            Optional<JobPosting> jobOpt = jobPostingService.getJobPostingById(jobId);
            if (jobOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Job not found"));
            }
            
            JobPosting job = jobOpt.get();
            List<Application> applications = applicationRepository.findByJobPostingId(jobId);
            
            Map<String, Object> debugInfo = new HashMap<>();
            debugInfo.put(JOB_ID_KEY, jobId);
            debugInfo.put("jobTitle", job.getTitle());
            debugInfo.put("jobCompany", job.getCompany());
            debugInfo.put("jobRecruiter", job.getRecruiter() != null ? 
                Map.of("id", job.getRecruiter().getId(), "company", job.getRecruiter().getCompanyName()) : "No recruiter");
            debugInfo.put("totalApplications", applications.size());
            
            List<Map<String, Object>> appDetails = applications.stream().map(app -> {
                Map<String, Object> appInfo = new HashMap<>();
                appInfo.put("id", app.getId());
                appInfo.put("applicantName", getSafeApplicantName(app));
                appInfo.put("applicantEmail", getSafeApplicantEmail(app));
                appInfo.put("applicationDate", app.getApplicationDate());
                appInfo.put("status", app.getStatus());
                appInfo.put(USER_ID_KEY, app.getUser() != null ? app.getUser().getId() : "No user");
                appInfo.put("coverLetterLength", app.getCoverLetter() != null ? app.getCoverLetter().length() : 0);
                appInfo.put("hasSkills", app.getApplicantSkills() != null && !app.getApplicantSkills().isEmpty());
                return appInfo;
            }).toList();
            
            debugInfo.put("applications", appDetails);
            
            System.out.println("Found " + applications.size() + " applications for job " + jobId);
            
            return ResponseEntity.ok(debugInfo);
            
        } catch (Exception e) {
            System.out.println("DEBUG ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Debug failed: " + e.getMessage()));
        }
    }
    
    // Debug endpoint to check raw data reception
    @PostMapping("/{jobId}/debug-apply/{userId}")
    public ResponseEntity<Map<String, Object>> debugApply(
            @PathVariable Long jobId,
            @PathVariable Long userId,
            @RequestBody Object rawData) {
        
        try {
            System.out.println("=== DEBUG APPLY ENDPOINT ===");
            System.out.println("Raw data type: " + (rawData != null ? rawData.getClass().getName() : "null"));
            System.out.println("Raw data: " + rawData);
            System.out.println("Job ID: " + jobId);
            System.out.println("User ID: " + userId);
            
            return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Debug endpoint working",
                "rawData", rawData != null ? rawData.toString() : "null",
                "dataType", rawData != null ? rawData.getClass().getName() : "null",
                JOB_ID_KEY, jobId,
                USER_ID_KEY, userId
            ));
            
        } catch (Exception e) {
            System.out.println("DEBUG ERROR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, "Debug failed: " + e.getMessage()));
        }
    }
    // Add this method to your existing JobController

@PostMapping("/{jobId}/apply-with-resume/{userId}")
public ResponseEntity<Map<String, Object>> applyForJobWithResume(
        @PathVariable Long jobId,
        @PathVariable Long userId,
        @RequestBody Map<String, Object> applicationData) {
    try {
        System.out.println("=== APPLY FOR JOB WITH RESUME ===");
        System.out.println("Job ID: " + jobId);
        System.out.println("User ID: " + userId);
        System.out.println("Received application data: " + applicationData.keySet());
        
        // Extract data with safe defaults
        String coverLetter = getStringValue(applicationData, "coverLetter", "");
        String applicantSkills = getStringValue(applicationData, "applicantSkills", "[]");
        String applicantEducation = getStringValue(applicationData, "applicantEducation", "Not specified");
        String resumeFilePath = getStringValue(applicationData, "resumeFilePath", "");
        String resumeParsedText = getStringValue(applicationData, "resumeParsedText", "");
        
        // Handle experience with safe parsing
        Integer applicantExperience = parseExperienceSafely(applicationData.get("applicantExperience"));
        
        System.out.println("Final parsed data:");
        System.out.println("- CoverLetter: " + coverLetter);
        System.out.println("- Skills: " + applicantSkills);
        System.out.println("- Experience: " + applicantExperience);
        System.out.println("- Education: " + applicantEducation);
        System.out.println("- Resume File: " + resumeFilePath);
        
        // Call enhanced service method
        Application application = jobPostingService.applyForJobWithResume(
            userId, jobId, coverLetter, applicantSkills, applicantExperience, 
            applicantEducation, resumeFilePath, resumeParsedText);
        
        return ResponseEntity.ok(Map.of(
            "message", "Application submitted successfully with resume analysis",
            "applicationId", application.getId(),
            "matchPercentage", application.getMatchPercentage(),
            "jobId", jobId,
            "userId", userId
        ));
        
    } catch (Exception e) {
        System.out.println("ERROR in apply with resume: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Failed to apply for job: " + e.getMessage()));
    }
}
    // Test endpoint for debugging
    @PostMapping("/{jobId}/test-apply/{userId}")
    public ResponseEntity<Map<String, Object>> testApplyForJob(
            @PathVariable Long jobId,
            @PathVariable Long userId) {
        try {
            System.out.println("=== TEST APPLY ENDPOINT CALLED ===");
            System.out.println("Job ID: " + jobId);
            System.out.println("User ID: " + userId);
            
            // Test with simple data
            jobPostingService.applyForJob(userId, jobId, 
                "Test cover letter from debug endpoint", 
                "[\"Java\", \"Spring\", \"React\"]", 
                3, 
                "Bachelor's Degree"
            );
            
            return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Test application submitted successfully",
                JOB_ID_KEY, jobId,
                USER_ID_KEY, userId
            ));
        } catch (Exception e) {
            System.out.println("ERROR in test apply: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, "Test failed: " + e.getMessage()));
        }
    }
    
    // Simple debug endpoint
    @PostMapping("/{jobId}/simple-apply/{userId}")
    public ResponseEntity<Map<String, Object>> simpleApply(
            @PathVariable Long jobId,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> applicationData) {
        
        try {
            System.out.println("=== SIMPLE APPLY DEBUG ===");
            System.out.println("Job ID: " + jobId);
            System.out.println("User ID: " + userId);
            System.out.println("Received data: " + applicationData);
            
            // Extract data safely with defaults
            String coverLetter = getStringValue(applicationData, "coverLetter", "Interested in this position");
            String skills = getStringValue(applicationData, "applicantSkills", "[]");
            String education = getStringValue(applicationData, "applicantEducation", "Not specified");
            
            // Handle experience safely
            Integer experience = parseExperienceSafely(applicationData.get("applicantExperience"));
            
            System.out.println("Parsed - Cover: " + coverLetter);
            System.out.println("Parsed - Skills: " + skills);
            System.out.println("Parsed - Exp: " + experience);
            System.out.println("Parsed - Edu: " + education);
            
            // Use your existing service
            jobPostingService.applyForJob(userId, jobId, coverLetter, skills, experience, education);
            
            return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Application submitted successfully via simple endpoint",
                JOB_ID_KEY, jobId,
                USER_ID_KEY, userId,
                "dataReceived", applicationData
            ));
            
        } catch (Exception e) {
            System.out.println("ERROR in simple apply: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, "Application failed: " + e.getMessage()));
        }
    }
    
    // Main apply method with enhanced error handling
    @PostMapping("/{jobId}/apply/{userId}")
    public ResponseEntity<Map<String, Object>> applyForJob(
            @PathVariable Long jobId,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> applicationData) {
        try {
            System.out.println("=== MAIN APPLY ENDPOINT ===");
            System.out.println("Job ID: " + jobId);
            System.out.println("User ID: " + userId);
            System.out.println("Received application data: " + applicationData);
            
            // Validate required fields
            if (applicationData == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "Application data is required"));
            }
            
            // Extract data with safe defaults
            String coverLetter = getStringValue(applicationData, "coverLetter", "");
            String applicantSkills = getStringValue(applicationData, "applicantSkills", "[]");
            String applicantEducation = getStringValue(applicationData, "applicantEducation", "Not specified");
            
            // Handle experience with safe parsing
            Integer applicantExperience = parseExperienceSafely(applicationData.get("applicantExperience"));
            
            System.out.println("Final parsed data:");
            System.out.println("- CoverLetter: " + coverLetter);
            System.out.println("- Skills: " + applicantSkills);
            System.out.println("- Experience: " + applicantExperience);
            System.out.println("- Education: " + applicantEducation);
            
            // Call service method
            jobPostingService.applyForJob(userId, jobId, coverLetter, 
                                        applicantSkills, applicantExperience, 
                                        applicantEducation);
            
            return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Application submitted successfully",
                JOB_ID_KEY, jobId,
                USER_ID_KEY, userId
            ));
            
        } catch (Exception e) {
            System.out.println("ERROR in main apply endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, "Failed to apply for job: " + e.getMessage()));
        }
    }
    
    // Helper method to safely get string values with default
    private String getStringValue(Map<String, Object> data, String key, String defaultValue) {
        if (data == null) return defaultValue;
        
        Object value = data.get(key);
        if (value == null) {
            return defaultValue;
        }
        return value.toString().trim();
    }
    
    // Helper method to safely parse experience values
    private Integer parseExperienceSafely(Object experienceObj) {
        if (experienceObj == null) return 0;
        
        try {
            if (experienceObj instanceof Integer integer) {
                return integer;
            } else if (experienceObj instanceof String string) {
                return Integer.parseInt(string.trim());
            } else if (experienceObj instanceof Number number) {
                return number.intValue();
            } else {
                return 0;
            }
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    // Helper methods for safe applicant data access
    private String getSafeApplicantName(Application app) {
        if (app.getApplicantName() != null && !app.getApplicantName().trim().isEmpty()) {
            return app.getApplicantName();
        }
        return app.getUser() != null ? app.getUser().getFullName() : "Unknown Applicant";
    }
    
    private String getSafeApplicantEmail(Application app) {
        if (app.getApplicantEmail() != null && !app.getApplicantEmail().trim().isEmpty()) {
            return app.getApplicantEmail();
        }
        return app.getUser() != null ? app.getUser().getEmail() : "No email provided";
    }
}