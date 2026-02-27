package com.jobera.controller;

import com.jobera.entity.Application;
import com.jobera.entity.JobPosting;
import com.jobera.service.JobPostingService;
import com.jobera.service.RecruiterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recruiters/jobs")
@CrossOrigin(origins = "http://localhost:3000")
public class JobManagementController {
    
    private final JobPostingService jobPostingService;
    private final RecruiterService recruiterService;
    
    public JobManagementController(JobPostingService jobPostingService, RecruiterService recruiterService) {
        this.jobPostingService = jobPostingService;
        this.recruiterService = recruiterService;
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createJob(@RequestBody Map<String, Object> request) {
        try {
            System.out.println("=== CREATE JOB REQUEST ===");
            System.out.println("Request data: " + request);
            
            Long recruiterId = Long.valueOf(request.get("recruiterId").toString());
            String title = (String) request.get("title");
            String company = (String) request.get("company");
            String description = (String) request.get("description");
            String requiredSkills = (String) request.get("requiredSkills");
            String preferredSkills = (String) request.get("preferredSkills");
            String location = (String) request.get("location");
            String salaryRange = (String) request.get("salaryRange");
            String jobType = (String) request.get("jobType");
            String experienceLevel = (String) request.get("experienceLevel");
            
            System.out.println("Recruiter ID: " + recruiterId);
            System.out.println("Title: " + title);
            System.out.println("Company: " + company);
            
            JobPosting job = jobPostingService.createJobPosting(
                recruiterId, title, company, description, requiredSkills, 
                preferredSkills, location, salaryRange, jobType, experienceLevel
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Job posted successfully");
            response.put("job", createJobResponse(job));
            
            System.out.println("✅ Job created successfully with ID: " + job.getId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ ERROR creating job: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create job: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> updateJob(
            @PathVariable Long jobId, 
            @RequestBody Map<String, Object> request) {
        try {
            Long recruiterId = Long.valueOf(request.get("recruiterId").toString());
            String title = (String) request.get("title");
            String description = (String) request.get("description");
            String requiredSkills = (String) request.get("requiredSkills");
            String preferredSkills = (String) request.get("preferredSkills");
            String location = (String) request.get("location");
            String salaryRange = (String) request.get("salaryRange");
            String jobType = (String) request.get("jobType");
            String experienceLevel = (String) request.get("experienceLevel");
            Boolean isActive = (Boolean) request.get("isActive");
            
            JobPosting job = jobPostingService.updateJobPosting(
                jobId, recruiterId, title, description, requiredSkills, 
                preferredSkills, location, salaryRange, jobType, experienceLevel, isActive
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Job updated successfully");
            response.put("job", createJobResponse(job));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update job: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{jobId}")
    public ResponseEntity<Map<String, Object>> deleteJob(
            @PathVariable Long jobId, 
            @RequestParam Long recruiterId) {
        try {
            jobPostingService.deleteJobPosting(jobId, recruiterId);
            return ResponseEntity.ok(Map.of("message", "Job deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete job: " + e.getMessage()));
        }
    }
    
    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<List<JobPosting>> getRecruiterJobs(@PathVariable Long recruiterId) {
        try {
            System.out.println("=== GET RECRUITER JOBS ===");
            System.out.println("Recruiter ID: " + recruiterId);
            
            List<JobPosting> jobs = jobPostingService.getJobsByRecruiter(recruiterId);
            
            System.out.println("✅ Found " + jobs.size() + " jobs for recruiter " + recruiterId);
            
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            System.out.println("❌ ERROR fetching recruiter jobs: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }
    
    // ENHANCED VERSION - FIXED
    @GetMapping("/{jobId}/applications")
    public ResponseEntity<Map<String, Object>> getJobApplications(
            @PathVariable Long jobId, 
            @RequestParam Long recruiterId) {
        try {
            System.out.println("=== GET JOB APPLICATIONS - ENHANCED ===");
            System.out.println("Job ID: " + jobId);
            System.out.println("Recruiter ID: " + recruiterId);
            
            List<Application> applications = jobPostingService.getApplicationsForJob(jobId, recruiterId);
            
            System.out.println("✅ Final applications count: " + applications.size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("applications", applications.stream().map(this::createEnhancedApplicationResponse).toList());
            response.put("totalApplications", applications.size());
            response.put("jobId", jobId);
            response.put("recruiterId", recruiterId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("❌ ERROR fetching applications: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to fetch applications: " + e.getMessage()));
        }
    }
    
    // NEW DEBUG ENDPOINT - Get applications without recruiter verification
    @GetMapping("/{jobId}/applications/debug")
    public ResponseEntity<Map<String, Object>> getJobApplicationsDebug(@PathVariable Long jobId) {
        try {
            System.out.println("=== DEBUG: GET APPLICATIONS WITHOUT RECRUITER VERIFICATION ===");
            System.out.println("Job ID: " + jobId);
            
            List<Application> applications = jobPostingService.getApplicationsForJobDebug(jobId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("applications", applications.stream().map(this::createEnhancedApplicationResponse).toList());
            response.put("totalApplications", applications.size());
            response.put("jobId", jobId);
            response.put("message", "Debug mode - no recruiter verification");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("❌ ERROR in debug endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", "Debug failed: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> createJobResponse(JobPosting job) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", job.getId());
        response.put("title", job.getTitle());
        response.put("company", job.getCompany());
        response.put("description", job.getDescription());
        response.put("requiredSkills", job.getRequiredSkills());
        response.put("preferredSkills", job.getPreferredSkills());
        response.put("location", job.getLocation());
        response.put("salaryRange", job.getSalaryRange());
        response.put("jobType", job.getJobType());
        response.put("experienceLevel", job.getExperienceLevel());
        response.put("postedDate", job.getPostedDate());
        response.put("isActive", job.getIsActive());
        
        if (job.getRecruiter() != null) {
            response.put("recruiterId", job.getRecruiter().getId());
            response.put("recruiterCompany", job.getRecruiter().getCompanyName());
        }
        
        return response;
    }
    
    // ENHANCED APPLICATION RESPONSE WITH MORE DETAILS
    private Map<String, Object> createEnhancedApplicationResponse(Application application) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", application.getId());
        response.put("applicationDate", application.getApplicationDate());
        response.put("status", application.getStatus());
        response.put("coverLetter", application.getCoverLetter());
        response.put("applicantSkills", application.getApplicantSkills());
        response.put("applicantExperience", application.getApplicantExperience());
        response.put("applicantEducation", application.getApplicantEducation());
        
        // Enhanced applicant information with fallbacks
        if (application.getUser() != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", application.getUser().getId());
            userInfo.put("fullName", application.getUser().getFullName());
            userInfo.put("email", application.getUser().getEmail());
            userInfo.put("phone", application.getUser().getPhone());
            response.put("applicant", userInfo);
            
            // Also set direct fields for easier frontend access
            response.put("applicantName", application.getUser().getFullName());
            response.put("applicantEmail", application.getUser().getEmail());
            response.put("applicantPhone", application.getUser().getPhone());
        } else {
            // Fallback to stored applicant info
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