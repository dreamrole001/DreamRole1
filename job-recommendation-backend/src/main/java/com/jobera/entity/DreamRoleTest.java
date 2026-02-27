// File: src/main/java/com/jobera/entity/DreamRoleTest.java
package com.jobera.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "dream_role_tests")
public class DreamRoleTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "recruiter_id", nullable = false)
    private Recruiter recruiter;
    
    @Column(nullable = false)
    private String testName;
    
    private String description;
    
    @Column(nullable = false)
    private Integer durationMinutes = 60;
    
    @Column(nullable = false)
    private Integer totalQuestions = 50;
    
    @Column(nullable = false)
    private Integer aptitudeQuestions = 40;
    
    @Column(nullable = false)
    private Integer technicalQuestions = 10;
    
    @Column(nullable = false)
    private Integer passingScore = 60;
    
    @Column(nullable = false)
    private String targetBranch;
    
    @Column(nullable = false)
    private Boolean shuffleQuestions = true;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "dreamRoleTest", cascade = CascadeType.ALL)
    private List<DreamRoleTestQuestion> technicalQuestionsList;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Recruiter getRecruiter() { return recruiter; }
    public void setRecruiter(Recruiter recruiter) { this.recruiter = recruiter; }
    
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    
    public Integer getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(Integer totalQuestions) { this.totalQuestions = totalQuestions; }
    
    public Integer getAptitudeQuestions() { return aptitudeQuestions; }
    public void setAptitudeQuestions(Integer aptitudeQuestions) { this.aptitudeQuestions = aptitudeQuestions; }
    
    public Integer getTechnicalQuestions() { return technicalQuestions; }
    public void setTechnicalQuestions(Integer technicalQuestions) { this.technicalQuestions = technicalQuestions; }
    
    public Integer getPassingScore() { return passingScore; }
    public void setPassingScore(Integer passingScore) { this.passingScore = passingScore; }
    
    public String getTargetBranch() { return targetBranch; }
    public void setTargetBranch(String targetBranch) { this.targetBranch = targetBranch; }
    
    public Boolean getShuffleQuestions() { return shuffleQuestions; }
    public void setShuffleQuestions(Boolean shuffleQuestions) { this.shuffleQuestions = shuffleQuestions; }
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<DreamRoleTestQuestion> getTechnicalQuestionsList() { return technicalQuestionsList; }
    public void setTechnicalQuestionsList(List<DreamRoleTestQuestion> technicalQuestionsList) { this.technicalQuestionsList = technicalQuestionsList; }
}