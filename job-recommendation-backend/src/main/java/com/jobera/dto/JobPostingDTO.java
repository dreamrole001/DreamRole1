package com.jobera.dto;

import java.time.LocalDateTime;

public class JobPostingDTO {
    private Long id;
    private String title;
    private String company;
    private String description;
    private String requiredSkills;
    private String preferredSkills;
    private String location;
    private String salaryRange;
    private String jobType;
    private String experienceLevel;
    private LocalDateTime postedDate;
    private Boolean isActive;
    private Double averageRating;
    private Integer totalRatings;

    // Constructor from Entity
    public JobPostingDTO(com.jobera.entity.JobPosting job) {
        this.id = job.getId();
        this.title = job.getTitle();
        this.company = job.getCompany();
        this.description = job.getDescription();
        this.requiredSkills = job.getRequiredSkills();
        this.preferredSkills = job.getPreferredSkills();
        this.location = job.getLocation();
        this.salaryRange = job.getSalaryRange();
        this.jobType = job.getJobType();
        this.experienceLevel = job.getExperienceLevel();
        this.postedDate = job.getPostedDate();
        this.isActive = job.getIsActive();
        this.averageRating = job.getAverageRating();
        this.totalRatings = job.getTotalRatings();
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
}