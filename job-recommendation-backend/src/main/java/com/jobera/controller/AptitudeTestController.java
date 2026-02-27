// File: src/main/java/com/jobera/controller/AptitudeTestController.java
package com.jobera.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobera.entity.AptitudeTest;
import com.jobera.entity.Application;
import com.jobera.entity.ProctoringViolation;
import com.jobera.entity.Recruiter;
import com.jobera.entity.TestAssignment;
import com.jobera.entity.TestQuestion;
import com.jobera.entity.User;
import com.jobera.repository.AptitudeTestRepository;
import com.jobera.repository.ApplicationRepository;
import com.jobera.repository.ProctoringViolationRepository;
import com.jobera.repository.RecruiterRepository;
import com.jobera.repository.TestAssignmentRepository;
import com.jobera.repository.TestQuestionRepository;

@RestController
@RequestMapping("/api/aptitude-tests")
@CrossOrigin(origins = "http://localhost:3000")
public class AptitudeTestController {
    
    // ========== CONSTANTS ==========
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String TEST_ID_KEY = "testId";
    private static final String APPLICATION_ID_KEY = "applicationId";
    private static final String ASSIGNMENT_ID_KEY = "assignmentId";
    private static final String QUESTIONS_KEY = "questions";
    private static final String ASSIGNMENT_KEY = "assignment";
    private static final String STATUS_KEY = "status";
    private static final String ANSWERS_KEY = "answers";
    
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_EXPIRED = "EXPIRED";
    private static final String STATUS_FAILED_PROCTORING = "FAILED_PROCTORING";
    
    private static final String APPLICATION_STATUS_TEST_ASSIGNED = "TEST_ASSIGNED";
    private static final String APPLICATION_STATUS_TEST_PASSED = "TEST_PASSED";
    private static final String APPLICATION_STATUS_TEST_FAILED = "TEST_FAILED";
    private static final String APPLICATION_STATUS_INTERVIEW_SCHEDULED = "INTERVIEW_SCHEDULED";
    
    // ========== REPOSITORIES ==========
    private final AptitudeTestRepository testRepository;
    private final TestQuestionRepository questionRepository;
    private final TestAssignmentRepository assignmentRepository;
    private final ApplicationRepository applicationRepository;
    private final RecruiterRepository recruiterRepository;
    private final ProctoringViolationRepository violationRepository;
    private final ObjectMapper objectMapper;

    // ========== CONSTRUCTOR ==========
    public AptitudeTestController(
            AptitudeTestRepository testRepository,
            TestQuestionRepository questionRepository,
            TestAssignmentRepository assignmentRepository,
            ApplicationRepository applicationRepository,
            RecruiterRepository recruiterRepository,
            ProctoringViolationRepository violationRepository) {
        this.testRepository = testRepository;
        this.questionRepository = questionRepository;
        this.assignmentRepository = assignmentRepository;
        this.applicationRepository = applicationRepository;
        this.recruiterRepository = recruiterRepository;
        this.violationRepository = violationRepository;
        this.objectMapper = new ObjectMapper();
    }

    // ========== TEST CREATION ==========
    
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createTest(@RequestBody Map<String, Object> request) {
        try {
            Long recruiterId = Long.valueOf(request.get("recruiterId").toString());
            String testName = (String) request.get("testName");
            String description = (String) request.get("description");
            Integer durationMinutes = Integer.valueOf(request.get("durationMinutes").toString());
            Integer passingScore = Integer.valueOf(request.get("passingScore").toString());
            
            Optional<Recruiter> recruiter = recruiterRepository.findById(recruiterId);
            if (recruiter.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Recruiter not found"));
            }
            
            AptitudeTest test = new AptitudeTest();
            test.setRecruiter(recruiter.get());
            test.setTestName(testName);
            test.setDescription(description);
            test.setDurationMinutes(durationMinutes);
            test.setPassingScore(passingScore);
            test.setTotalQuestions(0);
            test.setIsActive(true);
            
            AptitudeTest savedTest = testRepository.save(test);
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Test created successfully");
            response.put("test", createTestResponse(savedTest));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to create test: " + e.getMessage()));
        }
    }

