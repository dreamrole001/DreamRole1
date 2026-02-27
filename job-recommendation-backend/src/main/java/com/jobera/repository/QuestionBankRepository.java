// File: src/main/java/com/jobera/repository/QuestionBankRepository.java
package com.jobera.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.jobera.entity.QuestionBank;

@Repository
public interface QuestionBankRepository extends JpaRepository<QuestionBank, Long> {
    
    // Find by branch
    List<QuestionBank> findByBranchAndIsActiveTrue(String branch);
    
    // Count by branch
    long countByBranchAndIsActiveTrue(String branch);
    
    // Get all distinct branches
    @Query("SELECT DISTINCT q.branch FROM QuestionBank q WHERE q.isActive = true")
    List<String> findAllBranches();
    
    // Get random questions by branch - Using JPQL with PageRequest (NO NATIVE QUERY)
    @Query("SELECT q FROM QuestionBank q WHERE q.branch = :branch AND q.isActive = true ORDER BY FUNCTION('RAND')")
    List<QuestionBank> findRandomQuestionsByBranch(@Param("branch") String branch, org.springframework.data.domain.Pageable pageable);
    
    // Count all active questions
    @Query("SELECT COUNT(q) FROM QuestionBank q WHERE q.isActive = true")
    long countAllActive();
    
    // Find by category and branch
    List<QuestionBank> findByCategoryAndBranchAndIsActiveTrue(String category, String branch);
    
    // Find by difficulty and branch
    List<QuestionBank> findByDifficultyLevelAndBranchAndIsActiveTrue(String difficultyLevel, String branch);
    
    // Find least used questions by branch
    @Query("SELECT q FROM QuestionBank q WHERE q.isActive = true AND q.branch = :branch ORDER BY q.timesUsed ASC")
    List<QuestionBank> findLeastUsedByBranch(@Param("branch") String branch);
}