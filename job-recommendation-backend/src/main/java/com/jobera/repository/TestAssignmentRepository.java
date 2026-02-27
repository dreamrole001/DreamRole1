// File: src/main/java/com/jobera/repository/TestAssignmentRepository.java
package com.jobera.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jobera.entity.TestAssignment;

@Repository
public interface TestAssignmentRepository extends JpaRepository<TestAssignment, Long> {
    List<TestAssignment> findByApplicationId(Long applicationId);
    List<TestAssignment> findByTestId(Long testId);
    Optional<TestAssignment> findByApplicationIdAndTestId(Long applicationId, Long testId);
    
    @Query("SELECT ta FROM TestAssignment ta WHERE ta.application.jobPosting.id = :jobId")
    List<TestAssignment> findByJobId(@Param("jobId") Long jobId);
    
    @Query("SELECT ta FROM TestAssignment ta WHERE ta.application.jobPosting.id = :jobId AND ta.passed = true")
    List<TestAssignment> findPassedByJobId(@Param("jobId") Long jobId);
    
    @Query("SELECT ta FROM TestAssignment ta WHERE ta.test.recruiter.id = :recruiterId")
    List<TestAssignment> findByRecruiterId(@Param("recruiterId") Long recruiterId);
    
    @Query("SELECT AVG(ta.score) FROM TestAssignment ta WHERE ta.test.id = :testId AND ta.status = 'COMPLETED'")
    Double getAverageScoreForTest(@Param("testId") Long testId);
}