package com.jobera.service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;

import com.jobera.entity.Application;
import com.jobera.entity.JobPosting;
import com.jobera.entity.Recruiter;
import com.jobera.entity.User;
import com.jobera.repository.ApplicationRepository;
import com.jobera.repository.JobPostingRepository;
import com.jobera.repository.RecruiterRepository;
import com.jobera.repository.UserRepository;

@Service
public class JobPostingService {
    
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final RecruiterRepository recruiterRepository;
    private final ObjectMapper objectMapper;
    
    public JobPostingService(JobPostingRepository jobPostingRepository,
                           ApplicationRepository applicationRepository,
                           UserRepository userRepository,
                           RecruiterRepository recruiterRepository) {
        this.jobPostingRepository = jobPostingRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.recruiterRepository = recruiterRepository;
        this.objectMapper = new ObjectMapper();
    }
    
    public List<JobPosting> getActiveJobPostings() {
        return getActiveJobPostingsSortedByDate();
    }
    
    public List<JobPosting> searchJobs(String keyword) {
        return jobPostingRepository.searchActiveJobs(keyword);
    }
    
    public Optional<JobPosting> getJobPostingById(Long id) {
        return jobPostingRepository.findById(id);
    }
    
    // Updated method with additional applicant information and enhanced logging
    public void applyForJob(Long userId, Long jobId, String coverLetter, 
                           String applicantSkills, Integer applicantExperience, 
                           String applicantEducation) {
        System.out.println("=== JOB POSTING SERVICE - APPLY FOR JOB ===");
        System.out.println("User ID: " + userId);
        System.out.println("Job ID: " + jobId);
        System.out.println("Cover Letter: " + (coverLetter != null ? coverLetter.substring(0, Math.min(50, coverLetter.length())) + "..." : "null"));
        System.out.println("Skills: " + applicantSkills);
        System.out.println("Experience: " + applicantExperience);
        System.out.println("Education: " + applicantEducation);
        
        Optional<User> user = userRepository.findById(userId);
        Optional<JobPosting> job = jobPostingRepository.findById(jobId);
        
        System.out.println("User found: " + user.isPresent());
        System.out.println("Job found: " + job.isPresent());
        
        if (user.isPresent() && job.isPresent()) {
            try {
                System.out.println("Creating Application entity...");
                Application application = new Application(
                    user.get(), 
                    job.get(), 
                    coverLetter,
                    applicantSkills,
                    applicantExperience,
                    applicantEducation
                );
                
                System.out.println("Application entity created, saving to database...");
                Application savedApplication = applicationRepository.save(application);
                System.out.println("✅ Application saved successfully with ID: " + savedApplication.getId());
                System.out.println("✅ Applicant Name: " + savedApplication.getApplicantName());
                System.out.println("✅ Applicant Email: " + savedApplication.getApplicantEmail());
                System.out.println("✅ Application Date: " + savedApplication.getApplicationDate());
                System.out.println("✅ Status: " + savedApplication.getStatus());
                
            } catch (Exception e) {
                System.out.println("❌ Error saving application: " + e.getMessage());
                e.printStackTrace();
                throw new JobApplicationException("Failed to save application: " + e.getMessage());
            }
        } else {
            String errorMsg = "User or Job not found with ID: User=" + userId + ", Job=" + jobId;
            System.out.println("❌ " + errorMsg);
            if (user.isEmpty()) {
                System.out.println("❌ User not found in database");
            }
            if (job.isEmpty()) {
                System.out.println("❌ Job not found in database");
            }
            throw new JobApplicationException(errorMsg);
        }
    }

