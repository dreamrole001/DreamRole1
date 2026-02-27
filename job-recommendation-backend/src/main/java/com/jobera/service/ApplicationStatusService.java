package com.jobera.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jobera.entity.Application;
import com.jobera.repository.ApplicationRepository;

@Service
public class ApplicationStatusService {
    
    private final ApplicationRepository applicationRepository;
    
    public ApplicationStatusService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }
    
    public Application updateApplicationStatus(Long applicationId, String newStatus, String recruiterNotes) {
        Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
        if (applicationOpt.isEmpty()) {
            throw new ApplicationStatusException("Application not found with ID: " + applicationId);
        }
        
        Application application = applicationOpt.get();
        application.setStatus(newStatus);
        application.setRecruiterNotes(recruiterNotes);
        application.setStatusUpdatedAt(LocalDateTime.now());
        
        return applicationRepository.save(application);
    }
    
    public Application markAsViewed(Long applicationId) {
        Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
        if (applicationOpt.isEmpty()) {
            throw new ApplicationStatusException("Application not found with ID: " + applicationId);
        }
        
        Application application = applicationOpt.get();
        application.markAsViewed();
        
        return applicationRepository.save(application);
    }
    
    public Application shortlistApplication(Long applicationId, String notes) {
        Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
        if (applicationOpt.isEmpty()) {
            throw new ApplicationStatusException("Application not found with ID: " + applicationId);
        }
        
        Application application = applicationOpt.get();
        application.shortlist();
        application.setRecruiterNotes(notes);
        
        return applicationRepository.save(application);
    }
    
    public Application scheduleInterview(Long applicationId, LocalDateTime interviewDate, 
                                       String interviewLocation, String notes) {
        Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
        if (applicationOpt.isEmpty()) {
            throw new ApplicationStatusException("Application not found with ID: " + applicationId);
        }
        
        Application application = applicationOpt.get();
        application.scheduleInterview(interviewDate, interviewLocation);
        application.setRecruiterNotes(notes);
        
        return applicationRepository.save(application);
    }
    
    public Application rejectApplication(Long applicationId, String rejectionReason) {
        Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
        if (applicationOpt.isEmpty()) {
            throw new ApplicationStatusException("Application not found with ID: " + applicationId);
        }
        
        Application application = applicationOpt.get();
        application.reject();
        application.setRecruiterNotes(rejectionReason);
        
        return applicationRepository.save(application);
    }
    
    public Application sendOffer(Long applicationId, String offerDetails) {
        Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
        if (applicationOpt.isEmpty()) {
            throw new ApplicationStatusException("Application not found with ID: " + applicationId);
        }
        
        Application application = applicationOpt.get();
        application.sendOffer();
        application.setRecruiterNotes(offerDetails);
        
        return applicationRepository.save(application);
    }
    
    public List<Application> getUserApplications(Long userId) {
        try {
            System.out.println("=== GETTING APPLICATIONS FOR USER: " + userId + " ===");
            List<Application> applications = applicationRepository.findByUserId(userId);
            System.out.println("✅ Found " + applications.size() + " applications");
            
            // Debug: Print each application
            for (Application app : applications) {
                System.out.println("📄 Application ID: " + app.getId());
                System.out.println("   Job: " + (app.getJobPosting() != null ? app.getJobPosting().getTitle() : "No job"));
                System.out.println("   Status: " + app.getStatus());
                System.out.println("   Applicant: " + app.getSafeApplicantName());
            }
            
            return applications;
        } catch (Exception e) {
            System.out.println("❌ ERROR in getUserApplications: " + e.getMessage());
            e.printStackTrace();
            throw new ApplicationStatusException("Failed to get user applications: " + e.getMessage());
        }
    }
    
    public List<Application> getJobApplications(Long jobId) {
        return applicationRepository.findByJobPostingId(jobId);
    }
    
    public static class ApplicationStatusException extends RuntimeException {
        public ApplicationStatusException(String message) {
            super(message);
        }
    }
}