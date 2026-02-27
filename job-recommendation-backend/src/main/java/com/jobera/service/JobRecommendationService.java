package com.jobera.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobera.entity.JobPosting;
import com.jobera.entity.Resume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobRecommendationService {
    
    private static final Logger logger = LoggerFactory.getLogger(JobRecommendationService.class);
    
    private final JobPostingService jobPostingService;
    private final ResumeService resumeService;
    private final ResumeProcessingService resumeProcessingService;
    private final ObjectMapper objectMapper;
    
    // Enhanced skill categories with comprehensive skill sets
    private final Map<String, Set<String>> skillCategories;
    
    // Skill importance weights (higher = more important)
    private final Map<String, Double> skillImportanceWeights;
    
    public JobRecommendationService(JobPostingService jobPostingService, 
                                   ResumeService resumeService,
                                   ResumeProcessingService resumeProcessingService) {
        this.jobPostingService = jobPostingService;
        this.resumeService = resumeService;
        this.resumeProcessingService = resumeProcessingService;
        this.objectMapper = new ObjectMapper();
        
        // Initialize enhanced skill categories
        this.skillCategories = initializeSkillCategories();
        
        // Initialize skill importance weights
        this.skillImportanceWeights = initializeSkillImportanceWeights();
    }
    
    private Map<String, Set<String>> initializeSkillCategories() {
        Map<String, Set<String>> categories = new HashMap<>();
        
        // Programming Languages
        categories.put("programming_languages", new HashSet<>(Arrays.asList(
            "java", "python", "javascript", "typescript", "c++", "c#", "ruby", "php",
            "swift", "kotlin", "go", "rust", "scala", "r", "matlab", "perl", "dart",
            "html", "css", "sql", "bash", "shell", "powershell"
        )));
        
        // Frontend Development
        categories.put("frontend", new HashSet<>(Arrays.asList(
            "react", "angular", "vue", "html", "css", "sass", "less", "bootstrap", 
            "tailwind", "webpack", "vite", "next.js", "nuxt.js", "jquery", "redux",
            "vuex", "material-ui", "ant design", "chakra ui"
        )));
        
        // Backend Development
        categories.put("backend", new HashSet<>(Arrays.asList(
            "node.js", "spring boot", "spring", "django", "flask", "express.js", 
            "laravel", "ruby on rails", "asp.net", "fastapi", "graphql", "rest api",
            "microservices", "serverless", "spring mvc", "spring security"
        )));
        
        // Databases
        categories.put("databases", new HashSet<>(Arrays.asList(
            "mysql", "postgresql", "mongodb", "redis", "oracle", "sql server", 
            "cassandra", "elasticsearch", "dynamodb", "firebase", "sqlite", "mariadb",
            "cosmos db", "couchbase", "neo4j", "hbase"
        )));
        
        // Cloud Platforms
        categories.put("cloud_platforms", new HashSet<>(Arrays.asList(
            "aws", "azure", "google cloud", "docker", "kubernetes", "jenkins", 
            "terraform", "ansible", "ci/cd", "github actions", "gitlab ci",
            "aws ec2", "aws s3", "aws lambda", "azure devops", "gcp"
        )));
        
        // Mobile Development
        categories.put("mobile", new HashSet<>(Arrays.asList(
            "android", "ios", "react native", "flutter", "xamarin", "swiftui", "jetpack compose",
            "kotlin android", "java android", "objective-c", "swift"
        )));
        
        // AI/ML & Data Science
        categories.put("ai_ml", new HashSet<>(Arrays.asList(
            "machine learning", "deep learning", "tensorflow", "pytorch", "scikit-learn",
            "nlp", "computer vision", "data science", "pandas", "numpy", "opencv",
            "keras", "data analysis", "big data", "hadoop", "spark", "tableau", "power bi"
        )));
        
        // Tools & DevOps
        categories.put("tools", new HashSet<>(Arrays.asList(
            "git", "jira", "confluence", "slack", "maven", "gradle", "npm", "yarn",
            "postman", "swagger", "figma", "jenkins", "sonarqube", "prometheus", "grafana",
            "splunk", "new relic", "datadog"
        )));
        
        // Engineering Domains
        categories.put("mechanical_engineering", new HashSet<>(Arrays.asList(
            "autocad", "solidworks", "catia", "ansys", "finite element analysis", "fea",
            "computational fluid dynamics", "cfd", "matlab", "product design", "cad",
            "cam", "cnc", "gd&t", "thermodynamics", "heat transfer", "fluid mechanics"
        )));
        
        categories.put("civil_engineering", new HashSet<>(Arrays.asList(
            "autocad", "revit", "staad pro", "etabs", "primavea", "ms project",
            "structural analysis", "construction management", "project management",
            "bim", "building information modeling", "surveying"
        )));
        
        categories.put("electrical_engineering", new HashSet<>(Arrays.asList(
            "matlab", "simulink", "labview", "autocad electrical", "etap", "pscad",
            "circuit design", "power systems", "control systems", "embedded systems",
            "plc programming", "scada", "arduino", "raspberry pi", "iot", "vlsi"
        )));
        
        // Business & Management
        categories.put("business_management", new HashSet<>(Arrays.asList(
            "project management", "agile", "scrum", "kanban", "jira", "confluence",
            "strategic planning", "business development", "market research",
            "financial analysis", "budgeting", "forecasting", "stakeholder management"
        )));
        
        categories.put("sales_marketing", new HashSet<>(Arrays.asList(
            "digital marketing", "seo", "sem", "social media marketing", "content marketing",
            "email marketing", "market research", "sales strategy", "customer relationship management",
            "crm", "salesforce", "hubspot", "google analytics"
        )));
        
        return categories;
    }
    
    private Map<String, Double> initializeSkillImportanceWeights() {
        Map<String, Double> weights = new HashMap<>();
        
        // Core technical skills have higher weights
        weights.put("programming_languages", 1.3);
        weights.put("backend", 1.2);
        weights.put("frontend", 1.2);
        weights.put("databases", 1.1);
        weights.put("cloud_platforms", 1.1);
        weights.put("ai_ml", 1.4); // High demand skills
        weights.put("mobile", 1.1);
        weights.put("tools", 0.9);
        weights.put("mechanical_engineering", 1.2);
        weights.put("civil_engineering", 1.2);
        weights.put("electrical_engineering", 1.2);
        weights.put("business_management", 1.1);
        weights.put("sales_marketing", 1.0);
        
        return weights;
    }
    
    // MAIN RECOMMENDATION METHOD - ENHANCED WITH BETTER SKILL EXTRACTION
    public List<JobPosting> recommendJobsForUser(Long userId) {
        Optional<Resume> latestResume = resumeService.getLatestResumeByUserId(userId);
        if (latestResume.isEmpty()) {
            List<JobPosting> allJobs = jobPostingService.getActiveJobPostings();
            return allJobs.subList(0, Math.min(10, allJobs.size()));
        }
        
        Resume resume = latestResume.get();
        Set<String> userSkills = extractSkillsFromResume(resume);
        
        List<JobPosting> allJobs = jobPostingService.getActiveJobPostings();
        
        // Enhanced scoring with better filtering
        List<ScoredJob> scoredJobs = allJobs.stream()
            .map(job -> {
                double score = calculateEnhancedJobMatchScore(job, userSkills, resume);
                return new ScoredJob(job, score);
            })
            .filter(scoredJob -> scoredJob.score > 20.0) // Lower threshold to include more matches
            .sorted((j1, j2) -> Double.compare(j2.score, j1.score))
            .toList();
        
        return scoredJobs.stream()
            .map(scoredJob -> scoredJob.job)
            .limit(25) // Return more matches for better selection
            .toList();
    }
    
    // ENHANCED SKILL EXTRACTION USING ResumeProcessingService
    private Set<String> extractSkillsFromResume(Resume resume) {
        try {
            // First try to use the enhanced ResumeProcessingService
            Map<String, Object> analysis = resumeProcessingService.parseResumeContent(resume.getParsedText());
            @SuppressWarnings("unchecked")
            Set<String> skills = (Set<String>) analysis.get("skills");
            if (skills != null && !skills.isEmpty()) {
                logger.info("Extracted {} skills using enhanced processing", skills.size());
                return normalizeSkills(skills);
            }
        } catch (Exception e) {
            logger.info("Enhanced skill extraction failed, falling back to JSON parsing");
        }
        
        // Fallback to JSON parsing
        try {
            if (resume.getSkills() != null && !resume.getSkills().isEmpty()) {
                Set<String> skills = objectMapper.readValue(resume.getSkills(), 
                    new TypeReference<Set<String>>() {});
                logger.info("Extracted {} skills from JSON", skills.size());
                return normalizeSkills(skills);
            }
        } catch (IOException e) {
            logger.info("JSON parsing failed, falling back to text extraction");
        }
        
        // Final fallback to text extraction
        Set<String> skillsFromText = resumeService.extractSkillsFromText(resume.getParsedText());
        logger.info("Extracted {} skills from text analysis", skillsFromText.size());
        return normalizeSkills(skillsFromText);
    }
    
    private Set<String> normalizeSkills(Set<String> skills) {
        return skills.stream()
            .map(String::toLowerCase)
            .map(skill -> skill.replaceAll("[^a-zA-Z0-9#+. ]", ""))
            .map(skill -> skill.trim())
            .filter(skill -> skill.length() > 1)
            .collect(Collectors.toSet());
    }
    
    // ENHANCED: More accurate weighted percentage calculation
    private double calculateEnhancedJobMatchScore(JobPosting job, Set<String> userSkills, Resume resume) {
        double totalScore = 0.0;
        
        try {
            Set<String> requiredSkills = parseSkillsFromJson(job.getRequiredSkills());
            Set<String> preferredSkills = parseSkillsFromJson(
                job.getPreferredSkills() != null ? job.getPreferredSkills() : "[]");
            
            // IMPROVED WEIGHTS FOR BETTER ACCURACY:
            
            // 1. Required Skills Match (50% weight) - Most important
            double requiredSkillsScore = calculateEnhancedSkillsMatchScore(requiredSkills, userSkills);
            
            // 2. Preferred Skills Match (20% weight)
            double preferredSkillsScore = calculateEnhancedSkillsMatchScore(preferredSkills, userSkills);
            
            // 3. Skill Category Match (15% weight)
            double categoryScore = calculateEnhancedCategoryMatchScore(requiredSkills, userSkills);
            
            // 4. Experience Level Match (10% weight)
            double experienceScore = calculateEnhancedExperienceScore(job, resume);
            
            // 5. Education Level Match (5% weight)
            double educationScore = calculateEnhancedEducationScore(job, resume);
            
            // ENHANCED WEIGHTED FORMULA
            totalScore = (requiredSkillsScore * 0.50) + 
                        (preferredSkillsScore * 0.20) + 
                        (categoryScore * 0.15) + 
                        (experienceScore * 0.10) + 
                        (educationScore * 0.05);
            
            // Apply bonus for perfect matches in critical skills
            double bonusScore = calculateBonusScore(job, userSkills, resume);
            totalScore += bonusScore;
            
            // Ensure score doesn't exceed 1.0 (100%)
            totalScore = Math.min(totalScore, 1.0);
            
        } catch (Exception e) {
            logger.error("Error calculating match score for job {}: {}", job.getId(), e.getMessage());
            totalScore = calculateEnhancedFallbackScore(job, userSkills);
            totalScore = Math.min(totalScore, 1.0);
        }
        
        return totalScore * 100;
    }
    
    // ENHANCED: Better skill matching with partial matches and importance weighting
    private double calculateEnhancedSkillsMatchScore(Set<String> jobSkills, Set<String> userSkills) {
        if (jobSkills.isEmpty()) return 0.5; // Neutral score for jobs with no specific skills
        
        double totalMatchScore = 0.0;
        int matchedCount = 0;
        
        for (String jobSkill : jobSkills) {
            Optional<String> bestMatch = findBestSkillMatch(jobSkill, userSkills);
            
            if (bestMatch.isPresent()) {
                double similarity = calculateSkillSimilarity(jobSkill, bestMatch.get());
                double importance = calculateSkillImportance(jobSkill);
                totalMatchScore += similarity * importance;
                matchedCount++;
            }
        }
        
        if (matchedCount == 0) return 0.0;
        
        return totalMatchScore / jobSkills.size();
    }
    
    // NEW: Find best matching skill from user skills
    private Optional<String> findBestSkillMatch(String jobSkill, Set<String> userSkills) {
        return userSkills.stream()
            .filter(userSkill -> calculateSkillSimilarity(jobSkill, userSkill) > 0.6) // Lower threshold for better matching
            .max((s1, s2) -> Double.compare(
                calculateSkillSimilarity(jobSkill, s1),
                calculateSkillSimilarity(jobSkill, s2)
            ));
    }
    
    // ENHANCED: Better category matching with importance weights
    private double calculateEnhancedCategoryMatchScore(Set<String> requiredSkills, Set<String> userSkills) {
        if (requiredSkills.isEmpty()) return 0.5;
        
        double totalCategoryScore = 0.0;
        int relevantCategories = 0;
        
        for (Map.Entry<String, Set<String>> category : skillCategories.entrySet()) {
            boolean jobUsesCategory = requiredSkills.stream()
                .anyMatch(skill -> category.getValue().contains(skill.toLowerCase()));
            
            if (jobUsesCategory) {
                boolean userHasCategory = userSkills.stream()
                    .anyMatch(skill -> category.getValue().contains(skill.toLowerCase()));
                
                double categoryWeight = skillImportanceWeights.getOrDefault(category.getKey(), 1.0);
                if (userHasCategory) {
                    totalCategoryScore += 1.0 * categoryWeight;
                } else {
                    totalCategoryScore += 0.2 * categoryWeight; // Small score for relevant category even without exact match
                }
                relevantCategories++;
            }
        }
        
        return relevantCategories > 0 ? Math.min(totalCategoryScore / relevantCategories, 1.0) : 0.5;
    }
    
    // ENHANCED: More granular experience scoring
    private double calculateEnhancedExperienceScore(JobPosting job, Resume resume) {
        if (job.getExperienceLevel() == null || resume == null) return 0.5;
        
        String jobLevel = job.getExperienceLevel().toLowerCase();
        Integer userExperience = resume.getExperienceYears();
        
        if (userExperience == null) return 0.3;
        
        Map<String, ExperienceRange> experienceRanges = new HashMap<>();
        experienceRanges.put("intern", new ExperienceRange(0, 1, 0.4));
        experienceRanges.put("entry", new ExperienceRange(0, 2, 0.6));
        experienceRanges.put("junior", new ExperienceRange(1, 3, 0.7));
        experienceRanges.put("mid", new ExperienceRange(2, 5, 0.8));
        experienceRanges.put("mid-level", new ExperienceRange(2, 5, 0.8));
        experienceRanges.put("senior", new ExperienceRange(4, 8, 0.9));
        experienceRanges.put("lead", new ExperienceRange(6, 12, 1.0));
        experienceRanges.put("principal", new ExperienceRange(8, 20, 1.0));
        experienceRanges.put("architect", new ExperienceRange(10, 25, 1.0));
        
        for (Map.Entry<String, ExperienceRange> entry : experienceRanges.entrySet()) {
            if (jobLevel.contains(entry.getKey())) {
                ExperienceRange range = entry.getValue();
                if (userExperience >= range.min && userExperience <= range.max) {
                    return range.idealScore;
                } else if (userExperience > range.max) {
                    // Overqualified - smaller penalty
                    double overqualification = (double) (userExperience - range.max) / range.max;
                    return Math.max(0.6, 1.0 - (overqualification * 0.2));
                } else {
                    // Underqualified - gradual penalty
                    double underqualification = (double) (range.min - userExperience) / range.min;
                    return Math.max(0.4, range.idealScore - (underqualification * 0.3));
                }
            }
        }
        
        return 0.5;
    }
    
    // ENHANCED: More comprehensive education scoring
    private double calculateEnhancedEducationScore(JobPosting job, Resume resume) {
        if (resume == null || resume.getEducationLevel() == null) return 0.5;
        
        String userEducation = resume.getEducationLevel().toLowerCase();
        String jobText = (job.getDescription() + " " + job.getTitle() + " " + 
                         job.getRequiredSkills()).toLowerCase();
        
        Map<String, EducationLevel> educationLevels = new HashMap<>();
        educationLevels.put("phd", new EducationLevel(5, 1.0));
        educationLevels.put("doctorate", new EducationLevel(5, 1.0));
        educationLevels.put("master", new EducationLevel(4, 0.8));
        educationLevels.put("m.tech", new EducationLevel(4, 0.8));
        educationLevels.put("m.sc", new EducationLevel(4, 0.8));
        educationLevels.put("mba", new EducationLevel(4, 0.8));
        educationLevels.put("bachelor", new EducationLevel(3, 0.7));
        educationLevels.put("b.tech", new EducationLevel(3, 0.7));
        educationLevels.put("b.sc", new EducationLevel(3, 0.7));
        educationLevels.put("bca", new EducationLevel(3, 0.7));
        educationLevels.put("associate", new EducationLevel(2, 0.5));
        educationLevels.put("diploma", new EducationLevel(2, 0.5));
        educationLevels.put("high school", new EducationLevel(1, 0.3));
        
        EducationLevel userLevel = educationLevels.entrySet().stream()
            .filter(entry -> userEducation.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(new EducationLevel(0, 0.4));
        
        EducationLevel requiredLevel = educationLevels.entrySet().stream()
            .filter(entry -> jobText.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(new EducationLevel(0, 0.5));
        
        if (requiredLevel.level == 0) return 0.5;
        
        if (userLevel.level >= requiredLevel.level) {
            return userLevel.score;
        } else {
            // Smaller penalty for being underqualified
            double levelDifference = requiredLevel.level - userLevel.level;
            double penalty = levelDifference * 0.15;
            return Math.max(0.3, userLevel.score - penalty);
        }
    }
    
    // NEW: Calculate bonus score for exceptional matches
    private double calculateBonusScore(JobPosting job, Set<String> userSkills, Resume resume) {
        double bonus = 0.0;
        
        Set<String> requiredSkills = parseSkillsFromJson(job.getRequiredSkills());
        
        // Bonus for matching all required skills
        long exactMatches = requiredSkills.stream()
            .filter(reqSkill -> userSkills.stream()
                .anyMatch(userSkill -> calculateSkillSimilarity(reqSkill, userSkill) > 0.8))
            .count();
        
        if (exactMatches == requiredSkills.size() && !requiredSkills.isEmpty()) {
            bonus += 0.15; // 15% bonus for perfect skill match
        }
        
        // Bonus for high-demand skills match
        long highDemandMatches = userSkills.stream()
            .filter(this::isHighDemandSkill)
            .filter(userSkill -> requiredSkills.stream()
                .anyMatch(reqSkill -> calculateSkillSimilarity(reqSkill, userSkill) > 0.7))
            .count();
        
        bonus += highDemandMatches * 0.05; // 5% per high-demand skill
        
        return Math.min(bonus, 0.25); // Cap bonus at 25%
    }
    
    // ENHANCED: Better skill similarity calculation
    private double calculateSkillSimilarity(String skill1, String skill2) {
        String s1 = skill1.toLowerCase().trim();
        String s2 = skill2.toLowerCase().trim();
        
        // Exact match
        if (s1.equals(s2)) return 1.0;
        
        // Contains match
        if (s1.contains(s2) || s2.contains(s1)) return 0.9;
        
        // Common variations and abbreviations
        Map<String, String> commonVariations = new HashMap<>();
        commonVariations.put("js", "javascript");
        commonVariations.put("reactjs", "react");
        commonVariations.put("angularjs", "angular");
        commonVariations.put("vuejs", "vue");
        commonVariations.put("nodejs", "node.js");
        commonVariations.put("springboot", "spring boot");
        commonVariations.put("ml", "machine learning");
        commonVariations.put("ai", "artificial intelligence");
        commonVariations.put("db", "database");
        
        for (Map.Entry<String, String> variation : commonVariations.entrySet()) {
            if ((s1.equals(variation.getKey()) && s2.equals(variation.getValue())) ||
                (s1.equals(variation.getValue()) && s2.equals(variation.getKey()))) {
                return 0.95;
            }
        }
        
        // Word-based similarity
        String[] words1 = s1.split("[^a-zA-Z0-9]");
        String[] words2 = s2.split("[^a-zA-Z0-9]");
        
        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.length() > 2 && word2.length() > 2 && 
                    (word1.equals(word2) || word1.contains(word2) || word2.contains(word1))) {
                    commonWords++;
                }
            }
        }
        
        if (commonWords > 0) {
            double wordSimilarity = (double) commonWords / Math.max(words1.length, words2.length);
            return Math.min(wordSimilarity * 1.2, 0.8); // Boost word similarity
        }
        
        return 0.0;
    }
    
    // ENHANCED: Calculate skill importance based on category and market demand
    private double calculateSkillImportance(String skill) {
        String lowerSkill = skill.toLowerCase();
        
        // Check which category this skill belongs to and apply weight
        for (Map.Entry<String, Set<String>> category : skillCategories.entrySet()) {
            if (category.getValue().contains(lowerSkill)) {
                return skillImportanceWeights.getOrDefault(category.getKey(), 1.0);
            }
        }
        
        // Check for partial matches in categories
        for (Map.Entry<String, Set<String>> category : skillCategories.entrySet()) {
            boolean partialMatch = category.getValue().stream()
                .anyMatch(catSkill -> lowerSkill.contains(catSkill) || catSkill.contains(lowerSkill));
            if (partialMatch) {
                return skillImportanceWeights.getOrDefault(category.getKey(), 1.0) * 0.8;
            }
        }
        
        return 1.0; // Default weight
    }
    
    // ENHANCED: Check if a skill is high demand
    private boolean isHighDemandSkill(String skill) {
        Set<String> highDemandSkills = new HashSet<>(Arrays.asList(
            "python", "java", "javascript", "aws", "docker", "kubernetes", 
            "react", "node.js", "machine learning", "data science", "spring boot",
            "tensorflow", "pytorch", "artificial intelligence", "cloud computing",
            "devops", "microservices", "sql", "nosql", "ci/cd"
        ));
        
        String lowerSkill = skill.toLowerCase();
        return highDemandSkills.contains(lowerSkill) ||
               highDemandSkills.stream().anyMatch(highDemand -> 
                   lowerSkill.contains(highDemand) || highDemand.contains(lowerSkill));
    }
    
    private double calculateEnhancedFallbackScore(JobPosting job, Set<String> userSkills) {
        String jobText = (job.getTitle() + " " + job.getDescription() + " " + 
                         job.getRequiredSkills() + " " + job.getPreferredSkills()).toLowerCase();
        
        long matchedSkills = userSkills.stream()
            .filter(skill -> jobText.contains(skill.toLowerCase()))
            .count();
        
        double baseScore = userSkills.isEmpty() ? 0.2 : (double) matchedSkills / Math.max(userSkills.size(), 1);
        
        // Apply importance weighting even in fallback
        double weightedScore = baseScore;
        for (String skill : userSkills) {
            if (jobText.contains(skill.toLowerCase())) {
                weightedScore += (calculateSkillImportance(skill) - 1.0) * 0.05;
            }
        }
        
        return Math.min(weightedScore, 1.0);
    }
    
    private Set<String> parseSkillsFromJson(String skillsJson) {
        try {
            if (skillsJson != null && !skillsJson.isEmpty()) {
                return objectMapper.readValue(skillsJson, new TypeReference<Set<String>>() {});
            }
        } catch (IOException e) {
            logger.warn("Failed to parse skills JSON, returning empty set: {}", skillsJson);
        }
        return new HashSet<>();
    }
    
    // ENHANCED: Get detailed recommendation analysis
    public Map<String, Object> getDetailedRecommendationAnalysis(Long userId) {
        Optional<Resume> latestResume = resumeService.getLatestResumeByUserId(userId);
        Map<String, Object> analysis = new HashMap<>();
        
        if (latestResume.isEmpty()) {
            analysis.put("message", "No resume found for user");
            List<JobPosting> allJobs = jobPostingService.getActiveJobPostings();
            List<Map<String, Object>> jobDetails = allJobs.stream()
                .limit(5)
                .map(this::createCompleteJobAnalysis)
                .toList();
            analysis.put("detailedRecommendations", jobDetails);
            return analysis;
        }
        
        Resume resume = latestResume.get();
        Set<String> userSkills = extractSkillsFromResume(resume);
        List<JobPosting> recommendations = recommendJobsForUser(userId);
        
        Integer userExperience = resume.getExperienceYears();
        if (userExperience != null && (userExperience < 0 || userExperience > 50)) {
            userExperience = 0;
        }
        
        analysis.put("userSkills", new ArrayList<>(userSkills));
        analysis.put("userExperience", userExperience);
        analysis.put("userEducation", resume.getEducationLevel());
        analysis.put("totalJobsAnalyzed", jobPostingService.getActiveJobPostings().size());
        analysis.put("recommendedJobsCount", recommendations.size());
        
        List<Map<String, Object>> detailedRecommendations = recommendations.stream()
            .map(job -> createCompleteJobAnalysis(job, userSkills, resume))
            .toList();
        
        analysis.put("detailedRecommendations", detailedRecommendations);
        
        return analysis;
    }

    private Map<String, Object> createCompleteJobAnalysis(JobPosting job) {
        Map<String, Object> jobAnalysis = new HashMap<>();
        jobAnalysis.put("jobId", job.getId());
        jobAnalysis.put("title", job.getTitle());
        jobAnalysis.put("company", job.getCompany());
        jobAnalysis.put("description", job.getDescription());
        jobAnalysis.put("location", job.getLocation());
        jobAnalysis.put("salaryRange", job.getSalaryRange());
        jobAnalysis.put("jobType", job.getJobType());
        jobAnalysis.put("experienceLevel", job.getExperienceLevel());
        jobAnalysis.put("requiredSkills", job.getRequiredSkills());
        jobAnalysis.put("preferredSkills", job.getPreferredSkills());
        jobAnalysis.put("postedDate", job.getPostedDate());
        jobAnalysis.put("averageRating", job.getAverageRating());
        jobAnalysis.put("totalRatings", job.getTotalRatings());
        jobAnalysis.put("matchScore", 0);
        jobAnalysis.put("matchedSkills", new HashSet<>());
        jobAnalysis.put("missingSkills", parseSkillsFromJson(job.getRequiredSkills()));
        
        return jobAnalysis;
    }

    private Map<String, Object> createCompleteJobAnalysis(JobPosting job, Set<String> userSkills, Resume resume) {
        Map<String, Object> jobAnalysis = new HashMap<>();
        
        double matchScore = calculateEnhancedJobMatchScore(job, userSkills, resume);
        
        jobAnalysis.put("jobId", job.getId());
        jobAnalysis.put("title", job.getTitle());
        jobAnalysis.put("company", job.getCompany());
        jobAnalysis.put("description", job.getDescription());
        jobAnalysis.put("location", job.getLocation());
        jobAnalysis.put("salaryRange", job.getSalaryRange());
        jobAnalysis.put("jobType", job.getJobType());
        jobAnalysis.put("experienceLevel", job.getExperienceLevel());
        jobAnalysis.put("requiredSkills", job.getRequiredSkills());
        jobAnalysis.put("preferredSkills", job.getPreferredSkills());
        jobAnalysis.put("postedDate", job.getPostedDate());
        jobAnalysis.put("averageRating", job.getAverageRating());
        jobAnalysis.put("totalRatings", job.getTotalRatings());
        jobAnalysis.put("matchScore", Math.round(matchScore));
        jobAnalysis.put("matchedSkills", findMatchedSkills(job, userSkills));
        jobAnalysis.put("missingSkills", findMissingSkills(job, userSkills));
        
        return jobAnalysis;
    }
    
    private Set<String> findMatchedSkills(JobPosting job, Set<String> userSkills) {
        Set<String> requiredSkills = parseSkillsFromJson(job.getRequiredSkills());
        Set<String> preferredSkills = parseSkillsFromJson(
            job.getPreferredSkills() != null ? job.getPreferredSkills() : "[]");
        
        Set<String> allJobSkills = new HashSet<>();
        allJobSkills.addAll(requiredSkills);
        allJobSkills.addAll(preferredSkills);
        
        return userSkills.stream()
            .filter(userSkill -> allJobSkills.stream()
                .anyMatch(jobSkill -> calculateSkillSimilarity(jobSkill, userSkill) > 0.6))
            .collect(Collectors.toSet());
    }
    
    private Set<String> findMissingSkills(JobPosting job, Set<String> userSkills) {
        Set<String> requiredSkills = parseSkillsFromJson(job.getRequiredSkills());
        Set<String> matchedSkills = findMatchedSkills(job, userSkills);
        
        return requiredSkills.stream()
            .filter(jobSkill -> matchedSkills.stream()
                .noneMatch(matchedSkill -> calculateSkillSimilarity(jobSkill, matchedSkill) > 0.6))
            .collect(Collectors.toSet());
    }
    
    public List<JobPosting> recommendJobsBySkills(Set<String> skills) {
        List<JobPosting> allJobs = jobPostingService.getActiveJobPostings();
        
        List<ScoredJob> scoredJobs = allJobs.stream()
            .map(job -> {
                double score = calculateEnhancedSkillMatchScore(job, skills);
                return new ScoredJob(job, score);
            })
            .sorted((j1, j2) -> Double.compare(j2.score, j1.score))
            .toList();
        
        return scoredJobs.stream()
            .map(scoredJob -> scoredJob.job)
            .limit(20)
            .toList();
    }
    
    private double calculateEnhancedSkillMatchScore(JobPosting job, Set<String> skills) {
        try {
            Set<String> requiredSkills = parseSkillsFromJson(job.getRequiredSkills());
            Set<String> preferredSkills = parseSkillsFromJson(
                job.getPreferredSkills() != null ? job.getPreferredSkills() : "[]");
            
            double requiredScore = calculateEnhancedSkillsMatchScore(requiredSkills, skills);
            double preferredScore = calculateEnhancedSkillsMatchScore(preferredSkills, skills);
            
            double finalScore = (requiredScore * 0.7 + preferredScore * 0.3);
            
            return Math.min(finalScore, 1.0) * 100;
            
        } catch (Exception e) {
            double fallbackScore = calculateEnhancedFallbackScore(job, skills);
            return Math.min(fallbackScore, 1.0) * 100;
        }
    }
    
    // Enhanced helper classes
    private static class ScoredJob {
        JobPosting job;
        double score;
        
        ScoredJob(JobPosting job, double score) {
            this.job = job;
            this.score = score;
        }
    }
    
    private static class ExperienceRange {
        int min;
        int max;
        double idealScore;
        
        ExperienceRange(int min, int max, double idealScore) {
            this.min = min;
            this.max = max;
            this.idealScore = idealScore;
        }
    }
    
    private static class EducationLevel {
        int level;
        double score;
        
        EducationLevel(int level, double score) {
            this.level = level;
            this.score = score;
        }
    }
}