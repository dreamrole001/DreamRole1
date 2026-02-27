// File: src/main/java/com/jobera/controller/AdminController.java
package com.jobera.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobera.entity.Application;
import com.jobera.entity.JobPosting;
import com.jobera.entity.Recruiter;
import com.jobera.entity.User;
import com.jobera.repository.ApplicationRepository;
import com.jobera.repository.JobPostingRepository;
import com.jobera.repository.RecruiterRepository;
import com.jobera.repository.ResumeRepository;
import com.jobera.repository.UserRepository;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:3000")
public class AdminController {
    
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String EMAIL_KEY = "email";
    private static final String FULL_NAME_KEY = "fullName";
    private static final String IS_ACTIVE_KEY = "isActive";
    
    private final UserRepository userRepository;
    private final RecruiterRepository recruiterRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;
    
    public AdminController(UserRepository userRepository,
                         RecruiterRepository recruiterRepository,
                         JobPostingRepository jobPostingRepository,
                         ApplicationRepository applicationRepository,
                         ResumeRepository resumeRepository) {
        this.userRepository = userRepository;
        this.recruiterRepository = recruiterRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.resumeRepository = resumeRepository;
    }
    
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        try {
            long totalUsers = userRepository.count();
            long totalRecruiters = recruiterRepository.count();
            long totalJobs = jobPostingRepository.count();
            long totalApplications = applicationRepository.count();
            long totalResumes = resumeRepository.count();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("totalRecruiters", totalRecruiters);
            stats.put("totalJobs", totalJobs);
            stats.put("totalApplications", totalApplications);
            stats.put("totalResumes", totalResumes);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to fetch stats: " + e.getMessage()));
        }
    }
    
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            
            List<Map<String, Object>> userResponses = users.stream()
                .map(this::createSafeUserResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(userResponses);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
    private Map<String, Object> createSafeUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("id", user.getId());
        userResponse.put(EMAIL_KEY, user.getEmail());
        userResponse.put(FULL_NAME_KEY, user.getFullName());
        userResponse.put("phone", user.getPhone());
        userResponse.put("roleId", user.getRoleId());
        userResponse.put("role", user.getRole());
        userResponse.put(IS_ACTIVE_KEY, user.getIsActive());
        userResponse.put("createdAt", user.getCreatedAt());
        userResponse.put("lastLogin", user.getLastLogin());
        return userResponse;
    }
    
    @GetMapping("/recruiters")
    public ResponseEntity<List<Map<String, Object>>> getAllRecruiters() {
        try {
            List<Recruiter> recruiters = recruiterRepository.findAll();
            List<Map<String, Object>> recruiterDetails = recruiters.stream().map(recruiter -> {
                Map<String, Object> details = new HashMap<>();
                details.put("id", recruiter.getId());
                details.put("companyName", recruiter.getCompanyName());
                details.put("companySize", recruiter.getCompanySize());
                details.put("industry", recruiter.getIndustry());
                details.put("contactEmail", recruiter.getContactEmail());
                details.put("createdAt", recruiter.getCreatedAt());
                
                if (recruiter.getUser() != null) {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", recruiter.getUser().getId());
                    userInfo.put(EMAIL_KEY, recruiter.getUser().getEmail());
                    userInfo.put(FULL_NAME_KEY, recruiter.getUser().getFullName());
                    userInfo.put("phone", recruiter.getUser().getPhone());
                    details.put("user", userInfo);
                }
                
                long jobCount = jobPostingRepository.findByRecruiterIdAndIsActiveTrue(recruiter.getId()).size();
                details.put("jobCount", jobCount);
                
                return details;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(recruiterDetails);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
    @GetMapping("/jobs")
    public ResponseEntity<List<Map<String, Object>>> getAllJobs() {
        try {
            List<JobPosting> jobs = jobPostingRepository.findAll();
            List<Map<String, Object>> jobDetails = jobs.stream().map(job -> {
                Map<String, Object> details = new HashMap<>();
                details.put("id", job.getId());
                details.put("title", job.getTitle());
                details.put("company", job.getCompany());
                details.put("location", job.getLocation());
                details.put("jobType", job.getJobType());
                details.put("experienceLevel", job.getExperienceLevel());
                details.put("salaryRange", job.getSalaryRange());
                details.put("postedDate", job.getPostedDate());
                details.put(IS_ACTIVE_KEY, job.getIsActive());
                
                long applicationCount = applicationRepository.findByJobPostingId(job.getId()).size();
                details.put("applicationCount", applicationCount);
                
                if (job.getRecruiter() != null) {
                    Map<String, Object> recruiterInfo = new HashMap<>();
                    recruiterInfo.put("id", job.getRecruiter().getId());
                    recruiterInfo.put("companyName", job.getRecruiter().getCompanyName());
                    recruiterInfo.put("contactEmail", job.getRecruiter().getContactEmail());
                    details.put("recruiter", recruiterInfo);
                } else {
                    details.put("recruiter", "System Posted");
                }
                
                return details;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(jobDetails);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }
    
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long userId) {
        try {
            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(ERROR_KEY, "User not found"));
            }
            
            if (user.get().getRoleId() == 2L) {
                return ResponseEntity.status(403).body(Map.of(ERROR_KEY, "Cannot delete admin user"));
            }
            
            Optional<Recruiter> recruiter = recruiterRepository.findByUserId(userId);
            if (recruiter.isPresent()) {
                recruiterRepository.deleteById(recruiter.get().getId());
            } else {
                userRepository.deleteById(userId);
            }
            
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "User deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(ERROR_KEY, "Failed to delete user: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/recruiters/{recruiterId}")
    public ResponseEntity<Map<String, String>> deleteRecruiter(@PathVariable Long recruiterId) {
        try {
            Optional<Recruiter> recruiter = recruiterRepository.findById(recruiterId);
            if (recruiter.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(ERROR_KEY, "Recruiter not found"));
            }
            
            Long userId = recruiter.get().getUser().getId();
            
            recruiterRepository.deleteById(recruiterId);
            userRepository.deleteById(userId);
            
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Recruiter deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(ERROR_KEY, "Failed to delete recruiter: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/jobs/{jobId}")
    public ResponseEntity<Map<String, String>> deleteJob(@PathVariable Long jobId) {
        try {
            Optional<JobPosting> job = jobPostingRepository.findById(jobId);
            if (job.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(ERROR_KEY, "Job not found"));
            }
            
            jobPostingRepository.deleteById(jobId);
            return ResponseEntity.ok(Map.of(MESSAGE_KEY, "Job deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(ERROR_KEY, "Failed to delete job: " + e.getMessage()));
        }
    }
    
    @PutMapping("/jobs/{jobId}/toggle-active")
    public ResponseEntity<Map<String, Object>> toggleJobActive(@PathVariable Long jobId) {
        try {
            Optional<JobPosting> jobOpt = jobPostingRepository.findById(jobId);
            if (jobOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(ERROR_KEY, "Job not found"));
            }
            
            JobPosting job = jobOpt.get();
            job.setIsActive(!job.getIsActive());
            jobPostingRepository.save(job);
            
            return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Job status updated successfully",
                IS_ACTIVE_KEY, job.getIsActive()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(ERROR_KEY, "Failed to update job status: " + e.getMessage()));
        }
    }
    
    @GetMapping("/applications")
    public ResponseEntity<List<Map<String, Object>>> getAllApplications() {
        try {
            List<Application> applications = applicationRepository.findAll();
            List<Map<String, Object>> applicationDetails = applications.stream().map(app -> {
                Map<String, Object> details = new HashMap<>();
                details.put("id", app.getId());
                details.put("applicationDate", app.getApplicationDate());
                details.put("status", app.getStatus());
                details.put("coverLetter", app.getCoverLetter());
                details.put("applicantSkills", app.getApplicantSkills());
                details.put("applicantExperience", app.getApplicantExperience());
                details.put("applicantEducation", app.getApplicantEducation());
                
                if (app.getUser() != null) {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", app.getUser().getId());
                    userInfo.put(EMAIL_KEY, app.getUser().getEmail());
                    userInfo.put(FULL_NAME_KEY, app.getUser().getFullName());
                    details.put("applicant", userInfo);
                } else {
                    details.put("applicantName", app.getApplicantName());
                    details.put("applicantEmail", app.getApplicantEmail());
                }
                
                if (app.getJobPosting() != null) {
                    Map<String, Object> jobInfo = new HashMap<>();
                    jobInfo.put("id", app.getJobPosting().getId());
                    jobInfo.put("title", app.getJobPosting().getTitle());
                    jobInfo.put("company", app.getJobPosting().getCompany());
                    details.put("job", jobInfo);
                }
                
                return details;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(applicationDetails);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }
}