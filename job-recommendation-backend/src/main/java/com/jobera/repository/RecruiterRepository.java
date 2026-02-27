// File: src/main/java/com/jobera/repository/RecruiterRepository.java
package com.jobera.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobera.entity.Recruiter;

@Repository
public interface RecruiterRepository extends JpaRepository<Recruiter, Long> {
    Optional<Recruiter> findByUserId(Long userId);
    Boolean existsByUserId(Long userId);
    Optional<Recruiter> findByCompanyName(String companyName);
}