    // ENHANCED: Apply for job with resume matching
    public Application applyForJobWithResume(Long userId, Long jobId, String coverLetter, 
                                            String applicantSkills, Integer applicantExperience, 
                                            String applicantEducation, String resumeFilePath, 
                                            String resumeParsedText) {
        System.out.println("=== JOB POSTING SERVICE - APPLY FOR JOB WITH RESUME ===");
        System.out.println("User ID: " + userId);
        System.out.println("Job ID: " + jobId);
        System.out.println("Resume File Path: " + resumeFilePath);
        
        Optional<User> user = userRepository.findById(userId);
        Optional<JobPosting> job = jobPostingRepository.findById(jobId);
        
        System.out.println("User found: " + user.isPresent());
        System.out.println("Job found: " + job.isPresent());
        
        if (user.isPresent() && job.isPresent()) {
            try {
                System.out.println("Creating Application entity with resume data...");
                Application application = new Application(
                    user.get(), 
                    job.get(), 
                    coverLetter,
                    applicantSkills,
                    applicantExperience,
                    applicantEducation
                );
                
                // Set resume data
                application.setResumeFilePath(resumeFilePath);
                application.setResumeParsedText(resumeParsedText);
                
                // Calculate match percentage
                double matchPercentage = calculateResumeMatchPercentage(job.get(), resumeParsedText, applicantSkills);
                application.setMatchPercentage(matchPercentage);
                
                // Calculate matched and missing skills
                Map<String, Object> skillsAnalysis = analyzeSkillsMatch(job.get(), applicantSkills, resumeParsedText);
                application.setMatchedSkills(objectMapper.writeValueAsString(skillsAnalysis.get("matchedSkills")));
                application.setMissingSkills(objectMapper.writeValueAsString(skillsAnalysis.get("missingSkills")));
                
                System.out.println("Application entity created, saving to database...");
                Application savedApplication = applicationRepository.save(application);
                System.out.println("✅ Application saved successfully with ID: " + savedApplication.getId());
                System.out.println("✅ Match Percentage: " + savedApplication.getMatchPercentage() + "%");
                System.out.println("✅ Applicant Name: " + savedApplication.getApplicantName());
                
                return savedApplication;
                
            } catch (Exception e) {
                System.out.println("❌ Error saving application: " + e.getMessage());
                e.printStackTrace();
                throw new JobApplicationException("Failed to save application: " + e.getMessage());
            }
        } else {
            String errorMsg = "User or Job not found with ID: User=" + userId + ", Job=" + jobId;
            System.out.println("❌ " + errorMsg);
            throw new JobApplicationException(errorMsg);
        }
    }

    // Calculate resume match percentage
    private double calculateResumeMatchPercentage(JobPosting job, String resumeText, String applicantSkillsJson) {
        try {
            System.out.println("=== CALCULATING RESUME MATCH PERCENTAGE ===");
            
            // Parse required skills from job
            Set<String> requiredSkills = parseSkillsFromJson(job.getRequiredSkills());
            Set<String> preferredSkills = parseSkillsFromJson(job.getPreferredSkills() != null ? job.getPreferredSkills() : "[]");
            
            // Parse applicant skills
            Set<String> applicantSkills = parseSkillsFromJson(applicantSkillsJson);
            
            // Extract skills from resume text
            Set<String> resumeSkills = extractSkillsFromText(resumeText);
            
            // Combine all applicant skills
            Set<String> allApplicantSkills = new HashSet<>();
            allApplicantSkills.addAll(applicantSkills);
            allApplicantSkills.addAll(resumeSkills);
            
            System.out.println("Required Skills: " + requiredSkills.size());
            System.out.println("Applicant Skills: " + allApplicantSkills.size());
            
            if (requiredSkills.isEmpty()) {
                return 50.0; // Default score if no required skills specified
            }
            
            // Calculate match for required skills (70% weight)
            double requiredMatchScore = calculateSkillsMatchScore(requiredSkills, allApplicantSkills);
            
            // Calculate match for preferred skills (30% weight)
            double preferredMatchScore = preferredSkills.isEmpty() ? 1.0 : calculateSkillsMatchScore(preferredSkills, allApplicantSkills);
            
            // Calculate experience match (bonus)
            double experienceBonus = calculateExperienceBonus(job, allApplicantSkills);
            
            double totalScore = (requiredMatchScore * 0.7) + (preferredMatchScore * 0.3) + experienceBonus;
            
            // Convert to percentage and cap at 100%
            double percentage = Math.min(totalScore * 100, 100.0);
            
            System.out.println("Required Match: " + (requiredMatchScore * 100) + "%");
            System.out.println("Preferred Match: " + (preferredMatchScore * 100) + "%");
            System.out.println("Experience Bonus: " + (experienceBonus * 100) + "%");
            System.out.println("Final Match Percentage: " + percentage + "%");
            
            return percentage;
            
        } catch (Exception e) {
            System.out.println("Error calculating match percentage: " + e.getMessage());
            return 0.0;
        }
    }

