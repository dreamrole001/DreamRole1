package com.jobera.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jobera.entity.Resume;
import com.jobera.entity.User;
import com.jobera.repository.ResumeRepository;
import com.jobera.repository.UserRepository;

@Service
public class ResumeService {
    
    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final ResumeProcessingService resumeProcessingService;
    
    public ResumeService(ResumeRepository resumeRepository, 
                        UserRepository userRepository, 
                        ResumeProcessingService resumeProcessingService) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.resumeProcessingService = resumeProcessingService;
    }
    
    public Resume processAndSaveResume(Long userId, MultipartFile file) throws ResumeProcessingException {
        System.out.println("=== PROCESSING AND SAVING RESUME ===");
        System.out.println("User ID: " + userId);
        System.out.println("File Name: " + file.getOriginalFilename());
        System.out.println("File Size: " + file.getSize() + " bytes");
        
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            System.out.println("❌ User not found with ID: " + userId);
            throw new ResumeProcessingException("User not found with ID: " + userId);
        }
        
        // Ensure uploads directory exists
        java.nio.file.Path uploadsDir = Paths.get("uploads");
        try {
            Files.createDirectories(uploadsDir);
            System.out.println("✅ Uploads directory: " + uploadsDir.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("❌ Failed to create uploads directory: " + e.getMessage());
            throw new ResumeProcessingException("Failed to create uploads directory");
        }
        
        String textContent;
        String fileExtension = getFileExtension(file.getOriginalFilename());
        String savedFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        java.nio.file.Path filePath = uploadsDir.resolve(savedFileName);
        
        try {
            // Save the file physically
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ File saved to: " + filePath.toAbsolutePath());
            
            if ("pdf".equalsIgnoreCase(fileExtension)) {
                System.out.println("📄 Processing PDF file...");
                textContent = resumeProcessingService.extractTextFromPdf(file);
            } else if ("txt".equalsIgnoreCase(fileExtension)) {
                System.out.println("📄 Processing text file...");
                textContent = resumeProcessingService.extractTextFromTextFile(file);
            } else {
                System.out.println("📄 Processing as text file...");
                textContent = resumeProcessingService.extractTextFromTextFile(file);
            }
        } catch (IOException e) {
            System.out.println("❌ Failed to save/process file: " + e.getMessage());
            throw new ResumeProcessingException("Failed to save/process file: " + e.getMessage(), e);
        }
        
        System.out.println("✅ Text extraction successful. Content length: " + textContent.length() + " characters");
        
        // Parse resume content
        Map<String, Object> parsedData = resumeProcessingService.parseResumeContent(textContent);
        
        // Create and populate resume entity
        Resume resume = new Resume();
        resume.setUser(user.get());
        resume.setFileName(file.getOriginalFilename());
        resume.setFilePath("/uploads/" + savedFileName); // Store relative path for web access
        resume.setParsedText(textContent);
        
        try {
            @SuppressWarnings("unchecked")
            Set<String> skills = (Set<String>) parsedData.get("skills");
            String skillsJson = resumeProcessingService.convertSkillsToJson(skills);
            resume.setSkills(skillsJson);
            System.out.println("✅ Extracted " + skills.size() + " skills from resume");
        } catch (Exception e) {
            System.out.println("❌ Failed to process skills: " + e.getMessage());
            throw new ResumeProcessingException("Failed to process skills: " + e.getMessage(), e);
        }
        
        // Set experience and education
        Integer experienceYears = (Integer) parsedData.get("experienceYears");
        String educationLevel = (String) parsedData.get("educationLevel");
        
        resume.setExperienceYears(experienceYears);
        resume.setEducationLevel(educationLevel);
        
        System.out.println("✅ Resume analysis completed:");
        System.out.println("   - Experience: " + experienceYears + " years");
        System.out.println("   - Education: " + educationLevel);
        
        // Save resume to database
        Resume savedResume = resumeRepository.save(resume);
        System.out.println("✅ Resume saved successfully with ID: " + savedResume.getId());
        
        return savedResume;
    }
    
    public List<Resume> getResumesByUserId(Long userId) {
        try {
            System.out.println("📋 Fetching resumes for user ID: " + userId);
            List<Resume> resumes = resumeRepository.findByUserId(userId);
            System.out.println("✅ Found " + resumes.size() + " resumes for user " + userId);
            return resumes;
        } catch (Exception e) {
            System.out.println("❌ Error fetching resumes for user " + userId + ": " + e.getMessage());
            return List.of();
        }
    }
    
    // FIXED: Enhanced getLatestResumeByUserId with better error handling
    public Optional<Resume> getLatestResumeByUserId(Long userId) {
        try {
            System.out.println("📋 Fetching latest resume for user ID: " + userId);
            
            // First check if user exists
            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty()) {
                System.out.println("❌ User not found with ID: " + userId);
                return Optional.empty();
            }
            
            List<Resume> resumes = resumeRepository.findByUserId(userId);
            System.out.println("✅ Found " + resumes.size() + " total resumes for user " + userId);
            
            if (resumes.isEmpty()) {
                System.out.println("ℹ️ No resume found for user " + userId);
                return Optional.empty();
            }
            
            // Sort by upload date descending and get the first one
            Optional<Resume> latestResume = resumes.stream()
                .sorted((r1, r2) -> r2.getUploadDate().compareTo(r1.getUploadDate()))
                .findFirst();
            
            if (latestResume.isPresent()) {
                Resume resume = latestResume.get();
                System.out.println("✅ Found latest resume with ID: " + resume.getId());
                System.out.println("   Skills: " + resume.getSkills());
                System.out.println("   Experience: " + resume.getExperienceYears());
                System.out.println("   Education: " + resume.getEducationLevel());
            }
            
            return latestResume;
            
        } catch (Exception e) {
            System.out.println("❌ Error fetching latest resume for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
    
    public Optional<Resume> getResumeById(Long resumeId) {
        try {
            System.out.println("📋 Fetching resume by ID: " + resumeId);
            Optional<Resume> resume = resumeRepository.findById(resumeId);
            if (resume.isPresent()) {
                System.out.println("✅ Found resume with ID: " + resumeId);
            } else {
                System.out.println("❌ Resume not found with ID: " + resumeId);
            }
            return resume;
        } catch (Exception e) {
            System.out.println("❌ Error fetching resume by ID " + resumeId + ": " + e.getMessage());
            return Optional.empty();
        }
    }
    
    public Map<String, Object> analyzeResume(Long resumeId) throws ResumeProcessingException {
        System.out.println("🔍 Analyzing resume with ID: " + resumeId);
        
        Optional<Resume> resume = resumeRepository.findById(resumeId);
        if (resume.isPresent()) {
            Resume resumeEntity = resume.get();
            System.out.println("✅ Resume found: " + resumeEntity.getFileName());
            System.out.println("📝 Parsing resume content...");
            
            Map<String, Object> analysis = resumeProcessingService.parseResumeContent(resumeEntity.getParsedText());
            System.out.println("✅ Resume analysis completed for ID: " + resumeId);
            
            return analysis;
        }
        
        System.out.println("❌ Resume not found with ID: " + resumeId);
        throw new ResumeProcessingException("Resume not found with ID: " + resumeId);
    }
    
    @SuppressWarnings("unchecked")
    public Set<String> extractSkillsFromText(String text) {
        try {
            System.out.println("🔧 Extracting skills from text (length: " + text.length() + " characters)");
            Map<String, Object> parsedData = resumeProcessingService.parseResumeContent(text);
            Set<String> skills = (Set<String>) parsedData.get("skills");
            System.out.println("✅ Extracted " + skills.size() + " skills from text");
            return skills;
        } catch (Exception e) {
            System.out.println("❌ Error extracting skills from text: " + e.getMessage());
            return Set.of();
        }
    }
    
    public boolean deleteResume(Long resumeId) {
        try {
            System.out.println("🗑️ Deleting resume with ID: " + resumeId);
            
            Optional<Resume> resume = resumeRepository.findById(resumeId);
            if (resume.isPresent()) {
                // Also delete the physical file
                Resume resumeEntity = resume.get();
                if (resumeEntity.getFilePath() != null && !resumeEntity.getFilePath().isEmpty()) {
                    try {
                        java.nio.file.Path filePath = Paths.get(resumeEntity.getFilePath().replace("/uploads/", "uploads/"));
                        if (Files.exists(filePath)) {
                            Files.delete(filePath);
                            System.out.println("✅ Physical file deleted: " + filePath);
                        }
                    } catch (IOException e) {
                        System.out.println("⚠️ Could not delete physical file: " + e.getMessage());
                    }
                }
                
                resumeRepository.delete(resumeEntity);
                System.out.println("✅ Resume deleted successfully: " + resumeId);
                return true;
            } else {
                System.out.println("❌ Resume not found for deletion: " + resumeId);
                return false;
            }
        } catch (Exception e) {
            System.out.println("❌ Error deleting resume " + resumeId + ": " + e.getMessage());
            return false;
        }
    }
    
    public List<Resume> getAllResumes() {
        try {
            System.out.println("📋 Fetching all resumes");
            List<Resume> resumes = resumeRepository.findAll();
            System.out.println("✅ Found " + resumes.size() + " resumes in total");
            return resumes;
        } catch (Exception e) {
            System.out.println("❌ Error fetching all resumes: " + e.getMessage());
            return List.of();
        }
    }
    
    public boolean userHasResume(Long userId) {
        try {
            System.out.println("🔍 Checking if user " + userId + " has resumes");
            List<Resume> resumes = resumeRepository.findByUserId(userId);
            boolean hasResume = !resumes.isEmpty();
            System.out.println("✅ User " + userId + " has resumes: " + hasResume);
            return hasResume;
        } catch (Exception e) {
            System.out.println("❌ Error checking if user has resume: " + e.getMessage());
            return false;
        }
    }
    
    public int getResumeCountByUserId(Long userId) {
        try {
            System.out.println("📊 Getting resume count for user: " + userId);
            List<Resume> resumes = resumeRepository.findByUserId(userId);
            int count = resumes.size();
            System.out.println("✅ User " + userId + " has " + count + " resumes");
            return count;
        } catch (Exception e) {
            System.out.println("❌ Error getting resume count: " + e.getMessage());
            return 0;
        }
    }
    
    // NEW METHOD: Fix file paths for existing resumes
    public boolean fixResumeFilePath(Long resumeId) {
        try {
            System.out.println("🔧 Fixing file path for resume ID: " + resumeId);
            
            Optional<Resume> resumeOpt = resumeRepository.findById(resumeId);
            if (resumeOpt.isEmpty()) {
                System.out.println("❌ Resume not found: " + resumeId);
                return false;
            }
            
            Resume resume = resumeOpt.get();
            String currentPath = resume.getFilePath();
            
            if (currentPath == null || currentPath.isEmpty()) {
                System.out.println("❌ No file path to fix");
                return false;
            }
            
            // Check if file exists at current path
            File currentFile = new File(currentPath);
            if (currentFile.exists()) {
                System.out.println("✅ File already exists at: " + currentPath);
                return true;
            }
            
            // Try to find the file in uploads directory
            String fileName = new File(currentPath).getName();
            java.nio.file.Path uploadsDir = Paths.get("uploads");
            java.nio.file.Path possiblePath = uploadsDir.resolve(fileName);
            
            if (Files.exists(possiblePath)) {
                // Update with correct path
                resume.setFilePath("/uploads/" + fileName);
                resumeRepository.save(resume);
                System.out.println("✅ Fixed file path to: /uploads/" + fileName);
                return true;
            }
            
            System.out.println("❌ Could not find file for resume: " + resumeId);
            return false;
            
        } catch (Exception e) {
            System.out.println("❌ Error fixing resume file path: " + e.getMessage());
            return false;
        }
    }
    
    // NEW METHOD: Get physical file path for resume
    public java.nio.file.Path getPhysicalFilePath(Long resumeId) {
        try {
            Optional<Resume> resumeOpt = resumeRepository.findById(resumeId);
            if (resumeOpt.isPresent()) {
                Resume resume = resumeOpt.get();
                String filePath = resume.getFilePath();
                
                if (filePath != null && !filePath.isEmpty()) {
                    // Convert web path to physical path
                    if (filePath.startsWith("/uploads/")) {
                        return Paths.get("uploads", filePath.substring("/uploads/".length()));
                    } else if (filePath.startsWith("uploads/")) {
                        return Paths.get(filePath);
                    } else {
                        return Paths.get("uploads", filePath);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error getting physical file path: " + e.getMessage());
        }
        return null;
    }
    
    // NEW METHOD: Check if physical file exists
    public boolean physicalFileExists(Long resumeId) {
        try {
            java.nio.file.Path physicalPath = getPhysicalFilePath(resumeId);
            return physicalPath != null && Files.exists(physicalPath);
        } catch (Exception e) {
            System.out.println("❌ Error checking physical file existence: " + e.getMessage());
            return false;
        }
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf(".") == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    public static class ResumeProcessingException extends Exception {
        public ResumeProcessingException(String message) {
            super(message);
        }
        
        public ResumeProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}