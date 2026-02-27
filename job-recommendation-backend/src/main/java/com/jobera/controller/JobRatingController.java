package com.jobera.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jobera.entity.JobRating;
import com.jobera.service.JobRatingService;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "http://localhost:3000")
public class JobRatingController {
    
    private final JobRatingService jobRatingService;
    
    public JobRatingController(JobRatingService jobRatingService) {
        this.jobRatingService = jobRatingService;
    }
    
    @PostMapping("/{jobId}/rate/{userId}")
    public ResponseEntity<Map<String, Object>> rateJob(
            @PathVariable Long jobId,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> ratingRequest) {
        try {
            Integer rating = (Integer) ratingRequest.get("rating");
            String comment = (String) ratingRequest.get("comment");
            
            JobRating jobRating = jobRatingService.rateJob(userId, jobId, rating, comment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Job rated successfully");
            response.put("rating", createRatingResponse(jobRating));
            
            return ResponseEntity.ok(response);
            
        } catch (JobRatingService.RatingException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to rate job: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{jobId}/ratings")
    public ResponseEntity<List<JobRating>> getJobRatings(@PathVariable Long jobId) {
        try {
            List<JobRating> ratings = jobRatingService.getJobRatings(jobId);
            return ResponseEntity.ok(ratings);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }
    
    @GetMapping("/{jobId}/rating/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserRating(
            @PathVariable Long jobId,
            @PathVariable Long userId) {
        try {
            Optional<JobRating> userRating = jobRatingService.getUserRating(userId, jobId);
            
            Map<String, Object> response = new HashMap<>();
            if (userRating.isPresent()) {
                response.put("hasRated", true);
                response.put("rating", createRatingResponse(userRating.get()));
            } else {
                response.put("hasRated", false);
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("hasRated", false));
        }
    }
    
    @GetMapping("/{jobId}/rating/stats")
    public ResponseEntity<Map<String, Object>> getJobRatingStats(@PathVariable Long jobId) {
        try {
            Map<String, Object> stats = jobRatingService.getJobRatingStats(jobId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("averageRating", 0.0, "totalRatings", 0));
        }
    }
    
    @DeleteMapping("/ratings/{ratingId}")
    public ResponseEntity<Map<String, String>> deleteRating(@PathVariable Long ratingId) {
        try {
            jobRatingService.deleteRating(ratingId);
            return ResponseEntity.ok(Map.of("message", "Rating deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete rating: " + e.getMessage()));
        }
    }
    
    private Map<String, Object> createRatingResponse(JobRating rating) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", rating.getId());
        response.put("rating", rating.getRating());
        response.put("comment", rating.getComment());
        response.put("ratedAt", rating.getRatedAt());
        
        if (rating.getUser() != null) {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", rating.getUser().getId());
            userInfo.put("fullName", rating.getUser().getFullName());
            userInfo.put("email", rating.getUser().getEmail());
            response.put("user", userInfo);
        }
        
        if (rating.getJobPosting() != null) {
            Map<String, Object> jobInfo = new HashMap<>();
            jobInfo.put("id", rating.getJobPosting().getId());
            jobInfo.put("title", rating.getJobPosting().getTitle());
            response.put("job", jobInfo);
        }
        
        return response;
    }
}