    // Calculate skills match score
    private double calculateSkillsMatchScore(Set<String> jobSkills, Set<String> applicantSkills) {
        if (jobSkills.isEmpty()) return 0.0;
        
        long matchedCount = jobSkills.stream()
            .filter(jobSkill -> applicantSkills.stream()
                .anyMatch(applicantSkill -> calculateSkillSimilarity(jobSkill, applicantSkill) > 0.7))
            .count();
        
        return (double) matchedCount / jobSkills.size();
    }

    // Calculate skill similarity
    private double calculateSkillSimilarity(String skill1, String skill2) {
        String s1 = skill1.toLowerCase();
        String s2 = skill2.toLowerCase();
        
        // Exact match
        if (s1.equals(s2)) return 1.0;
        
        // Contains match
        if (s1.contains(s2) || s2.contains(s1)) return 0.9;
        
        // Common variations
        if ((s1.equals("js") && s2.equals("javascript")) || 
            (s2.equals("js") && s1.equals("javascript"))) return 0.95;
        
        if ((s1.equals("react") && s2.equals("reactjs")) || 
            (s2.equals("react") && s1.equals("reactjs"))) return 0.9;
        
        return 0.0;
    }

    // Extract skills from text
    private Set<String> extractSkillsFromText(String text) {
        Set<String> skills = new HashSet<>();
        if (text == null || text.trim().isEmpty()) return skills;
        
        String lowerText = text.toLowerCase();
        
        // Common technical skills to look for
        Set<String> technicalSkills = Set.of(
            "java", "python", "javascript", "typescript", "react", "angular", "vue", "node.js",
            "spring boot", "spring", "hibernate", "mysql", "postgresql", "mongodb",
            "aws", "azure", "docker", "kubernetes", "jenkins", "git", "rest api",
            "microservices", "machine learning", "ai", "data analysis", "sql",
            "html", "css", "express.js", "django", "flask", "c++", "c#", "ruby"
        );
        
        for (String skill : technicalSkills) {
            if (lowerText.contains(skill)) {
                skills.add(skill);
            }
        }
        
        return skills;
    }

    // Calculate experience bonus
    private double calculateExperienceBonus(JobPosting job, Set<String> applicantSkills) {
        // Simple bonus based on seniority indicators in skills
        long seniorSkills = applicantSkills.stream()
            .filter(skill -> Set.of("architecture", "leadership", "management", "mentoring", 
                                   "kubernetes", "docker", "aws", "azure", "microservices")
                    .contains(skill))
            .count();
        
        return Math.min(seniorSkills * 0.05, 0.15); // Max 15% bonus
    }

