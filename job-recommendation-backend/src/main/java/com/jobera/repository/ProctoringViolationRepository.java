package com.jobera.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobera.entity.ProctoringViolation;

@Repository
public interface ProctoringViolationRepository extends JpaRepository<ProctoringViolation, Long> {
    List<ProctoringViolation> findByTestAssignmentId(Long testAssignmentId);
    List<ProctoringViolation> findByTestAssignmentIdAndCriticalTrue(Long testAssignmentId);
    void deleteByTestAssignmentId(Long testAssignmentId);
}