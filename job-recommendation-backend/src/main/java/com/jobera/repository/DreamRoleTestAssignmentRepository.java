// File: src/main/java/com/jobera/repository/DreamRoleTestAssignmentRepository.java
package com.jobera.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jobera.entity.DreamRoleTestAssignment;

@Repository
public interface DreamRoleTestAssignmentRepository extends JpaRepository<DreamRoleTestAssignment, Long> {
    List<DreamRoleTestAssignment> findByApplicationId(Long applicationId);
    List<DreamRoleTestAssignment> findByDreamRoleTestId(Long dreamRoleTestId);
    Optional<DreamRoleTestAssignment> findByApplicationIdAndDreamRoleTestId(Long applicationId, Long dreamRoleTestId);
    
    @Query("SELECT dta FROM DreamRoleTestAssignment dta WHERE dta.application.jobPosting.id = :jobId")
    List<DreamRoleTestAssignment> findByJobId(@Param("jobId") Long jobId);
    
    @Query("SELECT dta FROM DreamRoleTestAssignment dta WHERE dta.dreamRoleTest.recruiter.id = :recruiterId")
    List<DreamRoleTestAssignment> findByRecruiterId(@Param("recruiterId") Long recruiterId);
    
    @Query("SELECT dta FROM DreamRoleTestAssignment dta WHERE dta.application.user.id = :userId")
    List<DreamRoleTestAssignment> findByUserId(@Param("userId") Long userId);
}