    // Analyze skills match
    private Map<String, Object> analyzeSkillsMatch(JobPosting job, String applicantSkillsJson, String resumeText) {
        Map<String, Object> analysis = new HashMap<>();
        
        try {
            Set<String> requiredSkills = parseSkillsFromJson(job.getRequiredSkills());
            Set<String> preferredSkills = parseSkillsFromJson(job.getPreferredSkills() != null ? job.getPreferredSkills() : "[]");
            Set<String> applicantSkills = parseSkillsFromJson(applicantSkillsJson);
            Set<String> resumeSkills = extractSkillsFromText(resumeText);
            
            // Combine all applicant skills
            Set<String> allApplicantSkills = new HashSet<>();
            allApplicantSkills.addAll(applicantSkills);
            allApplicantSkills.addAll(resumeSkills);
            
            // Find matched skills
            Set<String> matchedSkills = requiredSkills.stream()
                .filter(jobSkill -> allApplicantSkills.stream()
                    .anyMatch(applicantSkill -> calculateSkillSimilarity(jobSkill, applicantSkill) > 0.7))
                .collect(Collectors.toSet());
            
            // Find missing skills
            Set<String> missingSkills = requiredSkills.stream()
                .filter(jobSkill -> matchedSkills.stream()
                    .noneMatch(matchedSkill -> calculateSkillSimilarity(jobSkill, matchedSkill) > 0.7))
                .collect(Collectors.toSet());
            
            analysis.put("matchedSkills", matchedSkills);
            analysis.put("missingSkills", missingSkills);
            analysis.put("applicantSkills", allApplicantSkills);
            
        } catch (Exception e) {
            System.out.println("Error analyzing skills match: " + e.getMessage());
            analysis.put("matchedSkills", new HashSet<>());
            analysis.put("missingSkills", new HashSet<>());
            analysis.put("applicantSkills", new HashSet<>());
        }
        
        return analysis;
    }

    // Parse skills from JSON
    private Set<String> parseSkillsFromJson(String skillsJson) {
        try {
            if (skillsJson != null && !skillsJson.isEmpty()) {
                return objectMapper.readValue(skillsJson, new TypeReference<Set<String>>() {});
            }
        } catch (Exception e) {
            System.out.println("Failed to parse skills JSON: " + skillsJson);
        }
        return new HashSet<>();
    }

    // Add these methods to your existing JobPostingService class
    public Application updateApplicationStatus(Long applicationId, Long recruiterId, String newStatus, String notes) {
        Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
        if (applicationOpt.isEmpty()) {
            throw new JobPostingException("Application not found with ID: " + applicationId);
        }
        
        Application application = applicationOpt.get();
        
        // Verify the recruiter owns this job
        if (application.getJobPosting().getRecruiter() == null || 
            !application.getJobPosting().getRecruiter().getId().equals(recruiterId)) {
            throw new JobPostingException("You are not authorized to update this application");
        }
        
        application.setStatus(newStatus);
        application.setRecruiterNotes(notes);
        application.setStatusUpdatedAt(LocalDateTime.now());
        
        return applicationRepository.save(application);
    }

    public Application markApplicationAsViewed(Long applicationId, Long recruiterId) {
        Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
        if (applicationOpt.isEmpty()) {
            throw new JobPostingException("Application not found with ID: " + applicationId);
        }
        
        Application application = applicationOpt.get();
        
        // Verify the recruiter owns this job
        if (application.getJobPosting().getRecruiter() == null || 
            !application.getJobPosting().getRecruiter().getId().equals(recruiterId)) {
            throw new JobPostingException("You are not authorized to update this application");
        }
        
        application.markAsViewed();
        
        return applicationRepository.save(application);
    }
    
    // Keep the old method for backward compatibility
    public void applyForJob(Long userId, Long jobId, String coverLetter) {
        applyForJob(userId, jobId, coverLetter, null, null, null);
    }
    
    // New methods for recruiter functionality
    
    public JobPosting createJobPosting(Long recruiterId, String title, String company, String description,
                                      String requiredSkills, String preferredSkills, String location,
                                      String salaryRange, String jobType, String experienceLevel) {
        Optional<Recruiter> recruiter = recruiterRepository.findById(recruiterId);
        if (recruiter.isEmpty()) {
            throw new JobPostingException("Recruiter not found with ID: " + recruiterId);
        }
        
        JobPosting jobPosting = new JobPosting();
        jobPosting.setTitle(title);
        jobPosting.setCompany(company);
        jobPosting.setDescription(description);
        jobPosting.setRequiredSkills(requiredSkills);
        jobPosting.setPreferredSkills(preferredSkills);
        jobPosting.setLocation(location);
        jobPosting.setSalaryRange(salaryRange);
        jobPosting.setJobType(jobType);
        jobPosting.setExperienceLevel(experienceLevel);
        jobPosting.setRecruiter(recruiter.get());
        jobPosting.setIsActive(true);
        
        return jobPostingRepository.save(jobPosting);
    }
    
