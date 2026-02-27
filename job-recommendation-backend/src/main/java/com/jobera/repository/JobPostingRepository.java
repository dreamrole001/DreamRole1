// File: src/main/java/com/jobera/repository/JobPostingRepository.java
package com.jobera.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jobera.entity.JobPosting;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findByIsActiveTrue();
    List<JobPosting> findByIsActiveTrueOrderByPostedDateDesc();
    List<JobPosting> findByRecruiterIdAndIsActiveTrue(Long recruiterId);
    
    @Query("SELECT jp FROM JobPosting jp WHERE jp.isActive = true AND " +
           "(LOWER(jp.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(jp.company) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(jp.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<JobPosting> searchActiveJobs(@Param("keyword") String keyword);
    
    List<JobPosting> findByLocationContainingAndIsActiveTrue(String location);
}