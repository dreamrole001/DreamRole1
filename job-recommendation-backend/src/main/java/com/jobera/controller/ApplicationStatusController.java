// File: src/main/java/com/jobera/controller/ApplicationStatusController.java
package com.jobera.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jobera.entity.Application;
import com.jobera.entity.JobPosting;
import com.jobera.service.ApplicationStatusService;
import com.jobera.service.JobPostingService;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "http://localhost:3000")
public class ApplicationStatusController {
    
    private final ApplicationStatusService applicationStatusService;
    private final JobPostingService jobPostingService;
    
    private static final String STATUS_APPLICATION_SUBMITTED = "APPLICATION_SUBMITTED";
    private static final String STATUS_SHORTLISTED = "SHORTLISTED";
    private static final String STATUS_TEST_ASSIGNED = "TEST_ASSIGNED";
    private static final String STATUS_TEST_IN_PROGRESS = "TEST_IN_PROGRESS";
    private static final String STATUS_TEST_COMPLETED = "TEST_COMPLETED";
    private static final String STATUS_TEST_PASSED = "TEST_PASSED";
    private static final String STATUS_TEST_FAILED = "TEST_FAILED";
    private static final String STATUS_INTERVIEW_SCHEDULED = "INTERVIEW_SCHEDULED";
    private static final String STATUS_REJECTED = "REJECTED";
    
    private static final String RESPONSE_KEY_APPLICATIONS = "applications";
    private static final String RESPONSE_KEY_TOTAL = "totalApplications";
    private static final String RESPONSE_KEY_MESSAGE = "message";
    private static final String RESPONSE_KEY_ERROR = "error";
    private static final String RESPONSE_KEY_RECRUITER_ID = "recruiterId";
    private static final String RESPONSE_KEY_STATUS = "status";
    
