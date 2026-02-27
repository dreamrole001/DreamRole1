// File: src/main/java/com/jobera/repository/ResumeRepository.java
package com.jobera.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobera.entity.Resume;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserId(Long userId);
    Optional<Resume> findFirstByUserIdOrderByUploadDateDesc(Long userId);
}