    public JobPosting updateJobPosting(Long jobId, Long recruiterId, String title, String description,
                                      String requiredSkills, String preferredSkills, String location,
                                      String salaryRange, String jobType, String experienceLevel, Boolean isActive) {
        Optional<JobPosting> jobOpt = jobPostingRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            throw new JobPostingException("Job posting not found with ID: " + jobId);
        }
        
        JobPosting jobPosting = jobOpt.get();
        
        // Verify the recruiter owns this job posting
        if (jobPosting.getRecruiter() == null || !jobPosting.getRecruiter().getId().equals(recruiterId)) {
            throw new JobPostingException("You are not authorized to update this job posting");
        }
        
        jobPosting.setTitle(title);
        jobPosting.setDescription(description);
        jobPosting.setRequiredSkills(requiredSkills);
        jobPosting.setPreferredSkills(preferredSkills);
        jobPosting.setLocation(location);
        jobPosting.setSalaryRange(salaryRange);
        jobPosting.setJobType(jobType);
        jobPosting.setExperienceLevel(experienceLevel);
        
        if (isActive != null) {
            jobPosting.setIsActive(isActive);
        }
        
        return jobPostingRepository.save(jobPosting);
    }
    
    public void deleteJobPosting(Long jobId, Long recruiterId) {
        Optional<JobPosting> jobOpt = jobPostingRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            throw new JobPostingException("Job posting not found with ID: " + jobId);
        }
        
        JobPosting jobPosting = jobOpt.get();
        
        // Verify the recruiter owns this job posting
        if (jobPosting.getRecruiter() == null || !jobPosting.getRecruiter().getId().equals(recruiterId)) {
            throw new JobPostingException("You are not authorized to delete this job posting");
        }
        
        jobPostingRepository.delete(jobPosting);
    }
    
    public List<JobPosting> getJobsByRecruiter(Long recruiterId) {
        return jobPostingRepository.findByRecruiterIdAndIsActiveTrue(recruiterId);
    }
    
    // ENHANCED METHOD - FIXED VERSION
    public List<Application> getApplicationsForJob(Long jobId, Long recruiterId) {
        System.out.println("=== GET APPLICATIONS FOR JOB - ENHANCED ===");
        System.out.println("Job ID: " + jobId);
        System.out.println("Recruiter ID: " + recruiterId);
        
        Optional<JobPosting> jobOpt = jobPostingRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            System.out.println("❌ Job posting not found with ID: " + jobId);
            throw new JobPostingException("Job posting not found with ID: " + jobId);
        }
        
        JobPosting jobPosting = jobOpt.get();
        System.out.println("✅ Found job: " + jobPosting.getTitle() + " by " + jobPosting.getCompany());
        
        // Check if job has recruiter
        if (jobPosting.getRecruiter() == null) {
            System.out.println("❌ Job has no recruiter assigned. Job ID: " + jobId);
            System.out.println("⚠️  This might be a sample job created during initialization");
            // Return empty list instead of throwing exception for sample jobs
            return List.of();
        }
        
        System.out.println("Job Recruiter ID: " + jobPosting.getRecruiter().getId());
        System.out.println("Requested Recruiter ID: " + recruiterId);
        
        // Verify the recruiter owns this job posting
        if (!jobPosting.getRecruiter().getId().equals(recruiterId)) {
            System.out.println("❌ Recruiter ID mismatch. Job belongs to recruiter: " + 
                jobPosting.getRecruiter().getId() + ", but requested: " + recruiterId);
            throw new JobPostingException("You are not authorized to view applications for this job");
        }
        
        System.out.println("✅ Recruiter verification passed");
        
        // Get applications for this job
        List<Application> applications = applicationRepository.findByJobPostingId(jobId);
        System.out.println("✅ Found " + applications.size() + " applications for job " + jobId);
        
        // Debug: Print application details
        for (Application app : applications) {
            System.out.println("📄 Application ID: " + app.getId());
            System.out.println("   Applicant: " + (app.getUser() != null ? app.getUser().getFullName() : "No user"));
            System.out.println("   Application Date: " + app.getApplicationDate());
            System.out.println("   Status: " + app.getStatus());
            System.out.println("   Cover Letter Length: " + (app.getCoverLetter() != null ? app.getCoverLetter().length() : 0));
            System.out.println("   ---");
        }
        
        return applications;
    }
    
    // Add this method to JobPostingService class
    public List<JobPosting> getActiveJobPostingsSortedByDate() {
        return jobPostingRepository.findByIsActiveTrueOrderByPostedDateDesc();
    }
    
    // NEW METHOD: Get applications without recruiter verification (for debugging)
    public List<Application> getApplicationsForJobDebug(Long jobId) {
        System.out.println("=== DEBUG: GET APPLICATIONS WITHOUT RECRUITER VERIFICATION ===");
        System.out.println("Job ID: " + jobId);
        
        List<Application> applications = applicationRepository.findByJobPostingId(jobId);
        System.out.println("Found " + applications.size() + " applications for job " + jobId);
        
        return applications;
    }
    
    // Helper method to check if user is recruiter
    public boolean isUserRecruiter(Long userId) {
        Optional<Recruiter> recruiter = recruiterRepository.findByUserId(userId);
        return recruiter.isPresent();
    }
    
    // Helper method to get recruiter by user ID
    public Optional<Recruiter> getRecruiterByUserId(Long userId) {
        return recruiterRepository.findByUserId(userId);
    }

    // Enhanced method to get applications with resume data
    public List<Application> getApplicationsWithResumeAnalysis(Long jobId, Long recruiterId) {
        System.out.println("=== GET APPLICATIONS WITH RESUME ANALYSIS ===");
        
        List<Application> applications = getApplicationsForJob(jobId, recruiterId);
        
        // Enhance applications with resume analysis
        return applications.stream()
            .map(application -> {
                if (application.getResumeParsedText() != null && !application.getResumeParsedText().isEmpty()) {
                    // Recalculate match percentage if needed
                    if (application.getMatchPercentage() == null || application.getMatchPercentage() == 0.0) {
                        double matchPercentage = calculateResumeMatchPercentage(
                            application.getJobPosting(),
                            application.getResumeParsedText(),
                            application.getApplicantSkills()
                        );
                        application.setMatchPercentage(matchPercentage);
                        
                        // Save the updated application
                        applicationRepository.save(application);
                    }
                }
                return application;
            })
            .sorted((a1, a2) -> {
                Double score1 = a1.getMatchPercentage() != null ? a1.getMatchPercentage() : 0.0;
                Double score2 = a2.getMatchPercentage() != null ? a2.getMatchPercentage() : 0.0;
                return Double.compare(score2, score1);
            })
            .collect(Collectors.toList());
    }

    // Method to get application resume text
    public String getApplicationResumeText(Long applicationId) {
        Optional<Application> application = applicationRepository.findById(applicationId);
        return application.map(Application::getResumeParsedText).orElse("");
    }

    // Method to update match percentage for existing applications
    public void recalculateAllMatchPercentages(Long jobId) {
        List<Application> applications = applicationRepository.findByJobPostingId(jobId);
        
        for (Application application : applications) {
            if (application.getResumeParsedText() != null && !application.getResumeParsedText().isEmpty()) {
                double newMatchPercentage = calculateResumeMatchPercentage(
                    application.getJobPosting(),
                    application.getResumeParsedText(),
                    application.getApplicantSkills()
                );
                application.setMatchPercentage(newMatchPercentage);
                applicationRepository.save(application);
                
                System.out.println("Recalculated match for application " + application.getId() + 
                                 ": " + newMatchPercentage + "%");
            }
        }
    }
    
    // Exception classes
    public static class JobApplicationException extends RuntimeException {
        public JobApplicationException(String message) {
            super(message);
        }
    }
    
    public static class JobPostingException extends RuntimeException {
        public JobPostingException(String message) {
            super(message);
        }
    }
}