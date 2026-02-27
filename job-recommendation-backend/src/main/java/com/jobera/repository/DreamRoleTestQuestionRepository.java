// File: src/main/java/com/jobera/repository/DreamRoleTestQuestionRepository.java
package com.jobera.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.jobera.entity.DreamRoleTestQuestion;

@Repository
public interface DreamRoleTestQuestionRepository extends JpaRepository<DreamRoleTestQuestion, Long> {
    List<DreamRoleTestQuestion> findByDreamRoleTestId(Long dreamRoleTestId);
}