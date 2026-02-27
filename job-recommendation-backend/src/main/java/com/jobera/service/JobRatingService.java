package com.jobera.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.jobera.entity.JobPosting;
import com.jobera.entity.JobRating;
import com.jobera.entity.User;
import com.jobera.repository.JobPostingRepository;
import com.jobera.repository.JobRatingRepository;
import com.jobera.repository.UserRepository;

@Service
public class JobRatingService {
    
    private final JobRatingRepository jobRatingRepository;
    private final JobPostingRepository jobPostingRepository;
    private final UserRepository userRepository;
    
    public JobRatingService(JobRatingRepository jobRatingRepository,
                          JobPostingRepository jobPostingRepository,
                          UserRepository userRepository) {
        this.jobRatingRepository = jobRatingRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.userRepository = userRepository;
    }
    
    public JobRating rateJob(Long userId, Long jobId, Integer rating, String comment) {
        // Validate rating
        if (rating < 1 || rating > 5) {
            throw new RatingException("Rating must be between 1 and 5");
        }
        
        Optional<User> user = userRepository.findById(userId);
        Optional<JobPosting> job = jobPostingRepository.findById(jobId);
        
        if (user.isEmpty()) {
            throw new RatingException("User not found with ID: " + userId);
        }
        if (job.isEmpty()) {
            throw new RatingException("Job not found with ID: " + jobId);
        }
        
        // Check if user has already rated this job
        Optional<JobRating> existingRating = jobRatingRepository.findByUserIdAndJobPostingId(userId, jobId);
        
        JobRating jobRating;
        if (existingRating.isPresent()) {
            // Update existing rating
            jobRating = existingRating.get();
            jobRating.setRating(rating);
            jobRating.setComment(comment);
        } else {
            // Create new rating
            jobRating = new JobRating(user.get(), job.get(), rating, comment);
        }
        
        JobRating savedRating = jobRatingRepository.save(jobRating);
        
        // Update job's average rating and total ratings
        updateJobRatingStats(jobId);
        
        return savedRating;
    }
    
    private void updateJobRatingStats(Long jobId) {
        Double averageRating = jobRatingRepository.findAverageRatingByJobPostingId(jobId);
        Long totalRatings = jobRatingRepository.countRatingsByJobPostingId(jobId);
        
        Optional<JobPosting> job = jobPostingRepository.findById(jobId);
        if (job.isPresent()) {
            JobPosting jobPosting = job.get();
            jobPosting.setAverageRating(averageRating != null ? Math.round(averageRating * 100.0) / 100.0 : 0.0);
            jobPosting.setTotalRatings(totalRatings != null ? totalRatings.intValue() : 0);
            jobPostingRepository.save(jobPosting);
        }
    }
    
    public Optional<JobRating> getUserRating(Long userId, Long jobId) {
        return jobRatingRepository.findByUserIdAndJobPostingId(userId, jobId);
    }
    
    public List<JobRating> getJobRatings(Long jobId) {
        return jobRatingRepository.findByJobPostingId(jobId);
    }
    
    public List<JobRating> getUserRatings(Long userId) {
        return jobRatingRepository.findByUserId(userId);
    }
    
    public Map<String, Object> getJobRatingStats(Long jobId) {
        Double averageRating = jobRatingRepository.findAverageRatingByJobPostingId(jobId);
        Long totalRatings = jobRatingRepository.countRatingsByJobPostingId(jobId);
        
        return Map.of(
            "averageRating", averageRating != null ? Math.round(averageRating * 100.0) / 100.0 : 0.0,
            "totalRatings", totalRatings != null ? totalRatings : 0
        );
    }
    
    public boolean hasUserRatedJob(Long userId, Long jobId) {
        return jobRatingRepository.existsByUserIdAndJobPostingId(userId, jobId);
    }
    
    public void deleteRating(Long ratingId) {
        Optional<JobRating> rating = jobRatingRepository.findById(ratingId);
        if (rating.isPresent()) {
            Long jobId = rating.get().getJobPosting().getId();
            jobRatingRepository.deleteById(ratingId);
            updateJobRatingStats(jobId);
        }
    }
    
    public static class RatingException extends RuntimeException {
        public RatingException(String message) {
            super(message);
        }
    }
}