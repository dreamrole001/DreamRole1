package com.jobera.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jobera.entity.JobRating;

@Repository
public interface JobRatingRepository extends JpaRepository<JobRating, Long> {
    Optional<JobRating> findByUserIdAndJobPostingId(Long userId, Long jobPostingId);
    List<JobRating> findByJobPostingId(Long jobPostingId);
    List<JobRating> findByUserId(Long userId);
    
    @Query("SELECT AVG(jr.rating) FROM JobRating jr WHERE jr.jobPosting.id = :jobPostingId")
    Double findAverageRatingByJobPostingId(@Param("jobPostingId") Long jobPostingId);
    
    @Query("SELECT COUNT(jr) FROM JobRating jr WHERE jr.jobPosting.id = :jobPostingId")
    Long countRatingsByJobPostingId(@Param("jobPostingId") Long jobPostingId);
    
    boolean existsByUserIdAndJobPostingId(Long userId, Long jobPostingId);
}