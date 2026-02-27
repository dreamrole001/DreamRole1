package com.jobera.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "proctoring_violations")
public class ProctoringViolation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "test_assignment_id", nullable = false)
    private TestAssignment testAssignment;
    
    @Column(nullable = false)
    private String violationType; // NO_FACE, MULTIPLE_FACES, MOBILE_DETECTED, LOOKING_AWAY
    
    @Column(nullable = false)
    private String message;
    
    private Boolean critical;
    
    private LocalDateTime timestamp;
    
    private String imagePath; // Path to saved screenshot
    
    @Column(updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public ProctoringViolation() {}
    
    public ProctoringViolation(TestAssignment testAssignment, String violationType, 
                               String message, Boolean critical) {
        this.testAssignment = testAssignment;
        this.violationType = violationType;
        this.message = message;
        this.critical = critical;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public TestAssignment getTestAssignment() { return testAssignment; }
    public void setTestAssignment(TestAssignment testAssignment) { this.testAssignment = testAssignment; }
    
    public String getViolationType() { return violationType; }
    public void setViolationType(String violationType) { this.violationType = violationType; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Boolean getCritical() { return critical; }
    public void setCritical(Boolean critical) { this.critical = critical; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}