    public ApplicationStatusController(ApplicationStatusService applicationStatusService,
                                     JobPostingService jobPostingService) {
        this.applicationStatusService = applicationStatusService;
        this.jobPostingService = jobPostingService;
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserApplications(@PathVariable Long userId) {
        try {
            List<Application> applications = applicationStatusService.getUserApplications(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESPONSE_KEY_APPLICATIONS, applications.stream().map(this::createApplicationResponse).collect(Collectors.toList()));
            response.put(RESPONSE_KEY_TOTAL, applications.size());
            response.put(RESPONSE_KEY_MESSAGE, "Applications retrieved successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put(RESPONSE_KEY_ERROR, "Failed to load applications: " + e.getMessage());
            errorResponse.put(RESPONSE_KEY_APPLICATIONS, List.of());
            errorResponse.put(RESPONSE_KEY_TOTAL, 0);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    @GetMapping("/job/{jobId}")
    public ResponseEntity<List<Application>> getJobApplications(@PathVariable Long jobId) {
        try {
            List<Application> applications = applicationStatusService.getJobApplications(jobId);
            return ResponseEntity.ok(applications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(List.of());
        }
    }
    
    @GetMapping("/recruiter/{recruiterId}/all")
    public ResponseEntity<Map<String, Object>> getAllRecruiterApplications(@PathVariable Long recruiterId) {
        try {
            List<JobPosting> jobs = jobPostingService.getJobsByRecruiter(recruiterId);
            List<Application> allApplications = new java.util.ArrayList<>();
            
            for (JobPosting job : jobs) {
                List<Application> jobApps = applicationStatusService.getJobApplications(job.getId());
                jobApps.forEach(app -> {
                    if (app.getJobPosting() == null) {
                        app.setJobPosting(job);
                    }
                });
                allApplications.addAll(jobApps);
            }
            
            allApplications.sort((a1, a2) -> a2.getApplicationDate().compareTo(a1.getApplicationDate()));
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESPONSE_KEY_APPLICATIONS, allApplications.stream()
                .map(this::createEnhancedApplicationResponse)
                .collect(Collectors.toList()));
            response.put(RESPONSE_KEY_TOTAL, allApplications.size());
            response.put(RESPONSE_KEY_RECRUITER_ID, recruiterId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(RESPONSE_KEY_ERROR, "Failed to fetch applications: " + e.getMessage()));
        }
    }
    
    @GetMapping("/recruiter/{recruiterId}/by-status")
    public ResponseEntity<Map<String, Object>> getApplicationsByStatus(
            @PathVariable Long recruiterId,
            @RequestParam(required = false) String status) {
        try {
            List<JobPosting> jobs = jobPostingService.getJobsByRecruiter(recruiterId);
            List<Application> allApplications = new java.util.ArrayList<>();
            
            for (JobPosting job : jobs) {
                List<Application> jobApps = applicationStatusService.getJobApplications(job.getId());
                jobApps.forEach(app -> {
                    if (app.getJobPosting() == null) {
                        app.setJobPosting(job);
                    }
                });
                allApplications.addAll(jobApps);
            }
            
            if (status != null && !status.isEmpty()) {
                String statusLower = status.toLowerCase();
                allApplications = allApplications.stream()
                    .filter(app -> app.getStatus().toLowerCase().contains(statusLower))
                    .collect(Collectors.toList());
            }
            
            allApplications.sort((a1, a2) -> {
                Double score1 = a1.getMatchPercentage() != null ? a1.getMatchPercentage() : 0.0;
                Double score2 = a2.getMatchPercentage() != null ? a2.getMatchPercentage() : 0.0;
                return Double.compare(score2, score1);
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESPONSE_KEY_APPLICATIONS, allApplications.stream()
                .map(this::createEnhancedApplicationResponse)
                .collect(Collectors.toList()));
            response.put(RESPONSE_KEY_TOTAL, allApplications.size());
            response.put(RESPONSE_KEY_RECRUITER_ID, recruiterId);
            response.put(RESPONSE_KEY_STATUS, status);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(RESPONSE_KEY_ERROR, "Failed to fetch applications: " + e.getMessage()));
        }
    }
    
    @GetMapping("/recruiter/{recruiterId}/test-candidates")
    public ResponseEntity<Map<String, Object>> getTestCandidates(
            @PathVariable Long recruiterId,
            @RequestParam(required = false) String testStatus) {
        try {
            List<JobPosting> jobs = jobPostingService.getJobsByRecruiter(recruiterId);
            List<Application> allApplications = new java.util.ArrayList<>();
            
            for (JobPosting job : jobs) {
                allApplications.addAll(applicationStatusService.getJobApplications(job.getId()));
            }
            
            List<String> testStatuses = List.of(
                STATUS_TEST_ASSIGNED, 
                STATUS_TEST_IN_PROGRESS, 
                STATUS_TEST_COMPLETED,
                STATUS_TEST_PASSED, 
                STATUS_TEST_FAILED
            );
            
            List<Application> testCandidates = allApplications.stream()
                .filter(app -> testStatuses.contains(app.getStatus()))
                .collect(Collectors.toList());
            
            if (testStatus != null && !testStatus.isEmpty()) {
                testCandidates = testCandidates.stream()
                    .filter(app -> testStatus.equals(app.getStatus()))
                    .collect(Collectors.toList());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESPONSE_KEY_APPLICATIONS, testCandidates.stream()
                .map(this::createEnhancedApplicationResponse)
                .collect(Collectors.toList()));
            response.put(RESPONSE_KEY_TOTAL, testCandidates.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(RESPONSE_KEY_ERROR, "Failed to fetch test candidates: " + e.getMessage()));
        }
    }
    
    @GetMapping("/recruiter/{recruiterId}/stats")
    public ResponseEntity<Map<String, Object>> getApplicationStats(@PathVariable Long recruiterId) {
        try {
            List<JobPosting> jobs = jobPostingService.getJobsByRecruiter(recruiterId);
            List<Application> allApplications = new java.util.ArrayList<>();
            
            for (JobPosting job : jobs) {
                allApplications.addAll(applicationStatusService.getJobApplications(job.getId()));
            }
            
            long total = allApplications.size();
            long shortlisted = allApplications.stream()
                .filter(app -> STATUS_SHORTLISTED.equals(app.getStatus()))
                .count();
            long testAssigned = allApplications.stream()
                .filter(app -> STATUS_TEST_ASSIGNED.equals(app.getStatus()))
                .count();
            long testPassed = allApplications.stream()
                .filter(app -> STATUS_TEST_PASSED.equals(app.getStatus()))
                .count();
            long testFailed = allApplications.stream()
                .filter(app -> STATUS_TEST_FAILED.equals(app.getStatus()))
                .count();
            long interviewScheduled = allApplications.stream()
                .filter(app -> STATUS_INTERVIEW_SCHEDULED.equals(app.getStatus()))
                .count();
            long rejected = allApplications.stream()
                .filter(app -> STATUS_REJECTED.equals(app.getStatus()))
                .count();
            long pending = allApplications.stream()
                .filter(app -> STATUS_APPLICATION_SUBMITTED.equals(app.getStatus()))
                .count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put(RESPONSE_KEY_TOTAL, total);
            stats.put("shortlisted", shortlisted);
            stats.put("testAssigned", testAssigned);
            stats.put("testPassed", testPassed);
            stats.put("testFailed", testFailed);
            stats.put("interviewScheduled", interviewScheduled);
            stats.put("rejected", rejected);
            stats.put("pending", pending);
            stats.put(RESPONSE_KEY_RECRUITER_ID, recruiterId);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(RESPONSE_KEY_ERROR, "Failed to fetch stats: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{applicationId}/status")
    public ResponseEntity<Map<String, Object>> updateApplicationStatus(
            @PathVariable Long applicationId,
            @RequestParam Long recruiterId,
            @RequestBody Map<String, String> request) {
        try {
            String newStatus = request.get(RESPONSE_KEY_STATUS);
            String notes = request.get("notes");
            
            Application application = jobPostingService.updateApplicationStatus(applicationId, recruiterId, newStatus, notes);
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESPONSE_KEY_MESSAGE, "Application status updated successfully");
            response.put("application", createApplicationResponse(application));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(RESPONSE_KEY_ERROR, "Failed to update application status: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{applicationId}/view")
    public ResponseEntity<Map<String, Object>> markAsViewed(
            @PathVariable Long applicationId,
            @RequestParam Long recruiterId) {
        try {
            Application application = jobPostingService.markApplicationAsViewed(applicationId, recruiterId);
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESPONSE_KEY_MESSAGE, "Application marked as viewed");
            response.put("application", createApplicationResponse(application));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(RESPONSE_KEY_ERROR, "Failed to mark application as viewed: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{applicationId}/shortlist")
    public ResponseEntity<Map<String, Object>> shortlistApplication(
            @PathVariable Long applicationId,
            @RequestParam Long recruiterId,
            @RequestBody Map<String, String> request) {
        try {
            String notes = request.get("notes");
            
            Application application = applicationStatusService.shortlistApplication(applicationId, notes);
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESPONSE_KEY_MESSAGE, "Application shortlisted successfully");
            response.put("application", createApplicationResponse(application));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(RESPONSE_KEY_ERROR, "Failed to shortlist application: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{applicationId}/schedule-interview")
    public ResponseEntity<Map<String, Object>> scheduleInterview(
            @PathVariable Long applicationId,
            @RequestParam Long recruiterId,
            @RequestBody Map<String, Object> request) {
        try {
            String interviewDateStr = (String) request.get("interviewDate");
            String interviewLocation = (String) request.get("interviewLocation");
            String notes = (String) request.get("notes");
            
            System.out.println("=== SCHEDULE INTERVIEW REQUEST ===");
            System.out.println("Application ID: " + applicationId);
            System.out.println("Recruiter ID: " + recruiterId);
            System.out.println("Interview Date: " + interviewDateStr);
            System.out.println("Interview Location: " + interviewLocation);
            
            LocalDateTime interviewDate;
            if (interviewDateStr != null) {
                if (interviewDateStr.contains(".")) {
                    String cleanedDateStr = interviewDateStr.split("\\.")[0];
                    interviewDate = LocalDateTime.parse(cleanedDateStr);
                } else if (interviewDateStr.contains("Z")) {
                    String cleanedDateStr = interviewDateStr.replace("Z", "");
                    interviewDate = LocalDateTime.parse(cleanedDateStr);
                } else {
                    interviewDate = LocalDateTime.parse(interviewDateStr);
                }
            } else {
                throw new IllegalArgumentException("Interview date is required");
            }
            
            if (interviewLocation == null || interviewLocation.trim().isEmpty()) {
                throw new IllegalArgumentException("Interview location is required");
            }
            
            Application application = applicationStatusService.scheduleInterview(
                applicationId, interviewDate, interviewLocation.trim(), notes);
            
            System.out.println("✅ Application status updated to: " + application.getStatus());
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESPONSE_KEY_MESSAGE, "Interview scheduled successfully");
            response.put("application", createApplicationResponse(application));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ ERROR scheduling interview: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(RESPONSE_KEY_ERROR, "Failed to schedule interview: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{applicationId}/reject")
    public ResponseEntity<Map<String, Object>> rejectApplication(
            @PathVariable Long applicationId,
            @RequestParam Long recruiterId,
            @RequestBody Map<String, String> request) {
        try {
            String rejectionReason = request.get("rejectionReason");
            
            Application application = applicationStatusService.rejectApplication(applicationId, rejectionReason);
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESPONSE_KEY_MESSAGE, "Application rejected");
            response.put("application", createApplicationResponse(application));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(RESPONSE_KEY_ERROR, "Failed to reject application: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{applicationId}/send-offer")
    public ResponseEntity<Map<String, Object>> sendOffer(
            @PathVariable Long applicationId,
            @RequestParam Long recruiterId,
            @RequestBody Map<String, String> request) {
        try {
            String offerDetails = request.get("offerDetails");
            
            Application application = applicationStatusService.sendOffer(applicationId, offerDetails);
            
            Map<String, Object> response = new HashMap<>();
            response.put(RESPONSE_KEY_MESSAGE, "Offer sent successfully");
            response.put("application", createApplicationResponse(application));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(RESPONSE_KEY_ERROR, "Failed to send offer: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> createApplicationResponse(Application application) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", application.getId());
        response.put(RESPONSE_KEY_STATUS, application.getStatus());
        response.put("statusUpdatedAt", application.getStatusUpdatedAt());
        response.put("applicationDate", application.getApplicationDate());
        response.put("viewedByRecruiter", application.getViewedByRecruiter());
        response.put("viewedAt", application.getViewedAt());
        response.put("interviewDate", application.getInterviewDate());
        response.put("interviewLocation", application.getInterviewLocation());
        response.put("recruiterNotes", application.getRecruiterNotes());
        response.put("coverLetter", application.getCoverLetter());
        response.put("applicantSkills", application.getApplicantSkills());
        response.put("applicantExperience", application.getApplicantExperience());
        response.put("applicantEducation", application.getApplicantEducation());
        response.put("matchPercentage", application.getMatchPercentage());
        response.put("matchedSkills", application.getMatchedSkills());
        response.put("missingSkills", application.getMissingSkills());
        response.put("resumeFilePath", application.getResumeFilePath());
        response.put("hasResume", application.hasResume());
        response.put("hasTest", application.getStatus() != null && 
            (application.getStatus().equals(STATUS_TEST_ASSIGNED) ||
             application.getStatus().equals(STATUS_TEST_IN_PROGRESS) ||
             application.getStatus().equals(STATUS_TEST_COMPLETED) ||
             application.getStatus().equals(STATUS_TEST_PASSED) ||
             application.getStatus().equals(STATUS_TEST_FAILED)));
        
        Map<String, Object> applicantInfo = new HashMap<>();
        applicantInfo.put("name", application.getSafeApplicantName());
        applicantInfo.put("email", application.getSafeApplicantEmail());
        applicantInfo.put("phone", application.getSafeApplicantPhone());
        response.put("applicant", applicantInfo);
        
        response.put("applicantName", application.getSafeApplicantName());
        response.put("applicantEmail", application.getSafeApplicantEmail());
        response.put("applicantPhone", application.getSafeApplicantPhone());
        
        if (application.getJobPosting() != null) {
            Map<String, Object> jobInfo = new HashMap<>();
            jobInfo.put("id", application.getJobPosting().getId());
            jobInfo.put("title", application.getJobPosting().getTitle());
            jobInfo.put("company", application.getJobPosting().getCompany());
            jobInfo.put("location", application.getJobPosting().getLocation());
            jobInfo.put("jobType", application.getJobPosting().getJobType());
            response.put("job", jobInfo);
        }
        
        return response;
    }
    
    private Map<String, Object> createEnhancedApplicationResponse(Application application) {
        Map<String, Object> response = createApplicationResponse(application);
        response.put("hasResume", application.getResumeFilePath() != null && !application.getResumeFilePath().isEmpty());
        return response;
    }
}