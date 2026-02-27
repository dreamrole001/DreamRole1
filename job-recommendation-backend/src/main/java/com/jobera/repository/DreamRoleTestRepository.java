// File: src/main/java/com/jobera/repository/DreamRoleTestRepository.java
package com.jobera.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobera.entity.DreamRoleTest;

@Repository
public interface DreamRoleTestRepository extends JpaRepository<DreamRoleTest, Long> {
    List<DreamRoleTest> findByRecruiterId(Long recruiterId);
    List<DreamRoleTest> findByRecruiterIdAndIsActiveTrue(Long recruiterId);
}