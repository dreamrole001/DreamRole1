package com.jobera.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_assignments")
public class TestAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    private AptitudeTest test;
    
    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;
    
    @Column(nullable = false)
    private LocalDateTime assignedAt;
    
    private LocalDateTime startedAt;
    
    private LocalDateTime completedAt;
    
    @Column(nullable = false)
    private LocalDateTime deadline;
    
    private Integer score;
    
    private Integer correctAnswers;
    
    private Integer totalQuestions;
    
    private String status = "PENDING";
    
    @Column(columnDefinition = "JSON")
    private String answers;
    
    private Boolean passed = false;
    
    // ========== NEW PROCTORING FIELDS ==========
    
    @Column(name = "proctoring_active")
    private Boolean proctoringActive = false;
    
    @Column(name = "proctoring_terminated")
    private Boolean proctoringTerminated = false;
    
    @Column(name = "proctoring_termination_reason", length = 500)
    private String proctoringTerminationReason;
    
    @Column(name = "warning_count")
    private Integer warningCount = 0;
    
    @Column(name = "no_face_violations")
    private Integer noFaceViolations = 0;
    
    @Column(name = "multiple_faces_violations")
    private Integer multipleFacesViolations = 0;
    
    @Column(name = "mobile_detected_violations")
    private Integer mobileDetectedViolations = 0;
    
    @Column(name = "looking_away_violations")
    private Integer lookingAwayViolations = 0;
    
    @Column(name = "proctoring_started_at")
    private LocalDateTime proctoringStartedAt;
    
    @Column(name = "proctoring_ended_at")
    private LocalDateTime proctoringEndedAt;
    
    // ========== RELATIONSHIPS ==========
    
    @OneToMany(mappedBy = "testAssignment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ProctoringViolation> proctoringViolations;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (proctoringActive == null) proctoringActive = false;
        if (proctoringTerminated == null) proctoringTerminated = false;
        if (warningCount == null) warningCount = 0;
        if (noFaceViolations == null) noFaceViolations = 0;
        if (multipleFacesViolations == null) multipleFacesViolations = 0;
        if (mobileDetectedViolations == null) mobileDetectedViolations = 0;
        if (lookingAwayViolations == null) lookingAwayViolations = 0;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // ========== GETTERS AND SETTERS ==========
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public AptitudeTest getTest() { return test; }
    public void setTest(AptitudeTest test) { this.test = test; }
    
    public Application getApplication() { return application; }
    public void setApplication(Application application) { this.application = application; }
    
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    
    public LocalDateTime getDeadline() { return deadline; }
    public void setDeadline(LocalDateTime deadline) { this.deadline = deadline; }
    
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    
    public Integer getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(Integer correctAnswers) { this.correctAnswers = correctAnswers; }
    
    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getAnswers() { return answers; }
    public void setAnswers(String answers) { this.answers = answers; }
    
    public Boolean getPassed() { return passed; }
    public void setPassed(Boolean passed) { this.passed = passed; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // ========== PROCTORING GETTERS AND SETTERS ==========
    
    public Boolean getProctoringActive() { return proctoringActive; }
    public void setProctoringActive(Boolean proctoringActive) { this.proctoringActive = proctoringActive; }
    
    public Boolean getProctoringTerminated() { return proctoringTerminated; }
    public void setProctoringTerminated(Boolean proctoringTerminated) { this.proctoringTerminated = proctoringTerminated; }
    
    public String getProctoringTerminationReason() { return proctoringTerminationReason; }
    public void setProctoringTerminationReason(String proctoringTerminationReason) { this.proctoringTerminationReason = proctoringTerminationReason; }
    
    public Integer getWarningCount() { return warningCount; }
    public void setWarningCount(Integer warningCount) { this.warningCount = warningCount; }
    
    public Integer getNoFaceViolations() { return noFaceViolations; }
    public void setNoFaceViolations(Integer noFaceViolations) { this.noFaceViolations = noFaceViolations; }
    
    public Integer getMultipleFacesViolations() { return multipleFacesViolations; }
    public void setMultipleFacesViolations(Integer multipleFacesViolations) { this.multipleFacesViolations = multipleFacesViolations; }
    
    public Integer getMobileDetectedViolations() { return mobileDetectedViolations; }
    public void setMobileDetectedViolations(Integer mobileDetectedViolations) { this.mobileDetectedViolations = mobileDetectedViolations; }
    
    public Integer getLookingAwayViolations() { return lookingAwayViolations; }
    public void setLookingAwayViolations(Integer lookingAwayViolations) { this.lookingAwayViolations = lookingAwayViolations; }
    
    public LocalDateTime getProctoringStartedAt() { return proctoringStartedAt; }
    public void setProctoringStartedAt(LocalDateTime proctoringStartedAt) { this.proctoringStartedAt = proctoringStartedAt; }
    
    public LocalDateTime getProctoringEndedAt() { return proctoringEndedAt; }
    public void setProctoringEndedAt(LocalDateTime proctoringEndedAt) { this.proctoringEndedAt = proctoringEndedAt; }
    
    public List<ProctoringViolation> getProctoringViolations() { return proctoringViolations; }
    public void setProctoringViolations(List<ProctoringViolation> proctoringViolations) { this.proctoringViolations = proctoringViolations; }
    
    // ========== HELPER METHODS ==========
    
    public Integer getPercentage() {
        if (score != null && totalQuestions != null && totalQuestions > 0) {
            return (score * 100) / totalQuestions;
        }
        return 0;
    }
    
    public void incrementWarningCount() {
        if (this.warningCount == null) this.warningCount = 0;
        this.warningCount++;
    }
    
    public void incrementViolation(String type) {
        switch(type) {
            case "no_face":
                if (this.noFaceViolations == null) this.noFaceViolations = 0;
                this.noFaceViolations++;
                break;
            case "multiple_faces":
                if (this.multipleFacesViolations == null) this.multipleFacesViolations = 0;
                this.multipleFacesViolations++;
                break;
            case "mobile_detected":
                if (this.mobileDetectedViolations == null) this.mobileDetectedViolations = 0;
                this.mobileDetectedViolations++;
                break;
            case "looking_away":
                if (this.lookingAwayViolations == null) this.lookingAwayViolations = 0;
                this.lookingAwayViolations++;
                break;
        }
    }
    
    public void startProctoring() {
        this.proctoringActive = true;
        this.proctoringStartedAt = LocalDateTime.now();
        this.proctoringTerminated = false;
        this.warningCount = 0;
    }
    
    public void terminateProctoring(String reason) {
        this.proctoringActive = false;
        this.proctoringTerminated = true;
        this.proctoringTerminationReason = reason;
        this.proctoringEndedAt = LocalDateTime.now();
        this.status = "FAILED_PROCTORING";
        this.completedAt = LocalDateTime.now();
    }
    
    public void stopProctoring() {
        this.proctoringActive = false;
        this.proctoringEndedAt = LocalDateTime.now();
    }
    
    public boolean hasProctoringViolations() {
        return (noFaceViolations != null && noFaceViolations > 0) ||
               (multipleFacesViolations != null && multipleFacesViolations > 0) ||
               (mobileDetectedViolations != null && mobileDetectedViolations > 0) ||
               (lookingAwayViolations != null && lookingAwayViolations > 0);
    }
    
    public int getTotalViolations() {
        return (noFaceViolations != null ? noFaceViolations : 0) +
               (multipleFacesViolations != null ? multipleFacesViolations : 0) +
               (mobileDetectedViolations != null ? mobileDetectedViolations : 0) +
               (lookingAwayViolations != null ? lookingAwayViolations : 0);
    }
}