    // ========== GET RECRUITER TESTS ==========
    
    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<List<Map<String, Object>>> getRecruiterTests(@PathVariable Long recruiterId) {
        try {
            System.out.println("=== GET RECRUITER TESTS ===");
            System.out.println("Recruiter ID: " + recruiterId);
            
            List<AptitudeTest> tests = testRepository.findByRecruiterId(recruiterId);
            System.out.println("Found " + tests.size() + " tests for recruiter " + recruiterId);
            
            List<Map<String, Object>> response = tests.stream()
                .map(this::createTestResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("❌ Error fetching recruiter tests: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(List.of());
        }
    }

    // ========== QUESTION MANAGEMENT ==========
    
    @PostMapping("/{testId}/add-question")
    public ResponseEntity<Map<String, Object>> addQuestion(
            @PathVariable Long testId,
            @RequestBody Map<String, Object> request) {
        try {
            Optional<AptitudeTest> testOpt = testRepository.findById(testId);
            if (testOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test not found"));
            }
            
            AptitudeTest test = testOpt.get();
            
            long currentCount = questionRepository.countByTestId(testId);
            if (currentCount >= 40) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test already has 40 questions"));
            }
            
            TestQuestion question = new TestQuestion();
            question.setTest(test);
            question.setQuestion((String) request.get("question"));
            question.setOptionA((String) request.get("optionA"));
            question.setOptionB((String) request.get("optionB"));
            question.setOptionC((String) request.get("optionC"));
            question.setOptionD((String) request.get("optionD"));
            question.setCorrectAnswer((String) request.get("correctAnswer"));
            question.setExplanation((String) request.get("explanation"));
            question.setCategory((String) request.get("category"));
            question.setDifficultyLevel(Integer.valueOf(request.get("difficultyLevel").toString()));
            
            TestQuestion savedQuestion = questionRepository.save(question);
            
            test.setTotalQuestions((int) questionRepository.countByTestId(testId));
            testRepository.save(test);
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Question added successfully");
            response.put("question", createQuestionResponse(savedQuestion));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to add question: " + e.getMessage()));
        }
    }

    @PostMapping("/{testId}/add-multiple-questions")
    public ResponseEntity<Map<String, Object>> addMultipleQuestions(
            @PathVariable Long testId,
            @RequestBody List<Map<String, Object>> questions) {
        try {
            Optional<AptitudeTest> testOpt = testRepository.findById(testId);
            if (testOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test not found"));
            }
            
            AptitudeTest test = testOpt.get();
            
            long currentCount = questionRepository.countByTestId(testId);
            if (currentCount + questions.size() > 40) {
                return ResponseEntity.badRequest().body(Map.of(
                    ERROR_KEY, "Cannot add " + questions.size() + " questions. Max 40 allowed. Current: " + currentCount
                ));
            }
            
            int added = 0;
            for (Map<String, Object> q : questions) {
                TestQuestion question = new TestQuestion();
                question.setTest(test);
                question.setQuestion((String) q.get("question"));
                question.setOptionA((String) q.get("optionA"));
                question.setOptionB((String) q.get("optionB"));
                question.setOptionC((String) q.get("optionC"));
                question.setOptionD((String) q.get("optionD"));
                question.setCorrectAnswer((String) q.get("correctAnswer"));
                question.setExplanation((String) q.get("explanation"));
                question.setCategory((String) q.get("category"));
                question.setDifficultyLevel(Integer.valueOf(q.get("difficultyLevel").toString()));
                
                questionRepository.save(question);
                added++;
            }
            
            test.setTotalQuestions((int) questionRepository.countByTestId(testId));
            testRepository.save(test);
            
            return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, added + " questions added successfully",
                "totalQuestions", test.getTotalQuestions()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to add questions: " + e.getMessage()));
        }
    }

    @GetMapping("/{testId}/questions")
    public ResponseEntity<Map<String, Object>> getTestQuestions(@PathVariable Long testId) {
        try {
            Optional<AptitudeTest> testOpt = testRepository.findById(testId);
            if (testOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test not found"));
            }
            
            List<TestQuestion> questions = questionRepository.findByTestId(testId);
            
            Map<String, Object> response = new HashMap<>();
            response.put(TEST_ID_KEY, testId);
            response.put("testName", testOpt.get().getTestName());
            response.put("totalQuestions", questions.size());
            response.put(QUESTIONS_KEY, questions.stream().map(this::createQuestionResponse).collect(Collectors.toList()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to get questions: " + e.getMessage()));
        }
    }

    // ========== TEST ASSIGNMENT ==========
    
    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignTest(@RequestBody Map<String, Object> request) {
        try {
            Long testId = Long.valueOf(request.get(TEST_ID_KEY).toString());
            Long applicationId = Long.valueOf(request.get(APPLICATION_ID_KEY).toString());
            Integer deadlineHours = Integer.valueOf(request.get("deadlineHours").toString());
            
            Optional<AptitudeTest> testOpt = testRepository.findById(testId);
            if (testOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test not found"));
            }
            
            Optional<Application> appOpt = applicationRepository.findById(applicationId);
            if (appOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Application not found"));
            }
            
            Optional<TestAssignment> existing = assignmentRepository.findByApplicationIdAndTestId(applicationId, testId);
            if (existing.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test already assigned to this candidate"));
            }
            
            TestAssignment assignment = new TestAssignment();
            assignment.setTest(testOpt.get());
            assignment.setApplication(appOpt.get());
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setDeadline(LocalDateTime.now().plusHours(deadlineHours));
            assignment.setStatus(STATUS_PENDING);
            assignment.setTotalQuestions(testOpt.get().getTotalQuestions());
            assignment.setProctoringActive(false);
            assignment.setProctoringTerminated(false);
            assignment.setWarningCount(0);
            assignment.setNoFaceViolations(0);
            assignment.setMultipleFacesViolations(0);
            assignment.setMobileDetectedViolations(0);
            assignment.setLookingAwayViolations(0);
            
            TestAssignment saved = assignmentRepository.save(assignment);
            
            Application application = appOpt.get();
            application.setStatus(APPLICATION_STATUS_TEST_ASSIGNED);
            applicationRepository.save(application);
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Test assigned successfully");
            response.put(ASSIGNMENT_KEY, createAssignmentResponse(saved));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to assign test: " + e.getMessage()));
        }
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<Map<String, Object>> getApplicationTest(@PathVariable Long applicationId) {
        try {
            List<TestAssignment> assignments = assignmentRepository.findByApplicationId(applicationId);
            
            if (assignments.isEmpty()) {
                return ResponseEntity.ok(Map.of("hasTest", false));
            }
            
            TestAssignment latest = assignments.get(assignments.size() - 1);
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasTest", true);
            response.put(ASSIGNMENT_KEY, createAssignmentResponse(latest));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to get test: " + e.getMessage()));
        }
    }

    // ========== TEST STARTING ==========
    
    @GetMapping("/start/{assignmentId}")
    public ResponseEntity<Map<String, Object>> startTest(@PathVariable String assignmentId) {
        try {
            // Extract numeric ID if it contains prefix
            String numericId = assignmentId.replaceAll("[^0-9]", "");
            Long id = Long.parseLong(numericId);
            
            Optional<TestAssignment> assignmentOpt = assignmentRepository.findById(id);
            if (assignmentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Assignment not found: " + assignmentId));
            }
            
            TestAssignment assignment = assignmentOpt.get();
            
            // Check if deadline passed
            if (LocalDateTime.now().isAfter(assignment.getDeadline())) {
                assignment.setStatus(STATUS_EXPIRED);
                assignmentRepository.save(assignment);
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test deadline has passed"));
            }
            
            // Check if already completed or failed
            if (STATUS_COMPLETED.equals(assignment.getStatus()) || 
                STATUS_FAILED_PROCTORING.equals(assignment.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test already completed or failed"));
            }
            
            // If already in progress, return existing questions
            if (STATUS_IN_PROGRESS.equals(assignment.getStatus()) && assignment.getStartedAt() != null) {
                Map<String, Object> response = new HashMap<>();
                response.put(ASSIGNMENT_KEY, createAssignmentResponse(assignment));
                response.put(QUESTIONS_KEY, getTestQuestionsForCandidate(assignment.getTest().getId()));
                
                // Parse existing answers if any
                if (assignment.getAnswers() != null && !assignment.getAnswers().isEmpty()) {
                    try {
                        Map<String, String> existingAnswers = objectMapper.readValue(
                            assignment.getAnswers(), 
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, String>>() {}
                        );
                        response.put("existingAnswers", existingAnswers);
                    } catch (Exception e) {
                        response.put("existingAnswers", null);
                    }
                } else {
                    response.put("existingAnswers", null);
                }
                
                return ResponseEntity.ok(response);
            }
            
            // Start new test
            assignment.setStartedAt(LocalDateTime.now());
            assignment.setStatus(STATUS_IN_PROGRESS);
            assignment.setProctoringActive(true);
            assignment.setProctoringStartedAt(LocalDateTime.now());
            assignment.setWarningCount(0);
            assignment.setNoFaceViolations(0);
            assignment.setMultipleFacesViolations(0);
            assignment.setMobileDetectedViolations(0);
            assignment.setLookingAwayViolations(0);
            assignmentRepository.save(assignment);
            
            Map<String, Object> response = new HashMap<>();
            response.put(ASSIGNMENT_KEY, createAssignmentResponse(assignment));
            response.put(QUESTIONS_KEY, getTestQuestionsForCandidate(assignment.getTest().getId()));
            response.put("existingAnswers", null);
            
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Invalid assignment ID format: " + assignmentId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to start test: " + e.getMessage()));
        }
    }

    // ========== FIXED TEST SUBMISSION ==========
    
    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitTest(@RequestBody Map<String, Object> request) {
        try {
            Long assignmentId = Long.valueOf(request.get(ASSIGNMENT_ID_KEY).toString());
            @SuppressWarnings("unchecked")
            Map<String, String> answers = (Map<String, String>) request.get(ANSWERS_KEY);
            
            Optional<TestAssignment> assignmentOpt = assignmentRepository.findById(assignmentId);
            if (assignmentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Assignment not found"));
            }
            
            TestAssignment assignment = assignmentOpt.get();
            
            // Stop proctoring
            assignment.setProctoringActive(false);
            assignment.setProctoringEndedAt(LocalDateTime.now());
            
            // Check if already completed
            if (STATUS_COMPLETED.equals(assignment.getStatus()) || 
                STATUS_FAILED_PROCTORING.equals(assignment.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test already submitted or failed"));
            }
            
            // Check if expired
            if (LocalDateTime.now().isAfter(assignment.getDeadline())) {
                assignment.setStatus(STATUS_EXPIRED);
                assignmentRepository.save(assignment);
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test deadline has passed"));
            }
            
            // Get all questions
            List<TestQuestion> questions = questionRepository.findByTestId(assignment.getTest().getId());
            
            // Calculate score
            int correctCount = 0;
            Map<String, String> answersMap = answers != null ? answers : new HashMap<>();
            
            for (TestQuestion q : questions) {
                String userAnswer = answersMap.get(q.getId().toString());
                if (userAnswer != null && userAnswer.equalsIgnoreCase(q.getCorrectAnswer())) {
                    correctCount++;
                }
            }
            
            int totalQuestions = questions.size();
            int score = correctCount;
            int percentage = totalQuestions > 0 ? (score * 100) / totalQuestions : 0;
            int passingScore = assignment.getTest().getPassingScore();
            boolean passed = percentage >= passingScore;
            
            System.out.println("=== TEST SUBMISSION ===");
            System.out.println("Assignment ID: " + assignmentId);
            System.out.println("Total Questions: " + totalQuestions);
            System.out.println("Correct Answers: " + correctCount);
            System.out.println("Score: " + score + "/" + totalQuestions);
            System.out.println("Percentage: " + percentage + "%");
            System.out.println("Passing Score: " + passingScore + "%");
            System.out.println("Passed: " + passed);
            
            // Save results
            assignment.setCompletedAt(LocalDateTime.now());
            assignment.setStatus(STATUS_COMPLETED);
            assignment.setScore(score);
            assignment.setCorrectAnswers(correctCount);
            assignment.setTotalQuestions(totalQuestions);
            assignment.setPassed(passed);
            
            if (answers != null) {
                assignment.setAnswers(objectMapper.writeValueAsString(answers));
            }
            
            assignmentRepository.save(assignment);
            
            // Update application status
            Application application = assignment.getApplication();
            if (application != null) {
                if (passed) {
                    application.setStatus(APPLICATION_STATUS_TEST_PASSED);
                    System.out.println("✅ Application status updated to: TEST_PASSED");
                } else {
                    application.setStatus(APPLICATION_STATUS_TEST_FAILED);
                    System.out.println("❌ Application status updated to: TEST_FAILED");
                }
                applicationRepository.save(application);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Test submitted successfully");
            response.put("score", score);
            response.put("totalQuestions", totalQuestions);
            response.put("percentage", percentage);
            response.put("passed", passed);
            response.put("passingScore", passingScore);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error submitting test: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to submit test: " + e.getMessage()));
        }
    }

    // ========== PROCTORING ENDPOINTS ==========
    
    @PostMapping("/{assignmentId}/violations")
    public ResponseEntity<Map<String, Object>> receiveViolations(
            @PathVariable String assignmentId,
            @RequestBody Map<String, Object> violations) {
        try {
            System.out.println("=".repeat(60));
            System.out.println("📹 RECEIVED PROCTORING VIOLATIONS");
            System.out.println("=".repeat(60));
            System.out.println("Assignment ID: " + assignmentId);
            
            // Extract numeric ID
            String numericId = assignmentId.replaceAll("[^0-9]", "");
            Long id = Long.parseLong(numericId);
            
            Optional<TestAssignment> assignmentOpt = assignmentRepository.findById(id);
            if (assignmentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Assignment not found: " + assignmentId));
            }
            
            TestAssignment assignment = assignmentOpt.get();
            Application application = assignment.getApplication();
            
            System.out.println("Current assignment status: " + assignment.getStatus());
            System.out.println("Current application status: " + (application != null ? application.getStatus() : "null"));
            
            // Update warning count
            Integer warningCount = (Integer) violations.get("warning_count");
            if (warningCount != null) {
                assignment.setWarningCount(warningCount);
            }
            
            // Update violation counts
            Integer multipleFaceViolations = (Integer) violations.get("multiple_face_violations");
            if (multipleFaceViolations != null) {
                assignment.setMultipleFacesViolations(multipleFaceViolations);
            }
            
            Integer noFaceViolations = (Integer) violations.get("no_face_violations");
            if (noFaceViolations != null) {
                assignment.setNoFaceViolations(noFaceViolations);
            }
            
            // Check if test was terminated
            Boolean terminated = (Boolean) violations.get("terminated");
            if (terminated != null && terminated) {
                String reason = (String) violations.get("termination_reason");
                if (reason == null) reason = "Test terminated due to proctoring violations";
                
                // Mark test as failed
                assignment.setStatus(STATUS_FAILED_PROCTORING);
                assignment.setProctoringTerminated(true);
                assignment.setProctoringTerminationReason(reason);
                assignment.setCompletedAt(LocalDateTime.now());
                assignment.setPassed(false);
                
                System.out.println("🛑 TEST MARKED AS FAILED: " + reason);
                
                // Update application status to FAILED
                if (application != null) {
                    application.setStatus(APPLICATION_STATUS_TEST_FAILED);
                    applicationRepository.save(application);
                    System.out.println("✅ Application status updated to TEST_FAILED for user: " + 
                        (application.getUser() != null ? application.getUser().getEmail() : "unknown"));
                } else {
                    System.out.println("❌ Application is null, cannot update status");
                }
            }
            
            // Save individual violations if provided
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> warnings = (List<Map<String, Object>>) violations.get("violations");
            
            if (warnings != null) {
                for (Map<String, Object> warning : warnings) {
                    ProctoringViolation violation = new ProctoringViolation();
                    violation.setTestAssignment(assignment);
                    violation.setViolationType((String) warning.get("type"));
                    violation.setMessage((String) warning.get("message"));
                    violation.setCritical((Boolean) warning.get("critical"));
                    
                    String timestamp = (String) warning.get("timestamp");
                    if (timestamp != null) {
                        try {
                            violation.setTimestamp(LocalDateTime.parse(timestamp));
                        } catch (Exception e) {
                            violation.setTimestamp(LocalDateTime.now());
                        }
                    } else {
                        violation.setTimestamp(LocalDateTime.now());
                    }
                    
                    violationRepository.save(violation);
                }
            }
            
            assignmentRepository.save(assignment);
            
            System.out.println("✅ Violations processed successfully");
            if (terminated != null && terminated) {
                System.out.println("✅ Test marked as FAILED in database");
                System.out.println("New assignment status: " + assignment.getStatus());
                if (application != null) {
                    System.out.println("New application status: " + application.getStatus());
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Violations received successfully");
            if (terminated != null && terminated) {
                response.put("test_failed", true);
                response.put("reason", violations.get("termination_reason"));
                response.put("new_status", STATUS_FAILED_PROCTORING);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error receiving violations: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, "Failed to process violations: " + e.getMessage()));
        }
    }

    @GetMapping("/{assignmentId}/violations")
    public ResponseEntity<Map<String, Object>> getViolations(@PathVariable String assignmentId) {
        try {
            String numericId = assignmentId.replaceAll("[^0-9]", "");
            Long id = Long.parseLong(numericId);
            
            Optional<TestAssignment> assignmentOpt = assignmentRepository.findById(id);
            if (assignmentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Assignment not found"));
            }
            
            TestAssignment assignment = assignmentOpt.get();
            List<ProctoringViolation> violations = violationRepository.findByTestAssignmentId(id);
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("total_violations", violations.size());
            summary.put("no_face", assignment.getNoFaceViolations());
            summary.put("multiple_faces", assignment.getMultipleFacesViolations());
            summary.put("mobile_detected", assignment.getMobileDetectedViolations());
            summary.put("looking_away", assignment.getLookingAwayViolations());
            summary.put("warning_count", assignment.getWarningCount());
            summary.put("terminated", assignment.getProctoringTerminated());
            summary.put("termination_reason", assignment.getProctoringTerminationReason());
            summary.put("status", assignment.getStatus());
            
            List<Map<String, Object>> violationList = violations.stream()
                .map(v -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", v.getId());
                    map.put("type", v.getViolationType());
                    map.put("message", v.getMessage());
                    map.put("critical", v.getCritical());
                    map.put("timestamp", v.getTimestamp());
                    return map;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "summary", summary,
                "violations", violationList
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, "Failed to get violations: " + e.getMessage()));
        }
    }

    @GetMapping("/{assignmentId}/proctoring-status")
    public ResponseEntity<Map<String, Object>> getProctoringStatus(@PathVariable String assignmentId) {
        try {
            String numericId = assignmentId.replaceAll("[^0-9]", "");
            Long id = Long.parseLong(numericId);
            
            Optional<TestAssignment> assignmentOpt = assignmentRepository.findById(id);
            if (assignmentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Assignment not found"));
            }
            
            TestAssignment assignment = assignmentOpt.get();
            
            Map<String, Object> status = new HashMap<>();
            status.put("active", assignment.getProctoringActive());
            status.put("terminated", assignment.getProctoringTerminated());
            status.put("termination_reason", assignment.getProctoringTerminationReason());
            status.put("warning_count", assignment.getWarningCount());
            status.put("started_at", assignment.getProctoringStartedAt());
            status.put("ended_at", assignment.getProctoringEndedAt());
            status.put("no_face_violations", assignment.getNoFaceViolations());
            status.put("multiple_faces_violations", assignment.getMultipleFacesViolations());
            status.put("mobile_detected_violations", assignment.getMobileDetectedViolations());
            status.put("looking_away_violations", assignment.getLookingAwayViolations());
            status.put("status", assignment.getStatus());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, "Failed to get proctoring status: " + e.getMessage()));
        }
    }

    @PostMapping("/{assignmentId}/stop-proctoring")
    public ResponseEntity<Map<String, Object>> stopProctoring(@PathVariable String assignmentId) {
        try {
            String numericId = assignmentId.replaceAll("[^0-9]", "");
            Long id = Long.parseLong(numericId);
            
            Optional<TestAssignment> assignmentOpt = assignmentRepository.findById(id);
            if (assignmentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Assignment not found"));
            }
            
            TestAssignment assignment = assignmentOpt.get();
            assignment.setProctoringActive(false);
            assignment.setProctoringEndedAt(LocalDateTime.now());
            assignmentRepository.save(assignment);
            
            return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Proctoring stopped successfully"
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(ERROR_KEY, "Failed to stop proctoring: " + e.getMessage()));
        }
    }

    // ========== RESULTS & STATISTICS ==========
    
    @GetMapping("/results/{assignmentId}")
    public ResponseEntity<Map<String, Object>> getTestResults(@PathVariable Long assignmentId) {
        try {
            Optional<TestAssignment> assignmentOpt = assignmentRepository.findById(assignmentId);
            if (assignmentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Assignment not found"));
            }
            
            TestAssignment assignment = assignmentOpt.get();
            
            // Get candidate information
            Application application = assignment.getApplication();
            User candidate = application != null ? application.getUser() : null;
            
            Map<String, Object> response = new HashMap<>();
            response.put(ASSIGNMENT_KEY, createAssignmentResponse(assignment));
            response.put("candidateName", candidate != null ? candidate.getFullName() : "Unknown");
            response.put("candidateEmail", candidate != null ? candidate.getEmail() : "Unknown");
            response.put("jobTitle", application != null && application.getJobPosting() != null ? 
                application.getJobPosting().getTitle() : "Unknown Job");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to get results: " + e.getMessage()));
        }
    }

    @GetMapping("/test/{testId}/results")
    public ResponseEntity<Map<String, Object>> getTestResultsByTestId(@PathVariable Long testId) {
        try {
            Optional<AptitudeTest> testOpt = testRepository.findById(testId);
            if (testOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test not found"));
            }
            
            List<TestAssignment> assignments = assignmentRepository.findByTestId(testId);
            
            List<Map<String, Object>> results = assignments.stream()
                .map(assignment -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("assignmentId", assignment.getId());
                    result.put(TEST_ID_KEY, assignment.getTest().getId());
                    result.put("testName", assignment.getTest().getTestName());
                    result.put(APPLICATION_ID_KEY, assignment.getApplication().getId());
                    result.put("candidateName", assignment.getApplication().getSafeApplicantName());
                    result.put("candidateEmail", assignment.getApplication().getSafeApplicantEmail());
                    result.put(STATUS_KEY, assignment.getStatus());
                    result.put("score", assignment.getScore() != null ? assignment.getScore() : 0);
                    result.put("correctAnswers", assignment.getCorrectAnswers() != null ? assignment.getCorrectAnswers() : 0);
                    result.put("totalQuestions", assignment.getTotalQuestions() != null ? assignment.getTotalQuestions() : 0);
                    result.put("passed", assignment.getPassed() != null ? assignment.getPassed() : false);
                    result.put("percentage", assignment.getPercentage());
                    result.put("passingScore", assignment.getTest().getPassingScore());
                    result.put("startedAt", assignment.getStartedAt());
                    result.put("completedAt", assignment.getCompletedAt());
                    
                    // Add proctoring info
                    result.put("proctoring_terminated", assignment.getProctoringTerminated());
                    result.put("warning_count", assignment.getWarningCount());
                    result.put("no_face_violations", assignment.getNoFaceViolations());
                    result.put("multiple_faces_violations", assignment.getMultipleFacesViolations());
                    result.put("mobile_detected_violations", assignment.getMobileDetectedViolations());
                    result.put("termination_reason", assignment.getProctoringTerminationReason());
                    
                    return result;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                TEST_ID_KEY, testId,
                "testName", testOpt.get().getTestName(),
                "results", results
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to get results: " + e.getMessage()));
        }
    }

    @GetMapping("/test/{testId}/results/unscheduled")
    public ResponseEntity<Map<String, Object>> getUnscheduledTestResults(@PathVariable Long testId) {
        try {
            Optional<AptitudeTest> testOpt = testRepository.findById(testId);
            if (testOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test not found"));
            }
            
            List<TestAssignment> assignments = assignmentRepository.findByTestId(testId);
            
            List<Map<String, Object>> results = new ArrayList<>();
            int scheduledCount = 0;
            
            for (TestAssignment assignment : assignments) {
                Application application = assignment.getApplication();
                // Only include candidates who have NOT been scheduled for interview
                if (application != null && !APPLICATION_STATUS_INTERVIEW_SCHEDULED.equals(application.getStatus())) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("assignmentId", assignment.getId());
                    result.put(TEST_ID_KEY, assignment.getTest().getId());
                    result.put("testName", assignment.getTest().getTestName());
                    result.put("applicationId", application.getId());
                    result.put("candidateName", application.getSafeApplicantName());
                    result.put("candidateEmail", application.getSafeApplicantEmail());
                    result.put(STATUS_KEY, assignment.getStatus());
                    result.put("score", assignment.getScore() != null ? assignment.getScore() : 0);
                    result.put("correctAnswers", assignment.getCorrectAnswers() != null ? assignment.getCorrectAnswers() : 0);
                    result.put("totalQuestions", assignment.getTotalQuestions() != null ? assignment.getTotalQuestions() : 0);
                    result.put("passed", assignment.getPassed() != null ? assignment.getPassed() : false);
                    result.put("percentage", assignment.getPercentage());
                    result.put("passingScore", assignment.getTest().getPassingScore());
                    result.put("startedAt", assignment.getStartedAt());
                    result.put("completedAt", assignment.getCompletedAt());
                    results.add(result);
                } else {
                    scheduledCount++;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put(TEST_ID_KEY, testId);
            response.put("testName", testOpt.get().getTestName());
            response.put("totalAssignments", assignments.size());
            response.put("scheduledCount", scheduledCount);
            response.put("unscheduledCount", results.size());
            response.put("results", results);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to get results: " + e.getMessage()));
        }
    }

    @GetMapping("/stats/recruiter/{recruiterId}")
    public ResponseEntity<Map<String, Object>> getRecruiterTestStats(@PathVariable Long recruiterId) {
        try {
            List<TestAssignment> allAssignments = assignmentRepository.findByRecruiterId(recruiterId);
            
            long totalAssigned = allAssignments.size();
            long completed = allAssignments.stream()
                .filter(a -> STATUS_COMPLETED.equals(a.getStatus()))
                .count();
            long passed = allAssignments.stream()
                .filter(a -> Boolean.TRUE.equals(a.getPassed()))
                .count();
            long failed = allAssignments.stream()
                .filter(a -> STATUS_FAILED_PROCTORING.equals(a.getStatus()) || 
                             (STATUS_COMPLETED.equals(a.getStatus()) && !Boolean.TRUE.equals(a.getPassed())))
                .count();
            long pending = allAssignments.stream()
                .filter(a -> STATUS_PENDING.equals(a.getStatus()))
                .count();
            long inProgress = allAssignments.stream()
                .filter(a -> STATUS_IN_PROGRESS.equals(a.getStatus()))
                .count();
            long expired = allAssignments.stream()
                .filter(a -> STATUS_EXPIRED.equals(a.getStatus()))
                .count();
            long proctoringTerminated = allAssignments.stream()
                .filter(a -> Boolean.TRUE.equals(a.getProctoringTerminated()))
                .count();
            
            double avgScore = allAssignments.stream()
                .filter(a -> a.getScore() != null)
                .mapToInt(TestAssignment::getScore)
                .average()
                .orElse(0.0);
            
            double passRate = totalAssigned > 0 ? (passed * 100.0 / totalAssigned) : 0;
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalAssigned", totalAssigned);
            response.put("completed", completed);
            response.put("passed", passed);
            response.put("failed", failed);
            response.put("pending", pending);
            response.put("inProgress", inProgress);
            response.put("expired", expired);
            response.put("proctoringTerminated", proctoringTerminated);
            response.put("averageScore", Math.round(avgScore * 100.0) / 100.0);
            response.put("passRate", Math.round(passRate * 100.0) / 100.0);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to get stats: " + e.getMessage()));
        }
    }

    // ========== HELPER METHODS ==========
    
    private Map<String, Object> createTestResponse(AptitudeTest test) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", test.getId());
        response.put("testName", test.getTestName());
        response.put("description", test.getDescription());
        response.put("durationMinutes", test.getDurationMinutes());
        response.put("totalQuestions", test.getTotalQuestions());
        response.put("passingScore", test.getPassingScore());
        response.put("isActive", test.getIsActive());
        response.put("createdAt", test.getCreatedAt());
        return response;
    }

    private Map<String, Object> createQuestionResponse(TestQuestion question) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", question.getId());
        response.put("question", question.getQuestion());
        response.put("optionA", question.getOptionA());
        response.put("optionB", question.getOptionB());
        response.put("optionC", question.getOptionC());
        response.put("optionD", question.getOptionD());
        response.put("correctAnswer", question.getCorrectAnswer());
        response.put("explanation", question.getExplanation());
        response.put("category", question.getCategory());
        response.put("difficultyLevel", question.getDifficultyLevel());
        return response;
    }

    private Map<String, Object> createAssignmentResponse(TestAssignment assignment) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", assignment.getId());
        response.put(TEST_ID_KEY, assignment.getTest().getId());
        response.put("testName", assignment.getTest().getTestName());
        response.put(APPLICATION_ID_KEY, assignment.getApplication().getId());
        response.put("assignedAt", assignment.getAssignedAt());
        response.put("startedAt", assignment.getStartedAt());
        response.put("completedAt", assignment.getCompletedAt());
        response.put("deadline", assignment.getDeadline());
        response.put(STATUS_KEY, assignment.getStatus());
        response.put("score", assignment.getScore() != null ? assignment.getScore() : 0);
        response.put("correctAnswers", assignment.getCorrectAnswers() != null ? assignment.getCorrectAnswers() : 0);
        response.put("totalQuestions", assignment.getTotalQuestions() != null ? assignment.getTotalQuestions() : 0);
        response.put("passed", assignment.getPassed() != null ? assignment.getPassed() : false);
        response.put("percentage", assignment.getPercentage());
        response.put("passingScore", assignment.getTest().getPassingScore());
        
        // Proctoring info
        response.put("proctoringActive", assignment.getProctoringActive());
        response.put("proctoringTerminated", assignment.getProctoringTerminated());
        response.put("terminationReason", assignment.getProctoringTerminationReason());
        response.put("warningCount", assignment.getWarningCount());
        response.put("noFaceViolations", assignment.getNoFaceViolations());
        response.put("multipleFacesViolations", assignment.getMultipleFacesViolations());
        response.put("mobileDetectedViolations", assignment.getMobileDetectedViolations());
        
        return response;
    }

    private List<Map<String, Object>> getTestQuestionsForCandidate(Long testId) {
        List<TestQuestion> questions = questionRepository.findByTestId(testId);
        return questions.stream()
            .map(q -> {
                Map<String, Object> qMap = new HashMap<>();
                qMap.put("id", q.getId());
                qMap.put("question", q.getQuestion());
                qMap.put("optionA", q.getOptionA());
                qMap.put("optionB", q.getOptionB());
                qMap.put("optionC", q.getOptionC());
                qMap.put("optionD", q.getOptionD());
                qMap.put("category", q.getCategory());
                return qMap;
            })
            .collect(Collectors.toList());
    }
}