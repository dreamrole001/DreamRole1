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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "job_ratings", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_posting_id"}))
public class JobRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;
    
    @Column(nullable = false)
    private Integer rating; // 1 to 5
    
    private String comment;
    
    @Column(updatable = false)
    private LocalDateTime ratedAt;
    
    @PrePersist
    protected void onCreate() {
        ratedAt = LocalDateTime.now();
    }
    
    // Constructors
    public JobRating() {}
    
    public JobRating(User user, JobPosting jobPosting, Integer rating) {
        this.user = user;
        this.jobPosting = jobPosting;
        this.rating = rating;
    }
    
    public JobRating(User user, JobPosting jobPosting, Integer rating, String comment) {
        this.user = user;
        this.jobPosting = jobPosting;
        this.rating = rating;
        this.comment = comment;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public JobPosting getJobPosting() { return jobPosting; }
    public void setJobPosting(JobPosting jobPosting) { this.jobPosting = jobPosting; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public LocalDateTime getRatedAt() { return ratedAt; }
    public void setRatedAt(LocalDateTime ratedAt) { this.ratedAt = ratedAt; }
}