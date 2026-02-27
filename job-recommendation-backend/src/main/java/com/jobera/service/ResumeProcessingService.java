package com.jobera.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ResumeProcessingService {
    
    // COMPREHENSIVE MULTI-DOMAIN SKILLS DATABASE - FULLY RESTORED
    private static final Map<String, Set<String>> TECHNICAL_SKILLS_BY_CATEGORY = new HashMap<>();
    
    static {
        // IT & Software Development - FULLY RESTORED
        TECHNICAL_SKILLS_BY_CATEGORY.put("programming_languages", new HashSet<>(Arrays.asList(
            "java", "python", "javascript", "typescript", "c++", "c#", "ruby", "php",
            "swift", "kotlin", "go", "rust", "scala", "r", "matlab", "perl", "dart"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("frontend_development", new HashSet<>(Arrays.asList(
            "react", "angular", "vue", "html", "css", "sass", "less", "bootstrap", 
            "tailwind", "webpack", "vite", "next.js", "nuxt.js", "jquery", "redux"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("backend_development", new HashSet<>(Arrays.asList(
            "node.js", "spring boot", "spring", "django", "flask", "express.js", 
            "laravel", "ruby on rails", "asp.net", "fastapi", "graphql", "rest api"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("databases", new HashSet<>(Arrays.asList(
            "mysql", "postgresql", "mongodb", "redis", "oracle", "sql server", 
            "cassandra", "elasticsearch", "dynamodb", "firebase", "sqlite", "mariadb"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("cloud_platforms", new HashSet<>(Arrays.asList(
            "aws", "azure", "google cloud", "docker", "kubernetes", "jenkins", 
            "terraform", "ansible", "ci/cd", "github actions", "gitlab ci"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("mobile_development", new HashSet<>(Arrays.asList(
            "android", "ios", "react native", "flutter", "xamarin", "swiftui", "jetpack compose"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("ai_ml_data_science", new HashSet<>(Arrays.asList(
            "machine learning", "deep learning", "tensorflow", "pytorch", "scikit-learn",
            "nlp", "computer vision", "data science", "pandas", "numpy", "opencv",
            "keras", "data analysis", "big data", "hadoop", "spark"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("devops_tools", new HashSet<>(Arrays.asList(
            "git", "jira", "confluence", "slack", "maven", "gradle", "npm", "yarn",
            "postman", "swagger", "figma", "jenkins", "sonarqube", "prometheus", "grafana"
        )));
        
        // Engineering Domains - FULLY RESTORED
        TECHNICAL_SKILLS_BY_CATEGORY.put("mechanical_engineering", new HashSet<>(Arrays.asList(
            "autocad", "solidworks", "catia", "ansys", "finite element analysis", "fea",
            "computational fluid dynamics", "cfd", "matlab", "product design", "cad",
            "cam", "cnc", "gd&t", "thermodynamics", "heat transfer", "fluid mechanics",
            "machine design", "manufacturing", "3d printing", "additive manufacturing"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("civil_engineering", new HashSet<>(Arrays.asList(
            "autocad", "revit", "staad pro", "etabs", "primavea", "ms project",
            "structural analysis", "construction management", "project management",
            "bim", "building information modeling", "surveying", "geotechnical engineering",
            "transportation engineering", "environmental engineering", "water resources"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("electrical_engineering", new HashSet<>(Arrays.asList(
            "matlab", "simulink", "labview", "autocad electrical", "etap", "pscad",
            "circuit design", "power systems", "control systems", "embedded systems",
            "plc programming", "scada", "arduino", "raspberry pi", "iot", "vlsi",
            "digital signal processing", "dsp", "renewable energy"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("electronics_engineering", new HashSet<>(Arrays.asList(
            "vlsi", "embedded systems", "arduino", "raspberry pi", "pcb design",
            "altium", "orcad", "spice", "microcontrollers", "fpga", "verilog", "vhdl",
            "digital design", "analog design", "signal processing", "iot", "wireless"
        )));
        
        // Business & Management - FULLY RESTORED
        TECHNICAL_SKILLS_BY_CATEGORY.put("business_management", new HashSet<>(Arrays.asList(
            "project management", "agile", "scrum", "kanban", "jira", "confluence",
            "strategic planning", "business development", "market research",
            "financial analysis", "budgeting", "forecasting", "stakeholder management",
            "risk management", "change management", "process improvement", "six sigma",
            "lean manufacturing", "supply chain management", "logistics"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("human_resources", new HashSet<>(Arrays.asList(
            "recruitment", "talent acquisition", "employee relations", "performance management",
            "training and development", "compensation and benefits", "hr policies",
            "labor laws", "onboarding", "succession planning", "hr analytics"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("sales_marketing", new HashSet<>(Arrays.asList(
            "digital marketing", "seo", "sem", "social media marketing", "content marketing",
            "email marketing", "market research", "sales strategy", "customer relationship management",
            "crm", "salesforce", "hubspot", "google analytics", "brand management",
            "public relations", "advertising", "market analysis"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("finance_accounting", new HashSet<>(Arrays.asList(
            "financial analysis", "financial modeling", "accounting", "bookkeeping",
            "taxation", "auditing", "budgeting", "forecasting", "investment analysis",
            "risk management", "quickbooks", "sap fico", "ifrs", "gaap", "cfa", "cpa"
        )));
        
        // Healthcare & Medical - FULLY RESTORED
        TECHNICAL_SKILLS_BY_CATEGORY.put("healthcare_medical", new HashSet<>(Arrays.asList(
            "patient care", "medical terminology", "electronic health records", "ehr",
            "healthcare management", "clinical research", "pharmacy", "nursing",
            "medical coding", "icd-10", "cpr", "first aid", "health informatics",
            "medical devices", "telemedicine", "healthcare compliance"
        )));
        
        // Education & Teaching - FULLY RESTORED
        TECHNICAL_SKILLS_BY_CATEGORY.put("education_teaching", new HashSet<>(Arrays.asList(
            "curriculum development", "lesson planning", "classroom management",
            "student assessment", "educational technology", "e-learning",
            "special education", "teacher training", "academic counseling",
            "instructional design", "pedagogy", "andragogy"
        )));
        
        // Design & Creative - FULLY RESTORED
        TECHNICAL_SKILLS_BY_CATEGORY.put("design_creative", new HashSet<>(Arrays.asList(
            "ui design", "ux design", "user research", "wireframing", "prototyping",
            "adobe photoshop", "adobe illustrator", "adobe indesign", "figma",
            "sketch", "adobe xd", "graphic design", "web design", "motion graphics",
            "video editing", "adobe premiere", "after effects", "3d modeling", "blender"
        )));
        
        // Legal & Hospitality - FULLY RESTORED
        TECHNICAL_SKILLS_BY_CATEGORY.put("legal", new HashSet<>(Arrays.asList(
            "legal research", "contract law", "corporate law", "litigation",
            "legal writing", "compliance", "intellectual property", "paralegal"
        )));
        
        TECHNICAL_SKILLS_BY_CATEGORY.put("hospitality", new HashSet<>(Arrays.asList(
            "hotel management", "restaurant management", "customer service",
            "event planning", "tourism", "hospitality management"
        )));
        
        // Universal Soft Skills - FULLY RESTORED
        TECHNICAL_SKILLS_BY_CATEGORY.put("soft_skills", new HashSet<>(Arrays.asList(
            "communication", "leadership", "teamwork", "problem solving",
            "critical thinking", "time management", "adaptability", "creativity",
            "emotional intelligence", "negotiation", "presentation", "public speaking",
            "conflict resolution", "decision making", "analytical thinking"
        )));
    }
    
    // Domain mapping for categorization - FULLY RESTORED
    private static final Map<String, String> DOMAIN_MAPPING = new HashMap<>();
    
    static {
        // IT & Software
        DOMAIN_MAPPING.put("programming_languages", "IT & Software");
        DOMAIN_MAPPING.put("frontend_development", "IT & Software");
        DOMAIN_MAPPING.put("backend_development", "IT & Software");
        DOMAIN_MAPPING.put("databases", "IT & Software");
        DOMAIN_MAPPING.put("cloud_platforms", "IT & Software");
        DOMAIN_MAPPING.put("mobile_development", "IT & Software");
        DOMAIN_MAPPING.put("ai_ml_data_science", "IT & Software");
        DOMAIN_MAPPING.put("devops_tools", "IT & Software");
        
        // Engineering
        DOMAIN_MAPPING.put("mechanical_engineering", "Engineering");
        DOMAIN_MAPPING.put("civil_engineering", "Engineering");
        DOMAIN_MAPPING.put("electrical_engineering", "Engineering");
        DOMAIN_MAPPING.put("electronics_engineering", "Engineering");
        
        // Business & Management
        DOMAIN_MAPPING.put("business_management", "Business & Management");
        DOMAIN_MAPPING.put("human_resources", "Business & Management");
        DOMAIN_MAPPING.put("sales_marketing", "Business & Management");
        DOMAIN_MAPPING.put("finance_accounting", "Business & Management");
        
        // Other domains
        DOMAIN_MAPPING.put("healthcare_medical", "Healthcare & Medical");
        DOMAIN_MAPPING.put("education_teaching", "Education & Teaching");
        DOMAIN_MAPPING.put("design_creative", "Design & Creative");
        DOMAIN_MAPPING.put("legal", "Legal");
        DOMAIN_MAPPING.put("hospitality", "Hospitality");
        DOMAIN_MAPPING.put("soft_skills", "Soft Skills");
    }
    
    // Flat set of all skills for backward compatibility
    private static final Set<String> ALL_TECHNICAL_SKILLS = new HashSet<>();
    
    static {
        for (Set<String> skills : TECHNICAL_SKILLS_BY_CATEGORY.values()) {
            ALL_TECHNICAL_SKILLS.addAll(skills);
        }
    }
    
    private final ObjectMapper objectMapper;
    
    public ResumeProcessingService() {
        this.objectMapper = new ObjectMapper();
    }
    
    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    public String extractTextFromTextFile(MultipartFile file) throws IOException {
        return new String(file.getBytes());
    }
    
    // MAIN METHOD - WITH FIXED EDUCATION EXTRACTION AND FULL SKILLS
    public Map<String, Object> parseResumeContent(String content) {
        Map<String, Object> result = new HashMap<>();
        
        // Extract skills with enhanced validation
        Set<String> rawSkills = extractSkillsWithPatternMatching(content);
        Set<String> validatedSkills = validateSkillsAgainstResume(rawSkills, content);
        
        // Debug output
        debugSkillExtraction(content, rawSkills, validatedSkills);
        
        // Categorize skills for better job matching
        Map<String, Set<String>> categorizedSkills = categorizeSkills(validatedSkills);
        String primaryDomain = identifyPrimaryDomain(categorizedSkills);
        Map<String, Integer> domainBreakdown = getDomainBreakdown(categorizedSkills);
        
        result.put("skills", validatedSkills);
        result.put("categorized_skills", categorizedSkills);
        result.put("primary_domain", primaryDomain);
        result.put("domain_breakdown", domainBreakdown);
        
        // Extract experience (existing functionality)
        int experienceYears = extractExperienceYears(content);
        result.put("experienceYears", experienceYears);
        
        // EXTRACT EDUCATION - FINAL FIXED VERSION
        String educationLevel = extractEducationLevelFinal(content);
        result.put("educationLevel", educationLevel);
        
        // Enhanced job recommendations based on skills
        List<Map<String, Object>> jobRecommendations = generateJobRecommendations(validatedSkills, categorizedSkills, primaryDomain, experienceYears);
        result.put("job_recommendations", jobRecommendations);
        
        // Debug output
        System.out.println("=== RESUME PARSING DEBUG ===");
        System.out.println("Extracted experience: " + experienceYears + " years");
        System.out.println("Extracted education: " + educationLevel);
        System.out.println("Raw skills count: " + rawSkills.size());
        System.out.println("Validated skills count: " + validatedSkills.size());
        System.out.println("Primary domain: " + primaryDomain);
        System.out.println("Domain breakdown: " + domainBreakdown);
        System.out.println("Skill categories found: " + categorizedSkills.keySet());
        System.out.println("Job recommendations generated: " + jobRecommendations.size());
        
        return result;
    }
    
    // FINAL FIXED EDUCATION EXTRACTION METHOD
    private String extractEducationLevelFinal(String content) {
        System.out.println("=== FINAL EDUCATION EXTRACTION DEBUG ===");
        String lowerContent = content.toLowerCase();
        
        // Create education level scores
        Map<String, Integer> educationScores = new HashMap<>();
        educationScores.put("PhD", 0);
        educationScores.put("Master's", 0);
        educationScores.put("Bachelor's", 0);
        educationScores.put("Associate/Diploma", 0);
        educationScores.put("High School", 0);
        
        // PhD Patterns
        String[] phdPatterns = {
            "phd", "ph\\.d", "doctorate", "doctor of", "d\\.phil", "ph d", "ph\\. d"
        };
        
        // Master's Patterns  
        String[] mastersPatterns = {
            "master", "m\\.tech", "m\\.sc", "m\\.e", "m\\.a", "ms", "masters", "post graduate",
            "m\\. sc", "m\\. tech", "m\\. e", "m\\. a", "mba", "m\\.b\\.a", "m\\.s", "m\\.s\\.",
            "master of", "m\\.eng", "m\\.ed"
        };
        
        // Bachelor's Patterns
        String[] bachelorsPatterns = {
            "bachelor", "b\\.tech", "b\\.sc", "b\\.e", "b\\.a", "bs", "be", "bachelors", 
            "b\\. e", "b\\. tech", "b\\. sc", "b\\. a", "bca", "b\\.com", "b\\. com", 
            "graduat", "undergraduate", "baccalaureate", "b\\.s", "b\\.s\\.", "b\\.a\\.",
            "b\\.e\\.", "b\\.tech\\.", "bachelor of", "b\\.arch", "b\\.des"
        };
        
        // Associate/Diploma Patterns
        String[] associatePatterns = {
            "associate", "diploma", "associate degree", "a\\.a", "a\\.s", "a\\.a\\.s",
            "associate of", "diploma in", "certificate", "certification"
        };
        
        // High School Patterns
        String[] highSchoolPatterns = {
            "high school", "secondary", "higher secondary", "ssc", "hsc", "intermediate",
            "higher school", "senior secondary", "12th", "twelfth", "10th", "tenth",
            "highschool", "senior high", "secondary school"
        };
        
        // Check for each education level
        checkEducationPatterns(lowerContent, educationScores, "PhD", phdPatterns);
        checkEducationPatterns(lowerContent, educationScores, "Master's", mastersPatterns);
        checkEducationPatterns(lowerContent, educationScores, "Bachelor's", bachelorsPatterns);
        checkEducationPatterns(lowerContent, educationScores, "Associate/Diploma", associatePatterns);
        checkEducationPatterns(lowerContent, educationScores, "High School", highSchoolPatterns);
        
        // Additional context checks
        checkEducationContext(lowerContent, educationScores);
        
        // Find the highest education level with the highest score
        String highestEducation = findHighestEducation(educationScores);
        
        System.out.println("Education Scores: " + educationScores);
        System.out.println("Final Education Level: " + highestEducation);
        
        return highestEducation;
    }
    
    private void checkEducationPatterns(String content, Map<String, Integer> scores, String level, String[] patterns) {
        for (String pattern : patterns) {
            // Use word boundaries for exact matching
            String regex = "\\b" + pattern + "\\b";
            Pattern compiledPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = compiledPattern.matcher(content);
            
            int count = 0;
            while (matcher.find()) {
                count++;
                System.out.println("✓ Found '" + pattern + "' for " + level + " at position " + matcher.start());
            }
            
            if (count > 0) {
                scores.put(level, scores.get(level) + count);
            }
        }
    }
    
    private void checkEducationContext(String content, Map<String, Integer> scores) {
        // Check for university/college context
        Pattern institutionPattern = Pattern.compile(
            "(university|college|institute|institution|school of|faculty of|academy)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher institutionMatcher = institutionPattern.matcher(content);
        if (institutionMatcher.find()) {
            System.out.println("✓ Found educational institution");
            // If we found an institution but no specific degree, assume at least Bachelor's
            if (scores.get("Bachelor's") == 0 && scores.get("Master's") == 0 && scores.get("PhD") == 0) {
                scores.put("Bachelor's", 1);
            }
        }
        
        // Check for graduation years
        Pattern graduationPattern = Pattern.compile(
            "(graduat|completed|degree|completed).*?(19|20)\\d{2}",
            Pattern.CASE_INSENSITIVE
        );
        Matcher graduationMatcher = graduationPattern.matcher(content);
        if (graduationMatcher.find()) {
            System.out.println("✓ Found graduation pattern with year");
            if (scores.get("Bachelor's") == 0 && scores.get("Master's") == 0 && scores.get("PhD") == 0) {
                scores.put("Bachelor's", 1);
            }
        }
        
        // Check for degree fields (engineering, science, arts, etc.)
        Pattern degreeFieldPattern = Pattern.compile(
            "(engineering|technology|science|arts|commerce|business|computer|information)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher fieldMatcher = degreeFieldPattern.matcher(content);
        if (fieldMatcher.find()) {
            System.out.println("✓ Found degree field: " + fieldMatcher.group(1));
            if (scores.get("Bachelor's") == 0 && scores.get("Master's") == 0 && scores.get("PhD") == 0) {
                scores.put("Bachelor's", 1);
            }
        }
    }
    
    private String findHighestEducation(Map<String, Integer> scores) {
        // Define education hierarchy
        String[] hierarchy = {"PhD", "Master's", "Bachelor's", "Associate/Diploma", "High School"};
        
        // Find the highest level with a score > 0
        for (String level : hierarchy) {
            if (scores.get(level) > 0) {
                return level;
            }
        }
        
        return "Not Specified";
    }
    
    // ENHANCED SKILL EXTRACTION METHOD
    private Set<String> extractSkillsWithPatternMatching(String content) {
        Set<String> foundSkills = new HashSet<>();
        String lowerContent = content.toLowerCase();
        
        // Clean the content - remove special characters that might interfere
        String cleanContent = lowerContent.replaceAll("[^a-zA-Z0-9\\s\\+\\#\\.]", " ");
        
        // Check all skills across all categories with strict word boundaries
        for (String skill : ALL_TECHNICAL_SKILLS) {
            // Use strict word boundaries and handle multi-word skills
            String regexPattern;
            if (skill.contains(" ")) {
                // For multi-word skills like "spring boot"
                regexPattern = "\\b" + Pattern.quote(skill) + "\\b";
            } else {
                // For single-word skills with strict boundaries
                regexPattern = "(?<![a-zA-Z0-9])" + Pattern.quote(skill) + "(?![a-zA-Z0-9])";
            }
            
            Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(cleanContent);
            if (matcher.find()) {
                foundSkills.add(skill);
            }
        }
        
        return foundSkills;
    }
    
    // VALIDATION METHOD
    private Set<String> validateSkillsAgainstResume(Set<String> extractedSkills, String resumeContent) {
        Set<String> validatedSkills = new HashSet<>();
        String cleanContent = resumeContent.toLowerCase().replaceAll("[^a-zA-Z0-9\\s\\+\\#\\.]", " ");
        
        for (String skill : extractedSkills) {
            if (isSkillValid(skill, cleanContent)) {
                validatedSkills.add(skill);
            }
        }
        
        return validatedSkills;
    }
    
    private boolean isSkillValid(String skill, String resumeContent) {
        // For multi-word skills
        if (skill.contains(" ")) {
            return resumeContent.contains(skill);
        }
        
        // For single-word skills, ensure it's not part of another word
        String regex = "(?<![a-zA-Z0-9])" + Pattern.quote(skill) + "(?![a-zA-Z0-9])";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(resumeContent).find();
    }
    
    private void debugSkillExtraction(String content, Set<String> rawSkills, Set<String> validatedSkills) {
        System.out.println("=== SKILL EXTRACTION DEBUG ===");
        System.out.println("Content sample: " + content.substring(0, Math.min(200, content.length())) + "...");
        System.out.println("Raw skills found (" + rawSkills.size() + "): " + rawSkills);
        System.out.println("Validated skills (" + validatedSkills.size() + "): " + validatedSkills);
        
        Set<String> falsePositives = rawSkills.stream()
            .filter(skill -> !validatedSkills.contains(skill))
            .collect(Collectors.toSet());
            
        if (!falsePositives.isEmpty()) {
            System.out.println("False positives removed: " + falsePositives);
        }
        
        for (String skill : validatedSkills) {
            String regex;
            if (skill.contains(" ")) {
                regex = "\\b" + Pattern.quote(skill) + "\\b";
            } else {
                regex = "(?<![a-zA-Z0-9])" + Pattern.quote(skill) + "(?![a-zA-Z0-9])";
            }
            
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(content.toLowerCase());
            
            if (matcher.find()) {
                System.out.println("✓ Valid: " + skill + " at position " + matcher.start());
            }
        }
    }
    
    private Map<String, Set<String>> categorizeSkills(Set<String> skills) {
        Map<String, Set<String>> categorized = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> category : TECHNICAL_SKILLS_BY_CATEGORY.entrySet()) {
            Set<String> matchedSkills = new HashSet<>(skills);
            matchedSkills.retainAll(category.getValue());
            
            if (!matchedSkills.isEmpty()) {
                categorized.put(category.getKey(), matchedSkills);
            }
        }
        
        return categorized;
    }
    
    private String identifyPrimaryDomain(Map<String, Set<String>> categorizedSkills) {
        if (categorizedSkills.isEmpty()) {
            return "General";
        }
        
        Map<String, Integer> domainScores = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : categorizedSkills.entrySet()) {
            String category = entry.getKey();
            String domain = DOMAIN_MAPPING.getOrDefault(category, "Other");
            int score = entry.getValue().size();
            domainScores.put(domain, domainScores.getOrDefault(domain, 0) + score);
        }
        
        // Find domain with highest score
        return domainScores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("General");
    }
    
    private Map<String, Integer> getDomainBreakdown(Map<String, Set<String>> categorizedSkills) {
        Map<String, Integer> domainBreakdown = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : categorizedSkills.entrySet()) {
            String category = entry.getKey();
            String domain = DOMAIN_MAPPING.getOrDefault(category, "Other");
            int skillCount = entry.getValue().size();
            domainBreakdown.put(domain, domainBreakdown.getOrDefault(domain, 0) + skillCount);
        }
        
        return domainBreakdown;
    }
    
    // ENHANCED JOB RECOMMENDATION GENERATION
    private List<Map<String, Object>> generateJobRecommendations(Set<String> skills, 
                                                                Map<String, Set<String>> categorizedSkills,
                                                                String primaryDomain, 
                                                                int experienceYears) {
        List<Map<String, Object>> recommendations = new ArrayList<>();
        
        // Define job roles with their required skills
        Map<String, JobRole> jobRoles = createJobRolesDatabase();
        
        for (Map.Entry<String, JobRole> entry : jobRoles.entrySet()) {
            String jobTitle = entry.getKey();
            JobRole role = entry.getValue();
            
            // Calculate match score based on skills and domain
            double matchScore = calculateJobMatchScore(skills, role, primaryDomain, experienceYears);
            
            if (matchScore >= 30) { // Only recommend if reasonable match
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("category", jobTitle);
                recommendation.put("match_score", (int) Math.round(matchScore));
                recommendation.put("matched_skills", findMatchedSkills(skills, role.requiredSkills));
                recommendation.put("missing_skills", findMissingSkills(skills, role.requiredSkills));
                recommendation.put("experience_level", getExperienceLevel(experienceYears));
                
                recommendations.add(recommendation);
            }
        }
        
        // Sort by match score descending
        recommendations.sort((a, b) -> 
            Integer.compare((Integer) b.get("match_score"), (Integer) a.get("match_score")));
        
        return recommendations.stream().limit(5).collect(Collectors.toList());
    }
    
    private double calculateJobMatchScore(Set<String> userSkills, JobRole role, 
                                        String primaryDomain, int experienceYears) {
        int matchedSkills = 0;
        Set<String> userSkillsLower = userSkills.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
            
        Set<String> requiredSkillsLower = role.requiredSkills.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
        
        for (String skill : requiredSkillsLower) {
            if (userSkillsLower.contains(skill)) {
                matchedSkills++;
            }
        }
        
        double skillMatch = (double) matchedSkills / requiredSkillsLower.size() * 100;
        
        // Bonus for domain match
        double domainBonus = role.domain.equals(primaryDomain) ? 15 : 0;
        
        // Experience adjustment
        double experienceBonus = Math.min(experienceYears * 2, 10);
        
        return Math.min(skillMatch + domainBonus + experienceBonus, 100);
    }
    
    private Set<String> findMatchedSkills(Set<String> userSkills, Set<String> requiredSkills) {
        Set<String> userSkillsLower = userSkills.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
            
        return requiredSkills.stream()
            .filter(skill -> userSkillsLower.contains(skill.toLowerCase()))
            .collect(Collectors.toSet());
    }
    
    private Set<String> findMissingSkills(Set<String> userSkills, Set<String> requiredSkills) {
        Set<String> userSkillsLower = userSkills.stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
            
        return requiredSkills.stream()
            .filter(skill -> !userSkillsLower.contains(skill.toLowerCase()))
            .collect(Collectors.toSet());
    }
    
    private String getExperienceLevel(int years) {
        if (years <= 2) return "Entry Level";
        if (years <= 5) return "Mid Level";
        if (years <= 10) return "Senior Level";
        return "Executive Level";
    }
    
    // Job role database
    private Map<String, JobRole> createJobRolesDatabase() {
        Map<String, JobRole> roles = new HashMap<>();
        
        // IT & Software roles
        roles.put("Full Stack Developer", new JobRole("IT & Software", 
            new HashSet<>(Arrays.asList("javascript", "html", "css", "react", "node.js", "mongodb", "git"))));
        
        roles.put("Frontend Developer", new JobRole("IT & Software",
            new HashSet<>(Arrays.asList("javascript", "html", "css", "react", "typescript", "webpack", "git"))));
        
        roles.put("Backend Developer", new JobRole("IT & Software",
            new HashSet<>(Arrays.asList("java", "python", "node.js", "mysql", "mongodb", "rest api", "git"))));
        
        roles.put("DevOps Engineer", new JobRole("IT & Software",
            new HashSet<>(Arrays.asList("docker", "kubernetes", "aws", "jenkins", "terraform", "linux", "git"))));
        
        roles.put("Data Scientist", new JobRole("IT & Software",
            new HashSet<>(Arrays.asList("python", "machine learning", "pandas", "numpy", "sql", "statistics", "r"))));
        
        // Engineering roles
        roles.put("Mechanical Engineer", new JobRole("Engineering",
            new HashSet<>(Arrays.asList("autocad", "solidworks", "fea", "matlab", "mechanical design", "gd&t"))));
        
        roles.put("Civil Engineer", new JobRole("Engineering",
            new HashSet<>(Arrays.asList("autocad", "structural analysis", "project management", "bim", "construction"))));
        
        // Business roles
        roles.put("Project Manager", new JobRole("Business & Management",
            new HashSet<>(Arrays.asList("project management", "agile", "scrum", "jira", "leadership", "communication"))));
        
        roles.put("Business Analyst", new JobRole("Business & Management",
            new HashSet<>(Arrays.asList("business analysis", "requirements gathering", "sql", "excel", "communication"))));
        
        return roles;
    }
    
    // Helper class for job roles
    private static class JobRole {
        String domain;
        Set<String> requiredSkills;
        
        JobRole(String domain, Set<String> requiredSkills) {
            this.domain = domain;
            this.requiredSkills = requiredSkills;
        }
    }
    
    // EXISTING EXPERIENCE EXTRACTION METHODS (keep your current implementation)
    private int extractExperienceYears(String content) {
        System.out.println("=== EXPERIENCE EXTRACTION DEBUG ===");
        
        int explicitExperience = extractExplicitExperience(content);
        
        if (explicitExperience > 0) {
            System.out.println("Found explicit experience: " + explicitExperience + " years");
            return explicitExperience;
        }
        
        int enhancedExperience = extractEnhancedExperience(content);
        if (enhancedExperience > 0) {
            System.out.println("Found enhanced experience: " + enhancedExperience + " years");
            return enhancedExperience;
        }
        
        System.out.println("No explicit experience found. Returning 0 years.");
        return 0;
    }
    
    private int extractExplicitExperience(String content) {
        Pattern experiencePattern = Pattern.compile(
            "(\\d+)[\\s\\+]*\\s*(?:years?|yrs?)(?:\\s+of)?\\s*(?:experience|exp)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = experiencePattern.matcher(content);
        if (matcher.find()) {
            try {
                int years = Integer.parseInt(matcher.group(1));
                if (years >= 0 && years <= 50) {
                    return years;
                }
            } catch (NumberFormatException e) {}
        }
        
        Pattern altPattern = Pattern.compile(
            "experience[\\s:\\-]+(\\d+)[\\s\\+]*\\s*(?:years?|yrs?)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher altMatcher = altPattern.matcher(content);
        if (altMatcher.find()) {
            try {
                int years = Integer.parseInt(altMatcher.group(1));
                if (years >= 0 && years <= 50) {
                    return years;
                }
            } catch (NumberFormatException e) {}
        }
        
        return 0;
    }
    
    private int extractEnhancedExperience(String content) {
        Pattern rangePattern = Pattern.compile(
            "(\\d+)\\s*[-–]\\s*(\\d+)\\s*(?:years?|yrs?)(?:\\s+of)?\\s*(?:experience|exp)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher rangeMatcher = rangePattern.matcher(content);
        if (rangeMatcher.find()) {
            try {
                int minYears = Integer.parseInt(rangeMatcher.group(1));
                int maxYears = Integer.parseInt(rangeMatcher.group(2));
                return Math.max(minYears, (minYears + maxYears) / 2);
            } catch (NumberFormatException e) {}
        }
        
        Pattern plusPattern = Pattern.compile(
            "(\\d+)\\s*\\+\\s*(?:years?|yrs?)(?:\\s+of)?\\s*(?:experience|exp)",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher plusMatcher = plusPattern.matcher(content);
        if (plusMatcher.find()) {
            try {
                int years = Integer.parseInt(plusMatcher.group(1));
                if (years >= 0 && years <= 50) {
                    return years;
                }
            } catch (NumberFormatException e) {}
        }
        
        int workHistoryExperience = extractExperienceFromWorkHistory(content);
        if (workHistoryExperience > 0) {
            return workHistoryExperience;
        }
        
        return 0;
    }
    
    private int extractExperienceFromWorkHistory(String content) {
        Pattern workPattern = Pattern.compile(
            "(?:19|20)\\d{2}\\s*[-–]\\s*(?:present|current|now|(?:19|20)\\d{2})",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = workPattern.matcher(content);
        List<String> workPeriods = new ArrayList<>();
        while (matcher.find()) {
            workPeriods.add(matcher.group());
        }
        
        if (!workPeriods.isEmpty()) {
            return Math.min(workPeriods.size() * 2, 20);
        }
        
        return 0;
    }
    
    // [OTHER METHODS REMAIN UNCHANGED...]
    // Backward compatibility methods
    public String convertSkillsToJson(Set<String> skills) throws JsonProcessingException {
        return objectMapper.writeValueAsString(skills);
    }
    
    public String convertCategorizedSkillsToJson(Map<String, Set<String>> categorizedSkills) throws JsonProcessingException {
        return objectMapper.writeValueAsString(categorizedSkills);
    }
    
    public String convertDomainAnalysisToJson(Map<String, Object> domainAnalysis) throws JsonProcessingException {
        return objectMapper.writeValueAsString(domainAnalysis);
    }
    
    // Legacy method for backward compatibility
    private Set<String> extractSkills(String content) {
        return extractSkillsWithPatternMatching(content);
    }
    
    // Legacy education extraction method (for backward compatibility)
    private String extractEducationLevel(String content) {
        return extractEducationLevelFinal(content);
    }
}