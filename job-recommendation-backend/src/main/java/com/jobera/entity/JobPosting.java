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
import jakarta.persistence.Table;

@Entity
@Table(name = "job_postings")
public class JobPosting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false)
    private String company;
    
    @Column(columnDefinition = "LONGTEXT")
    private String description;
    
    @Column(columnDefinition = "JSON")
    private String requiredSkills;
    
    @Column(columnDefinition = "JSON")
    private String preferredSkills;
    
    private String location;
    private String salaryRange;
    private String jobType;
    private String experienceLevel;
    
    @Column(updatable = false)
    private LocalDateTime postedDate;
    
    private Boolean isActive;
    
    // Fixed: Remove precision and scale for MySQL compatibility
    @Column(name = "average_rating")
    private Double averageRating = 0.0;
    
    @Column(name = "total_ratings")
    private Integer totalRatings = 0;
    
    // New field for recruiter relationship
    @ManyToOne
    @JoinColumn(name = "recruiter_id")
    @JsonIgnore
    private Recruiter recruiter;
    
    @OneToMany(mappedBy = "jobPosting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Application> applications;
    
    @OneToMany(mappedBy = "jobPosting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<JobRating> ratings;
    
    @PrePersist
    protected void onCreate() {
        postedDate = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (averageRating == null) {
            averageRating = 0.0;
        }
        if (totalRatings == null) {
            totalRatings = 0;
        }
    }
    
    // Constructors
    public JobPosting() {}
    
    public JobPosting(String title, String company, String description) {
        this.title = title;
        this.company = company;
        this.description = description;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String requiredSkills) { this.requiredSkills = requiredSkills; }
    public String getPreferredSkills() { return preferredSkills; }
    public void setPreferredSkills(String preferredSkills) { this.preferredSkills = preferredSkills; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }
    public String getJobType() { return jobType; }
    public void setJobType(String jobType) { this.jobType = jobType; }
    public String getExperienceLevel() { return experienceLevel; }
    public void setExperienceLevel(String experienceLevel) { this.experienceLevel = experienceLevel; }
    public LocalDateTime getPostedDate() { return postedDate; }
    public void setPostedDate(LocalDateTime postedDate) { this.postedDate = postedDate; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
    public Integer getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }
    
    @JsonIgnore
    public Recruiter getRecruiter() { return recruiter; }
    public void setRecruiter(Recruiter recruiter) { this.recruiter = recruiter; }
    
    @JsonIgnore
    public List<Application> getApplications() { return applications; }
    public void setApplications(List<Application> applications) { this.applications = applications; }
    
    @JsonIgnore
    public List<JobRating> getRatings() { return ratings; }
    public void setRatings(List<JobRating> ratings) { this.ratings = ratings; }
    
    // Helper method to format posted date
    public String getFormattedPostedDate() {
        return postedDate != null ? postedDate.toString() : "Unknown";
    }
    
    // Helper method to check if job is recent (within 7 days)
    public boolean isRecent() {
        return postedDate != null && 
               postedDate.isAfter(LocalDateTime.now().minusDays(7));
    }
}