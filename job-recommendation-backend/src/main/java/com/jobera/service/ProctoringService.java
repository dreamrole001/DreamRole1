package com.jobera.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobera.entity.ProctoringViolation;
import com.jobera.entity.TestAssignment;
import com.jobera.repository.ProctoringViolationRepository;
import com.jobera.repository.TestAssignmentRepository;

@Service
public class ProctoringService {
    
    private final ProctoringViolationRepository violationRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    
    public ProctoringService(ProctoringViolationRepository violationRepository,
                            TestAssignmentRepository testAssignmentRepository) {
        this.violationRepository = violationRepository;
        this.testAssignmentRepository = testAssignmentRepository;
    }
    
    @Transactional
    public void saveViolations(Long assignmentId, Map<String, Object> violationData) {
        Optional<TestAssignment> assignmentOpt = testAssignmentRepository.findById(assignmentId);
        if (assignmentOpt.isEmpty()) {
            throw new RuntimeException("Test assignment not found: " + assignmentId);
        }
        
        TestAssignment assignment = assignmentOpt.get();
        
        // Update warning count
        Integer warningCount = (Integer) violationData.get("warning_count");
        if (warningCount != null) {
            assignment.setWarningCount(warningCount);
        }
        
        // Check if test was terminated
        Boolean terminated = (Boolean) violationData.get("terminated");
        if (terminated != null && terminated) {
            assignment.setProctoringTerminated(true);
            assignment.setProctoringTerminationReason("Test terminated due to proctoring violations");
            assignment.setStatus("FAILED_PROCTORING");
            assignment.setCompletedAt(LocalDateTime.now());
        }
        
        // Save individual violations
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> warnings = (List<Map<String, Object>>) violationData.get("warnings");
        
        if (warnings != null) {
            for (Map<String, Object> warning : warnings) {
                ProctoringViolation violation = new ProctoringViolation(
                    assignment,
                    (String) warning.get("type"),
                    (String) warning.get("message"),
                    (Boolean) warning.get("critical")
                );
                violation.setTimestamp(LocalDateTime.parse((String) warning.get("timestamp")));
                violationRepository.save(violation);
            }
        }
        
        testAssignmentRepository.save(assignment);
    }
    
    public List<ProctoringViolation> getViolationsForAssignment(Long assignmentId) {
        return violationRepository.findByTestAssignmentId(assignmentId);
    }
    
    public List<ProctoringViolation> getCriticalViolationsForAssignment(Long assignmentId) {
        return violationRepository.findByTestAssignmentIdAndCriticalTrue(assignmentId);
    }
    
    public Map<String, Object> getViolationSummary(Long assignmentId) {
        List<ProctoringViolation> violations = getViolationsForAssignment(assignmentId);
        
        long noFaceCount = violations.stream()
            .filter(v -> "no_face".equals(v.getViolationType()))
            .count();
        long multipleFacesCount = violations.stream()
            .filter(v -> "multiple_faces".equals(v.getViolationType()))
            .count();
        long mobileDetectedCount = violations.stream()
            .filter(v -> "mobile_detected".equals(v.getViolationType()))
            .count();
        long criticalCount = violations.stream()
            .filter(ProctoringViolation::getCritical)
            .count();
        
        return Map.of(
            "total_violations", violations.size(),
            "no_face_violations", noFaceCount,
            "multiple_faces_violations", multipleFacesCount,
            "mobile_detected_violations", mobileDetectedCount,
            "critical_violations", criticalCount
        );
    }
}