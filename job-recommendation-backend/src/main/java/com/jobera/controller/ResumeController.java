package com.jobera.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jobera.entity.Application;
import com.jobera.entity.Resume;
import com.jobera.repository.ApplicationRepository;
import com.jobera.service.ResumeService;

@RestController
@RequestMapping("/api/resumes")
@CrossOrigin(origins = "http://localhost:3000")
public class ResumeController {
    
    private final ResumeService resumeService;
    private final ApplicationRepository applicationRepository;
    
    private static final String ERROR_KEY = "error";
    private static final String UPLOADS_DIR = "uploads";
    
    public ResumeController(ResumeService resumeService, ApplicationRepository applicationRepository) {
        this.resumeService = resumeService;
        this.applicationRepository = applicationRepository;
    }
    
    @PostMapping("/upload/{userId}")
    public ResponseEntity<Map<String, Object>> uploadResume(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of(ERROR_KEY, "File is empty"));
            }
            
            // Ensure uploads directory exists
            ensureUploadsDirectory();
            
            Resume resume = resumeService.processAndSaveResume(userId, file);
            return ResponseEntity.ok(Map.of(
                "message", "Resume uploaded successfully",
                "resumeId", resume.getId(),
                "fileName", resume.getFileName(),
                "filePath", resume.getFilePath()
            ));
        } catch (ResumeService.ResumeProcessingException e) {
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, "Failed to upload resume: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(ERROR_KEY, "Unexpected error: " + e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Resume>> getUserResumes(@PathVariable Long userId) {
        try {
            List<Resume> resumes = resumeService.getResumesByUserId(userId);
            return ResponseEntity.ok(resumes);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }
    
    @GetMapping("/{resumeId}/analysis")
    public ResponseEntity<Map<String, Object>> analyzeResume(@PathVariable Long resumeId) {
        try {
            Map<String, Object> analysis = resumeService.analyzeResume(resumeId);
            return ResponseEntity.ok(analysis);
        } catch (ResumeService.ResumeProcessingException e) {
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, "Failed to analyze resume: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(ERROR_KEY, "Unexpected error: " + e.getMessage()));
        }
    }
    
    // DEBUG ENDPOINT - Check uploads directory
    @GetMapping("/uploads/check")
    public ResponseEntity<Map<String, Object>> checkUploadsDirectory() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            Path uploadsDir = Paths.get(UPLOADS_DIR);
            boolean exists = Files.exists(uploadsDir);
            boolean isDirectory = Files.isDirectory(uploadsDir);
            
            result.put("uploadsDirectoryExists", exists);
            result.put("isDirectory", isDirectory);
            result.put("absolutePath", uploadsDir.toAbsolutePath().toString());
            
            if (exists && isDirectory) {
                try (var paths = Files.list(uploadsDir)) {
                    List<String> files = paths
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
                    result.put("files", files);
                    result.put("fileCount", files.size());
                }
            } else {
                // Try to create the directory
                Files.createDirectories(uploadsDir);
                result.put("directoryCreated", true);
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "error", "Check failed: " + e.getMessage()
            ));
        }
    }
    
    // DEBUG ENDPOINT - Check specific application resume
    @GetMapping("/download/{applicationId}/debug")
    public ResponseEntity<Map<String, Object>> debugDownloadResume(@PathVariable Long applicationId) {
        try {
            System.out.println("=== DEBUG DOWNLOAD RESUME ===");
            System.out.println("Application ID: " + applicationId);
            
            Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
            if (applicationOpt.isEmpty()) {
                System.out.println("❌ Application not found");
                return ResponseEntity.ok(Map.of("error", "Application not found"));
            }
            
            Application application = applicationOpt.get();
            System.out.println("✅ Application found:");
            System.out.println("   - Applicant: " + application.getSafeApplicantName());
            System.out.println("   - Resume File Path: " + application.getResumeFilePath());
            System.out.println("   - Has Resume: " + (application.getResumeFilePath() != null && !application.getResumeFilePath().isEmpty()));
            
            if (application.getResumeFilePath() == null || application.getResumeFilePath().isEmpty()) {
                return ResponseEntity.ok(Map.of("error", "No resume file path in application"));
            }
            
            // Check different possible file locations
            String[] possiblePaths = {
                application.getResumeFilePath(),
                UPLOADS_DIR + "/" + application.getResumeFilePath(),
                "/" + UPLOADS_DIR + "/" + application.getResumeFilePath(),
                application.getResumeFilePath().replace("/uploads/", UPLOADS_DIR + "/"),
                application.getResumeFilePath().replace("uploads/", UPLOADS_DIR + "/"),
                new File(application.getResumeFilePath()).getName()
            };
            
            System.out.println("🔍 Checking file locations:");
            Map<String, Boolean> pathChecks = new HashMap<>();
            for (String path : possiblePaths) {
                File file = new File(path);
                boolean exists = file.exists();
                pathChecks.put(path, exists);
                System.out.println("   - " + path + " : " + (exists ? "✅ EXISTS" : "❌ NOT FOUND"));
            }
            
            // Check uploads directory
            File uploadsDir = new File(UPLOADS_DIR);
            System.out.println("📁 Uploads directory: " + uploadsDir.getAbsolutePath());
            System.out.println("📁 Uploads exists: " + uploadsDir.exists());
            System.out.println("📁 Uploads is directory: " + uploadsDir.isDirectory());
            
            List<String> uploadsFiles = new ArrayList<>();
            if (uploadsDir.exists()) {
                String[] files = uploadsDir.list();
                System.out.println("📁 Files in uploads directory: " + (files != null ? files.length : 0));
                if (files != null) {
                    for (String file : files) {
                        System.out.println("   - " + file);
                        uploadsFiles.add(file);
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "application", application.getSafeApplicantName(),
                "resumeFilePath", application.getResumeFilePath(),
                "hasResume", (application.getResumeFilePath() != null && !application.getResumeFilePath().isEmpty()),
                "pathChecks", pathChecks,
                "uploadsDirectory", uploadsDir.getAbsolutePath(),
                "uploadsFiles", uploadsFiles,
                "message", "Debug information collected"
            ));
            
        } catch (Exception e) {
            System.out.println("❌ DEBUG ERROR: " + e.getMessage());
            return ResponseEntity.ok(Map.of("error", "Debug failed: " + e.getMessage()));
        }
    }
    
    // FIXED: MAIN DOWNLOAD ENDPOINT - Now serves original uploaded file
    @GetMapping("/download/{applicationId}")
    public ResponseEntity<Resource> downloadResume(@PathVariable Long applicationId) {
        try {
            System.out.println("=== DOWNLOAD RESUME REQUEST ===");
            System.out.println("Application ID: " + applicationId);
            
            Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
            if (applicationOpt.isEmpty()) {
                System.out.println("❌ Application not found with ID: " + applicationId);
                return ResponseEntity.notFound().build();
            }
            
            Application application = applicationOpt.get();
            String resumeFilePath = application.getResumeFilePath();
            
            System.out.println("Resume File Path from DB: " + resumeFilePath);
            System.out.println("Applicant: " + application.getSafeApplicantName());
            
            // If no file path exists, try to find the original resume from the user's uploads
            if (resumeFilePath == null || resumeFilePath.isEmpty()) {
                System.out.println("⚠️ No resume file path in application, checking user's uploaded resumes...");
                
                // Get the user ID from the application
                Long userId = application.getUser() != null ? application.getUser().getId() : null;
                
                if (userId != null) {
                    // Try to get the user's latest resume
                    Optional<Resume> userResume = resumeService.getLatestResumeByUserId(userId);
                    
                    if (userResume.isPresent()) {
                        Resume resume = userResume.get();
                        String originalFilePath = resume.getFilePath();
                        System.out.println("✅ Found user's resume with path: " + originalFilePath);
                        
                        // Try to serve the original resume file
                        Path filePath = findResumeFile(originalFilePath);
                        
                        if (filePath != null && Files.exists(filePath)) {
                            System.out.println("✅ Found original resume file at: " + filePath.toAbsolutePath());
                            
                            Resource resource = new UrlResource(filePath.toUri());
                            
                            if (resource.exists() && resource.isReadable()) {
                                String contentType = determineContentType(filePath);
                                String sanitizedName = application.getSafeApplicantName().replaceAll("[^a-zA-Z0-9.-]", "_");
                                String originalFileName = resume.getFileName();
                                String extension = originalFileName.contains(".") ? 
                                    originalFileName.substring(originalFileName.lastIndexOf(".")) : 
                                    getFileExtension(filePath);
                                
                                String filename = "resume_" + sanitizedName + extension;
                                
                                System.out.println("✅ Serving original resume file: " + filename);
                                System.out.println("Content Type: " + contentType);
                                
                                return ResponseEntity.ok()
                                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                                        "attachment; filename=\"" + filename + "\"")
                                    .contentType(MediaType.parseMediaType(contentType))
                                    .body(resource);
                            }
                        }
                    }
                }
                
                // If no original resume found, generate a text resume as fallback
                System.out.println("⚠️ No original resume found, generating text resume from application data...");
                return generateResumeFromApplicationData(application);
            }

            // Try multiple possible file locations for the stored resume path
            Path filePath = findResumeFile(resumeFilePath);
            
            if (filePath != null && Files.exists(filePath)) {
                System.out.println("✅ Found file at: " + filePath.toAbsolutePath());
                
                Resource resource = new UrlResource(filePath.toUri());
                
                if (resource.exists() && resource.isReadable()) {
                    String contentType = determineContentType(filePath);
                    String sanitizedName = application.getSafeApplicantName().replaceAll("[^a-zA-Z0-9.-]", "_");
                    
                    // Try to get the original filename with correct extension
                    String originalFileName = new File(resumeFilePath).getName();
                    String extension = originalFileName.contains(".") ? 
                        originalFileName.substring(originalFileName.lastIndexOf(".")) : 
                        getFileExtension(filePath);
                    
                    String filename = "resume_" + sanitizedName + extension;
                    
                    System.out.println("✅ Serving file: " + filename);
                    System.out.println("Content Type: " + contentType);
                    
                    return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + filename + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
                }
            }
            
            // If file not found, try to find the original resume from user's uploads
            System.out.println("❌ Resume file not found at stored path, checking user's uploaded resumes...");
            
            Long userId = application.getUser() != null ? application.getUser().getId() : null;
            if (userId != null) {
                Optional<Resume> userResume = resumeService.getLatestResumeByUserId(userId);
                
                if (userResume.isPresent()) {
                    Resume resume = userResume.get();
                    String originalFilePath = resume.getFilePath();
                    
                    Path originalFilepath = findResumeFile(originalFilePath);
                    
                    if (originalFilepath != null && Files.exists(originalFilepath)) {
                        System.out.println("✅ Found user's original resume at: " + originalFilepath.toAbsolutePath());
                        
                        Resource resource = new UrlResource(originalFilepath.toUri());
                        
                        if (resource.exists() && resource.isReadable()) {
                            String contentType = determineContentType(originalFilepath);
                            String sanitizedName = application.getSafeApplicantName().replaceAll("[^a-zA-Z0-9.-]", "_");
                            String originalFileName = resume.getFileName();
                            String extension = originalFileName.contains(".") ? 
                                originalFileName.substring(originalFileName.lastIndexOf(".")) : 
                                getFileExtension(originalFilepath);
                            
                            String filename = "resume_" + sanitizedName + extension;
                            
                            return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, 
                                    "attachment; filename=\"" + filename + "\"")
                                .contentType(MediaType.parseMediaType(contentType))
                                .body(resource);
                        }
                    }
                }
            }
            
            // If all else fails, generate a resume from application data
            System.out.println("❌ No original resume file found, generating from application data...");
            return generateResumeFromApplicationData(application);
            
        } catch (Exception e) {
            System.out.println("❌ ERROR downloading resume: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // ALTERNATIVE DOWNLOAD ENDPOINT - Generate resume from application data
    @GetMapping("/download/{applicationId}/generate")
    public ResponseEntity<Resource> generateResumeFromApplication(@PathVariable Long applicationId) {
        try {
            System.out.println("=== GENERATE RESUME FROM APPLICATION DATA ===");
            
            Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
            if (applicationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Application application = applicationOpt.get();
            return generateResumeFromApplicationData(application);
            
        } catch (Exception e) {
            System.out.println("❌ Generate resume failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // TEXT-BASED RESUME DOWNLOAD
    @GetMapping("/download/{applicationId}/text")
    public ResponseEntity<Resource> downloadResumeAsText(@PathVariable Long applicationId) {
        try {
            System.out.println("=== DOWNLOAD RESUME AS TEXT ===");
            
            Optional<Application> applicationOpt = applicationRepository.findById(applicationId);
            if (applicationOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Application application = applicationOpt.get();
            
            // Generate text resume
            String resumeText = generateResumeText(application);
            byte[] textBytes = resumeText.getBytes();
            
            // Create temporary file
            Path tempFile = Files.createTempFile("resume_", ".txt");
            Files.write(tempFile, textBytes);
            
            Resource resource = new UrlResource(tempFile.toUri());
            
            String sanitizedName = application.getSafeApplicantName().replaceAll("[^a-zA-Z0-9.-]", "_");
            String filename = "resume_" + sanitizedName + ".txt";
            
            // Schedule file deletion after download
            tempFile.toFile().deleteOnExit();
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
                
        } catch (Exception e) {
            System.out.println("❌ Text download failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // FIX MISSING FILES ENDPOINT
    @PostMapping("/fix/missing-files")
    public ResponseEntity<Map<String, Object>> fixMissingResumeFiles() {
        try {
            System.out.println("=== FIXING MISSING RESUME FILES ===");
            
            Map<String, Object> result = new HashMap<>();
            List<Map<String, Object>> fixes = new ArrayList<>();
            List<Map<String, Object>> errors = new ArrayList<>();
            
            // Get all applications
            List<Application> applications = applicationRepository.findAll();
            System.out.println("Total applications: " + applications.size());
            
            for (Application application : applications) {
                try {
                    boolean needsFix = false;
                    
                    if (application.getResumeFilePath() == null || application.getResumeFilePath().isEmpty()) {
                        System.out.println("❌ No file path for application " + application.getId());
                        needsFix = true;
                    } else if (!checkPhysicalFileExists(application.getResumeFilePath())) {
                        System.out.println("❌ Missing file for application " + application.getId() + ": " + application.getResumeFilePath());
                        needsFix = true;
                    }
                    
                    if (needsFix) {
                        // Try to find the original resume from user's uploads
                        Long userId = application.getUser() != null ? application.getUser().getId() : null;
                        boolean foundOriginal = false;
                        
                        if (userId != null) {
                            Optional<Resume> userResume = resumeService.getLatestResumeByUserId(userId);
                            if (userResume.isPresent()) {
                                Resume resume = userResume.get();
                                String originalPath = resume.getFilePath();
                                
                                if (checkPhysicalFileExists(originalPath)) {
                                    application.setResumeFilePath(originalPath);
                                    applicationRepository.save(application);
                                    fixes.add(Map.of(
                                        "applicationId", application.getId(),
                                        "applicant", application.getSafeApplicantName(),
                                        "action", "Linked to original resume",
                                        "newPath", originalPath
                                    ));
                                    foundOriginal = true;
                                    System.out.println("✅ Linked to original resume for application " + application.getId());
                                }
                            }
                        }
                        
                        if (!foundOriginal) {
                            // Create a placeholder resume file
                            boolean created = createPlaceholderResume(application);
                            if (created) {
                                fixes.add(Map.of(
                                    "applicationId", application.getId(),
                                    "applicant", application.getSafeApplicantName(),
                                    "action", "Created placeholder resume",
                                    "newPath", application.getResumeFilePath()
                                ));
                                System.out.println("✅ Created placeholder for application " + application.getId());
                            } else {
                                errors.add(Map.of(
                                    "applicationId", application.getId(),
                                    "error", "Failed to create placeholder"
                                ));
                            }
                        }
                    }
                } catch (Exception e) {
                    errors.add(Map.of(
                        "applicationId", application.getId(),
                        "error", "Processing failed: " + e.getMessage()
                    ));
                }
            }
            
            result.put("fixesApplied", fixes);
            result.put("errors", errors);
            result.put("totalApplications", applications.size());
            result.put("fixedCount", fixes.size());
            result.put("errorCount", errors.size());
            
            System.out.println("✅ Fix completed: " + fixes.size() + " applications fixed");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            System.out.println("❌ Fix failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Fix failed: " + e.getMessage()));
        }
    }
    
    // ALTERNATIVE DOWNLOAD ENDPOINT - Direct by resume ID
    @GetMapping("/{resumeId}/download")
    public ResponseEntity<Resource> downloadResumeById(@PathVariable Long resumeId) {
        try {
            System.out.println("=== DOWNLOAD RESUME BY ID ===");
            System.out.println("Resume ID: " + resumeId);
            
            Optional<Resume> resumeOpt = resumeService.getResumeById(resumeId);
            if (resumeOpt.isEmpty()) {
                System.out.println("❌ Resume not found with ID: " + resumeId);
                return ResponseEntity.notFound().build();
            }
            
            Resume resume = resumeOpt.get();
            String filePath = resume.getFilePath();
            
            System.out.println("Resume File Path: " + filePath);
            System.out.println("User: " + resume.getUser().getFullName());
            
            if (filePath == null || filePath.isEmpty()) {
                System.out.println("❌ No file path found for resume");
                return ResponseEntity.notFound().build();
            }
            
            // Handle file path resolution
            Path path = findResumeFile(filePath);
            
            if (path == null || !Files.exists(path)) {
                System.out.println("❌ File does not exist at any location");
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(path.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(path);
                String sanitizedName = resume.getUser().getFullName().replaceAll("[^a-zA-Z0-9.-]", "_");
                String originalFileName = resume.getFileName();
                String extension = originalFileName.contains(".") ? 
                    originalFileName.substring(originalFileName.lastIndexOf(".")) : 
                    getFileExtension(path);
                
                String filename = "resume_" + sanitizedName + "_" + resume.getId() + extension;
                
                System.out.println("✅ Serving resume file: " + filename);
                
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
            } else {
                System.out.println("❌ File exists but is not readable");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.out.println("❌ ERROR downloading resume by ID: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<?> getLatestResume(@PathVariable Long userId) {
        try {
            System.out.println("=== GET LATEST RESUME FOR USER ===");
            System.out.println("User ID: " + userId);
            
            Optional<Resume> latestResume = resumeService.getLatestResumeByUserId(userId);
            
            if (latestResume.isPresent()) {
                Resume resume = latestResume.get();
                System.out.println("✅ Found latest resume with ID: " + resume.getId());
                System.out.println("   Skills: " + resume.getSkills());
                System.out.println("   Experience: " + resume.getExperienceYears());
                System.out.println("   Education: " + resume.getEducationLevel());
                
                // Create a safe response without circular references
                Map<String, Object> safeResponse = new HashMap<>();
                safeResponse.put("id", resume.getId());
                safeResponse.put("fileName", resume.getFileName());
                safeResponse.put("filePath", resume.getFilePath());
                safeResponse.put("skills", resume.getSkills());
                safeResponse.put("experienceYears", resume.getExperienceYears());
                safeResponse.put("educationLevel", resume.getEducationLevel());
                safeResponse.put("uploadDate", resume.getUploadDate());
                
                return ResponseEntity.ok(safeResponse);
            } else {
                System.out.println("❌ No resume found for user: " + userId);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("message", "No resume found for user");
                errorResponse.put("status", "NOT_FOUND");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error fetching latest resume: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to fetch resume: " + e.getMessage());
            errorResponse.put("status", "ERROR");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    // ===== NEW DEBUG ENDPOINT - Check all resumes for a user =====
    @GetMapping("/debug/user/{userId}")
    public ResponseEntity<Map<String, Object>> debugUserResumes(@PathVariable Long userId) {
        try {
            System.out.println("=== DEBUG: CHECKING USER RESUMES ===");
            System.out.println("User ID: " + userId);
            
            List<Resume> resumes = resumeService.getResumesByUserId(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("resumeCount", resumes.size());
            
            List<Map<String, Object>> resumeDetails = new ArrayList<>();
            for (Resume resume : resumes) {
                Map<String, Object> details = new HashMap<>();
                details.put("id", resume.getId());
                details.put("fileName", resume.getFileName());
                details.put("filePath", resume.getFilePath());
                details.put("skills", resume.getSkills());
                details.put("experienceYears", resume.getExperienceYears());
                details.put("educationLevel", resume.getEducationLevel());
                details.put("uploadDate", resume.getUploadDate());
                resumeDetails.add(details);
            }
            
            response.put("resumes", resumeDetails);
            
            System.out.println("✅ Found " + resumes.size() + " resumes for user " + userId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error in debug endpoint: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    // ========== HELPER METHODS ==========
    
    private void ensureUploadsDirectory() {
        try {
            Path uploadsDir = Paths.get(UPLOADS_DIR);
            if (!Files.exists(uploadsDir)) {
                Files.createDirectories(uploadsDir);
                System.out.println("✅ Created uploads directory: " + uploadsDir.toAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("❌ Failed to create uploads directory: " + e.getMessage());
        }
    }
    
    private boolean checkPhysicalFileExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) return false;
        
        try {
            // Try multiple possible locations
            String[] possiblePaths = {
                filePath,
                UPLOADS_DIR + "/" + filePath,
                "/" + UPLOADS_DIR + "/" + filePath,
                filePath.replace("/uploads/", UPLOADS_DIR + "/"),
                filePath.replace("uploads/", UPLOADS_DIR + "/"),
                new File(filePath).getName(),
                UPLOADS_DIR + "/" + new File(filePath).getName()
            };
            
            for (String path : possiblePaths) {
                File file = new File(path);
                if (file.exists() && file.isFile() && file.canRead()) {
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error checking file existence: " + e.getMessage());
        }
        return false;
    }
    
    private Path findResumeFile(String resumeFilePath) {
        System.out.println("🔍 Searching for resume file: " + resumeFilePath);
        
        if (resumeFilePath == null || resumeFilePath.isEmpty()) {
            System.out.println("❌ Resume file path is null or empty");
            return null;
        }
        
        List<Path> possiblePaths = new ArrayList<>();
        
        // Get just the filename
        String fileName = new File(resumeFilePath).getName();
        
        // Original path from database
        possiblePaths.add(Paths.get(resumeFilePath).normalize());
        
        // Various uploads directory combinations
        possiblePaths.add(Paths.get(UPLOADS_DIR, resumeFilePath).normalize());
        possiblePaths.add(Paths.get(UPLOADS_DIR, fileName).normalize());
        possiblePaths.add(Paths.get("/" + UPLOADS_DIR, resumeFilePath).normalize());
        possiblePaths.add(Paths.get("/" + UPLOADS_DIR, fileName).normalize());
        
        if (resumeFilePath.startsWith("/uploads/")) {
            String relativePath = resumeFilePath.substring("/uploads/".length());
            possiblePaths.add(Paths.get(UPLOADS_DIR, relativePath).normalize());
            possiblePaths.add(Paths.get(UPLOADS_DIR, fileName).normalize());
        }
        
        if (resumeFilePath.startsWith("uploads/")) {
            possiblePaths.add(Paths.get(resumeFilePath).normalize());
            possiblePaths.add(Paths.get("/" + resumeFilePath).normalize());
            possiblePaths.add(Paths.get(UPLOADS_DIR, fileName).normalize());
        }
        
        // Also check current directory
        possiblePaths.add(Paths.get(fileName).normalize());
        possiblePaths.add(Paths.get(UPLOADS_DIR, fileName).normalize());
        
        // Print all possible paths
        System.out.println("Possible file locations:");
        for (Path path : possiblePaths) {
            boolean exists = Files.exists(path);
            System.out.println("   - " + path.toAbsolutePath() + " : " + (exists ? "✅ EXISTS" : "❌ NOT FOUND"));
            if (exists) {
                return path;
            }
        }
        
        return null;
    }
    
    private boolean createPlaceholderResume(Application application) {
        try {
            // Ensure uploads directory exists
            Path uploadsDir = Paths.get(UPLOADS_DIR);
            Files.createDirectories(uploadsDir);
            
            // Create a simple text resume
            String resumeContent = generateResumeText(application);
            String fileName = "resume_" + application.getId() + "_" + 
                             application.getSafeApplicantName().replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
            
            Path filePath = uploadsDir.resolve(fileName);
            Files.writeString(filePath, resumeContent);
            
            // Update application with new file path
            application.setResumeFilePath("/uploads/" + fileName);
            applicationRepository.save(application);
            
            System.out.println("✅ Created placeholder resume: " + filePath);
            return true;
            
        } catch (Exception e) {
            System.out.println("❌ Failed to create placeholder: " + e.getMessage());
            return false;
        }
    }
    
    private ResponseEntity<Resource> generateResumeFromApplicationData(Application application) {
        try {
            // Generate text resume
            String resumeText = generateResumeText(application);
            byte[] textBytes = resumeText.getBytes();
            
            // Create temporary file
            Path tempFile = Files.createTempFile("resume_", ".txt");
            Files.write(tempFile, textBytes);
            
            Resource resource = new UrlResource(tempFile.toUri());
            
            String sanitizedName = application.getSafeApplicantName().replaceAll("[^a-zA-Z0-9.-]", "_");
            String filename = "resume_" + sanitizedName + ".txt";
            
            // Schedule file deletion after download
            tempFile.toFile().deleteOnExit();
            
            System.out.println("✅ Generated resume from application data");
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
                
        } catch (Exception e) {
            System.out.println("❌ Failed to generate resume: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private String generateResumeText(Application application) {
        StringBuilder resume = new StringBuilder();
        
        resume.append("RESUME\n");
        resume.append("======\n\n");
        
        resume.append("Applicant: ").append(application.getSafeApplicantName()).append("\n");
        resume.append("Email: ").append(application.getSafeApplicantEmail()).append("\n");
        if (application.getApplicantPhone() != null && !application.getApplicantPhone().isEmpty()) {
            resume.append("Phone: ").append(application.getApplicantPhone()).append("\n");
        }
        resume.append("\n");
        
        if (application.getApplicantExperience() != null) {
            resume.append("Experience: ").append(application.getApplicantExperience()).append(" years\n");
        }
        
        if (application.getApplicantEducation() != null && !application.getApplicantEducation().isEmpty()) {
            resume.append("Education: ").append(application.getApplicantEducation()).append("\n");
        }
        
        resume.append("\nSKILLS\n");
        resume.append("======\n");
        if (application.getApplicantSkills() != null && !application.getApplicantSkills().isEmpty()) {
            try {
                // Try to parse JSON skills
                String skillsJson = application.getApplicantSkills();
                if (skillsJson.startsWith("[") && skillsJson.endsWith("]")) {
                    String[] skills = skillsJson.replace("[", "").replace("]", "").replace("\"", "").split(",");
                    for (String skill : skills) {
                        String trimmedSkill = skill.trim();
                        if (!trimmedSkill.isEmpty()) {
                            resume.append("- ").append(trimmedSkill).append("\n");
                        }
                    }
                } else {
                    resume.append(application.getApplicantSkills()).append("\n");
                }
            } catch (Exception e) {
                resume.append(application.getApplicantSkills()).append("\n");
            }
        } else {
            resume.append("Not specified\n");
        }
        
        resume.append("\nCOVER LETTER\n");
        resume.append("============\n");
        if (application.getCoverLetter() != null && !application.getCoverLetter().isEmpty()) {
            resume.append(application.getCoverLetter()).append("\n");
        } else {
            resume.append("Not provided\n");
        }
        
        resume.append("\n---\n");
        resume.append("Generated from JobEra Application System\n");
        resume.append("Application Date: ").append(application.getApplicationDate()).append("\n");
        resume.append("Application ID: ").append(application.getId()).append("\n");
        
        return resume.toString();
    }
    
    private String determineContentType(Path filePath) {
        try {
            String fileName = filePath.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".pdf")) {
                return "application/pdf";
            } else if (fileName.endsWith(".doc")) {
                return "application/msword";
            } else if (fileName.endsWith(".docx")) {
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else if (fileName.endsWith(".txt")) {
                return "text/plain";
            } else {
                // Fallback to octet-stream
                return "application/octet-stream";
            }
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
    
    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(lastDotIndex);
        }
        return "";
    }
    
    private String getFileNameFromPath(String filePath) {
        return Paths.get(filePath).getFileName().toString();
    }
}