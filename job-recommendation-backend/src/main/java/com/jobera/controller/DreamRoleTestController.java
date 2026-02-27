// File: src/main/java/com/jobera/controller/DreamRoleTestController.java
package com.jobera.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobera.entity.Application;
import com.jobera.entity.DreamRoleTest;
import com.jobera.entity.DreamRoleTestAssignment;
import com.jobera.entity.DreamRoleTestQuestion;
import com.jobera.entity.QuestionBank;
import com.jobera.entity.Recruiter;
import com.jobera.entity.User;
import com.jobera.repository.ApplicationRepository;
import com.jobera.repository.DreamRoleTestAssignmentRepository;
import com.jobera.repository.DreamRoleTestQuestionRepository;
import com.jobera.repository.DreamRoleTestRepository;
import com.jobera.repository.QuestionBankRepository;
import com.jobera.repository.RecruiterRepository;

@RestController
@RequestMapping("/api/dream-role-tests")
@CrossOrigin(origins = "http://localhost:3000")
public class DreamRoleTestController {
    
    private static final String ERROR_KEY = "error";
    private static final String MESSAGE_KEY = "message";
    private static final String STATUS_KEY = "status";
    private static final String COMPLETED_STATUS = "COMPLETED";
    private static final String IN_PROGRESS_STATUS = "IN_PROGRESS";
    private static final String PENDING_STATUS = "PENDING";
    private static final String EXPIRED_STATUS = "EXPIRED";
    private static final String INTERVIEW_SCHEDULED_STATUS = "INTERVIEW_SCHEDULED";
    
    private final DreamRoleTestRepository dreamRoleTestRepository;
    private final DreamRoleTestQuestionRepository questionRepository;
    private final DreamRoleTestAssignmentRepository assignmentRepository;
    private final QuestionBankRepository questionBankRepository;
    private final ApplicationRepository applicationRepository;
    private final RecruiterRepository recruiterRepository;
    private final ObjectMapper objectMapper;

    public DreamRoleTestController(
            DreamRoleTestRepository dreamRoleTestRepository,
            DreamRoleTestQuestionRepository questionRepository,
            DreamRoleTestAssignmentRepository assignmentRepository,
            QuestionBankRepository questionBankRepository,
            ApplicationRepository applicationRepository,
            RecruiterRepository recruiterRepository) {
        this.dreamRoleTestRepository = dreamRoleTestRepository;
        this.questionRepository = questionRepository;
        this.assignmentRepository = assignmentRepository;
        this.questionBankRepository = questionBankRepository;
        this.applicationRepository = applicationRepository;
        this.recruiterRepository = recruiterRepository;
        this.objectMapper = new ObjectMapper();
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createDreamRoleTest(@RequestBody Map<String, Object> request) {
        try {
            Long recruiterId = Long.valueOf(request.get("recruiterId").toString());
            String testName = (String) request.get("testName");
            String description = (String) request.get("description");
            Integer durationMinutes = Integer.valueOf(request.get("durationMinutes").toString());
            Integer passingScore = Integer.valueOf(request.get("passingScore").toString());
            String targetBranch = (String) request.get("targetBranch");
            
            Optional<Recruiter> recruiter = recruiterRepository.findById(recruiterId);
            if (recruiter.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Recruiter not found"));
            }
            
            long availableQuestions = questionBankRepository.countByBranchAndIsActiveTrue(targetBranch);
            if (availableQuestions < 40) {
                return ResponseEntity.badRequest().body(Map.of(
                    ERROR_KEY, "Not enough questions for " + targetBranch + " branch. Available: " + availableQuestions
                ));
            }
            
            DreamRoleTest test = new DreamRoleTest();
            test.setRecruiter(recruiter.get());
            test.setTestName(testName);
            test.setDescription(description);
            test.setDurationMinutes(durationMinutes);
            test.setPassingScore(passingScore);
            test.setTargetBranch(targetBranch);
            test.setAptitudeQuestions(40);
            test.setTechnicalQuestions(10);
            test.setTotalQuestions(50);
            test.setShuffleQuestions(true);
            
            DreamRoleTest savedTest = dreamRoleTestRepository.save(test);
            
            Map<String, Object> response = new HashMap<>();
            response.put(MESSAGE_KEY, "DreamRole Test created successfully");
            response.put("test", createTestResponse(savedTest));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to create test: " + e.getMessage()));
        }
    }

    @PostMapping("/{testId}/add-technical-questions")
    public ResponseEntity<Map<String, Object>> addTechnicalQuestions(
            @PathVariable Long testId,
            @RequestBody List<Map<String, Object>> questions) {
        try {
            Optional<DreamRoleTest> testOpt = dreamRoleTestRepository.findById(testId);
            if (testOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test not found"));
            }
            
            DreamRoleTest test = testOpt.get();
            
            long currentCount = questionRepository.findByDreamRoleTestId(testId).size();
            if (currentCount + questions.size() > 10) {
                return ResponseEntity.badRequest().body(Map.of(
                    ERROR_KEY, "Cannot add more than 10 technical questions. Current: " + currentCount
                ));
            }
            
            for (Map<String, Object> q : questions) {
                DreamRoleTestQuestion question = new DreamRoleTestQuestion();
                question.setDreamRoleTest(test);
                question.setQuestion((String) q.get("question"));
                question.setOptionA((String) q.get("optionA"));
                question.setOptionB((String) q.get("optionB"));
                question.setOptionC((String) q.get("optionC"));
                question.setOptionD((String) q.get("optionD"));
                question.setCorrectAnswer((String) q.get("correctAnswer"));
                question.setExplanation((String) q.get("explanation"));
                question.setDifficultyLevel((String) q.get("difficultyLevel"));
                
                questionRepository.save(question);
            }
            
            long newCount = questionRepository.findByDreamRoleTestId(testId).size();
            
            return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Technical questions added successfully",
                "addedCount", questions.size(),
                "totalTechnicalQuestions", newCount
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to add questions: " + e.getMessage()));
        }
    }

    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<List<Map<String, Object>>> getRecruiterDreamRoleTests(@PathVariable Long recruiterId) {
        try {
            List<DreamRoleTest> tests = dreamRoleTestRepository.findByRecruiterId(recruiterId);
            return ResponseEntity.ok(tests.stream().map(this::createTestResponse).collect(Collectors.toList()));
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/branches/stats")
    public ResponseEntity<Map<String, Object>> getBranchStats() {
        try {
            List<String> branches = questionBankRepository.findAllBranches();
            Map<String, Long> branchCounts = new HashMap<>();
            
            for (String branch : branches) {
                long count = questionBankRepository.countByBranchAndIsActiveTrue(branch);
                branchCounts.put(branch, count);
            }
            
            return ResponseEntity.ok(Map.of(
                "branches", branches,
                "branchCounts", branchCounts
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to get stats: " + e.getMessage()));
        }
    }

    @PostMapping("/assign")
    public ResponseEntity<Map<String, Object>> assignDreamRoleTest(@RequestBody Map<String, Object> request) {
        try {
            Long testId = Long.valueOf(request.get("testId").toString());
            Long applicationId = Long.valueOf(request.get("applicationId").toString());
            Integer deadlineHours = Integer.valueOf(request.get("deadlineHours").toString());
            
            Optional<DreamRoleTest> testOpt = dreamRoleTestRepository.findById(testId);
            if (testOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test not found"));
            }
            
            Optional<Application> appOpt = applicationRepository.findById(applicationId);
            if (appOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Application not found"));
            }
            
            DreamRoleTest test = testOpt.get();
            Application application = appOpt.get();
            
            Optional<DreamRoleTestAssignment> existing = assignmentRepository.findByApplicationIdAndDreamRoleTestId(applicationId, testId);
            if (existing.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test already assigned to this candidate"));
            }
            
            List<QuestionBank> aptitudeQuestions = questionBankRepository.findRandomQuestionsByBranch(
                test.getTargetBranch(), 
                PageRequest.of(0, 40)
            );
            
            List<DreamRoleTestQuestion> technicalQuestions = questionRepository.findByDreamRoleTestId(testId);
            
            List<Map<String, Object>> allQuestions = new ArrayList<>();
            int questionNumber = 1;
            
            for (QuestionBank q : aptitudeQuestions) {
                Map<String, Object> questionMap = new HashMap<>();
                questionMap.put("id", "bank_" + q.getId());
                questionMap.put("questionNumber", questionNumber++);
                questionMap.put("question", q.getQuestion());
                questionMap.put("optionA", q.getOptionA());
                questionMap.put("optionB", q.getOptionB());
                questionMap.put("optionC", q.getOptionC());
                questionMap.put("optionD", q.getOptionD());
                questionMap.put("category", q.getCategory());
                questionMap.put("branch", q.getBranch());
                questionMap.put("difficulty", q.getDifficultyLevel());
                questionMap.put("correctAnswer", q.getCorrectAnswer());
                questionMap.put("explanation", q.getExplanation());
                allQuestions.add(questionMap);
                
                q.setTimesUsed(q.getTimesUsed() + 1);
                questionBankRepository.save(q);
            }
            
            for (DreamRoleTestQuestion q : technicalQuestions) {
                Map<String, Object> questionMap = new HashMap<>();
                questionMap.put("id", "tech_" + q.getId());
                questionMap.put("questionNumber", questionNumber++);
                questionMap.put("question", q.getQuestion());
                questionMap.put("optionA", q.getOptionA());
                questionMap.put("optionB", q.getOptionB());
                questionMap.put("optionC", q.getOptionC());
                questionMap.put("optionD", q.getOptionD());
                questionMap.put("category", "Technical");
                questionMap.put("difficulty", q.getDifficultyLevel());
                questionMap.put("correctAnswer", q.getCorrectAnswer());
                questionMap.put("explanation", q.getExplanation());
                allQuestions.add(questionMap);
            }
            
            if (test.getShuffleQuestions()) {
                Collections.shuffle(allQuestions);
                for (int i = 0; i < allQuestions.size(); i++) {
                    allQuestions.get(i).put("questionNumber", i + 1);
                }
            }
            
            DreamRoleTestAssignment assignment = new DreamRoleTestAssignment();
            assignment.setDreamRoleTest(test);
            assignment.setApplication(application);
            assignment.setAssignedAt(LocalDateTime.now());
            assignment.setDeadline(LocalDateTime.now().plusHours(deadlineHours));
            assignment.setStatus(PENDING_STATUS);
            assignment.setTotalQuestions(allQuestions.size());
            assignment.setQuestions(objectMapper.writeValueAsString(allQuestions));
            
            DreamRoleTestAssignment saved = assignmentRepository.save(assignment);
            
            application.setStatus("TEST_ASSIGNED");
            applicationRepository.save(application);
            
            return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "DreamRole Test assigned successfully",
                "assignmentId", saved.getId()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to assign test: " + e.getMessage()));
        }
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<Map<String, Object>> getDreamRoleTestByApplication(@PathVariable Long applicationId) {
        try {
            List<DreamRoleTestAssignment> assignments = assignmentRepository.findByApplicationId(applicationId);
            
            if (assignments.isEmpty()) {
                return ResponseEntity.ok(Map.of("hasTest", false));
            }
            
            DreamRoleTestAssignment latest = assignments.get(assignments.size() - 1);
            
            Map<String, Object> response = new HashMap<>();
            response.put("hasTest", true);
            response.put("assignment", createAssignmentResponse(latest));
            response.put("testType", "dreamrole");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to get test: " + e.getMessage()));
        }
    }

    @GetMapping("/start/{assignmentId}")
    public ResponseEntity<Map<String, Object>> startDreamRoleTest(@PathVariable Long assignmentId) {
        try {
            Optional<DreamRoleTestAssignment> assignmentOpt = assignmentRepository.findById(assignmentId);
            if (assignmentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Assignment not found"));
            }
            
            DreamRoleTestAssignment assignment = assignmentOpt.get();
            
            if (LocalDateTime.now().isAfter(assignment.getDeadline())) {
                assignment.setStatus(EXPIRED_STATUS);
                assignmentRepository.save(assignment);
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test deadline has passed"));
            }
            
            if (COMPLETED_STATUS.equals(assignment.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test already completed"));
            }
            
            List<Map<String, Object>> questions = objectMapper.readValue(
                assignment.getQuestions(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            List<Map<String, Object>> candidateQuestions = questions.stream()
                .map(q -> {
                    Map<String, Object> safeQ = new HashMap<>();
                    safeQ.put("id", q.get("id"));
                    safeQ.put("questionNumber", q.get("questionNumber"));
                    safeQ.put("question", q.get("question"));
                    safeQ.put("optionA", q.get("optionA"));
                    safeQ.put("optionB", q.get("optionB"));
                    safeQ.put("optionC", q.get("optionC"));
                    safeQ.put("optionD", q.get("optionD"));
                    safeQ.put("category", q.get("category"));
                    safeQ.put("branch", q.get("branch"));
                    safeQ.put("difficulty", q.get("difficulty"));
                    return safeQ;
                })
                .collect(Collectors.toList());
            
            if (IN_PROGRESS_STATUS.equals(assignment.getStatus()) && assignment.getStartedAt() != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("assignment", createAssignmentResponse(assignment));
                response.put("questions", candidateQuestions);
                response.put("existingAnswers", assignment.getAnswers() != null ? 
                    objectMapper.readValue(assignment.getAnswers(), new TypeReference<Map<String, String>>() {}) : null);
                return ResponseEntity.ok(response);
            }
            
            assignment.setStartedAt(LocalDateTime.now());
            assignment.setStatus(IN_PROGRESS_STATUS);
            assignmentRepository.save(assignment);
            
            Map<String, Object> response = new HashMap<>();
            response.put("assignment", createAssignmentResponse(assignment));
            response.put("questions", candidateQuestions);
            response.put("existingAnswers", null);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to start test: " + e.getMessage()));
        }
    }

    @PostMapping("/submit")
    public ResponseEntity<Map<String, Object>> submitDreamRoleTest(@RequestBody Map<String, Object> request) {
        try {
            Long assignmentId = Long.valueOf(request.get("assignmentId").toString());
            @SuppressWarnings("unchecked")
            Map<String, String> answers = (Map<String, String>) request.get("answers");
            
            Optional<DreamRoleTestAssignment> assignmentOpt = assignmentRepository.findById(assignmentId);
            if (assignmentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Assignment not found"));
            }
            
            DreamRoleTestAssignment assignment = assignmentOpt.get();
            
            if (COMPLETED_STATUS.equals(assignment.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test already submitted"));
            }
            
            if (LocalDateTime.now().isAfter(assignment.getDeadline())) {
                assignment.setStatus(EXPIRED_STATUS);
                assignmentRepository.save(assignment);
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test deadline has passed"));
            }
            
            List<Map<String, Object>> questions = objectMapper.readValue(
                assignment.getQuestions(), 
                new TypeReference<List<Map<String, Object>>>() {}
            );
            
            int correctCount = 0;
            for (Map<String, Object> q : questions) {
                String questionId = (String) q.get("id");
                String userAnswer = answers.get(questionId);
                String correctAnswer = (String) q.get("correctAnswer");
                
                if (userAnswer != null && userAnswer.equals(correctAnswer)) {
                    correctCount++;
                }
            }
            
            int totalQuestions = questions.size();
            int score = correctCount;
            int percentage = totalQuestions > 0 ? (score * 100) / totalQuestions : 0;
            boolean passed = percentage >= assignment.getDreamRoleTest().getPassingScore();
            
            assignment.setCompletedAt(LocalDateTime.now());
            assignment.setStatus(COMPLETED_STATUS);
            assignment.setScore(score);
            assignment.setCorrectAnswers(correctCount);
            assignment.setTotalQuestions(totalQuestions);
            assignment.setPassed(passed);
            assignment.setAnswers(objectMapper.writeValueAsString(answers));
            
            assignmentRepository.save(assignment);
            
            if (passed) {
                Application application = assignment.getApplication();
                application.setStatus("TEST_PASSED");
                applicationRepository.save(application);
            } else {
                Application application = assignment.getApplication();
                application.setStatus("TEST_FAILED");
                applicationRepository.save(application);
            }
            
            return ResponseEntity.ok(Map.of(
                MESSAGE_KEY, "Test submitted successfully",
                "score", score,
                "totalQuestions", totalQuestions,
                "percentage", percentage,
                "passed", passed,
                "passingScore", assignment.getDreamRoleTest().getPassingScore()
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to submit test: " + e.getMessage()));
        }
    }

    @GetMapping("/results/{assignmentId}")
    public ResponseEntity<Map<String, Object>> getDreamRoleTestResults(@PathVariable Long assignmentId) {
        try {
            Optional<DreamRoleTestAssignment> assignmentOpt = assignmentRepository.findById(assignmentId);
            if (assignmentOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Assignment not found"));
            }
            
            DreamRoleTestAssignment assignment = assignmentOpt.get();
            
            Application application = assignment.getApplication();
            User candidate = application.getUser();
            
            if (candidate == null) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Candidate not found"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("assignment", createAssignmentResponse(assignment));
            response.put("candidateName", candidate.getFullName());
            response.put("candidateEmail", candidate.getEmail());
            response.put("jobTitle", application.getJobPosting() != null ? 
                application.getJobPosting().getTitle() : "Unknown Job");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to get results: " + e.getMessage()));
        }
    }

    @GetMapping("/test/{testId}/results")
    public ResponseEntity<Map<String, Object>> getDreamRoleTestResultsByTestId(@PathVariable Long testId) {
        try {
            Optional<DreamRoleTest> testOpt = dreamRoleTestRepository.findById(testId);
            if (testOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test not found"));
            }
            
            List<DreamRoleTestAssignment> assignments = assignmentRepository.findByDreamRoleTestId(testId);
            
            List<Map<String, Object>> results = assignments.stream()
                .map(assignment -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("assignmentId", assignment.getId());
                    result.put("testId", assignment.getDreamRoleTest().getId());
                    result.put("testName", assignment.getDreamRoleTest().getTestName());
                    result.put("applicationId", assignment.getApplication().getId());
                    result.put("candidateName", assignment.getApplication().getSafeApplicantName());
                    result.put("candidateEmail", assignment.getApplication().getSafeApplicantEmail());
                    result.put(STATUS_KEY, assignment.getStatus());
                    result.put("score", assignment.getScore() != null ? assignment.getScore() : 0);
                    result.put("correctAnswers", assignment.getCorrectAnswers() != null ? assignment.getCorrectAnswers() : 0);
                    result.put("totalQuestions", assignment.getTotalQuestions() != null ? assignment.getTotalQuestions() : 0);
                    result.put("passed", assignment.getPassed() != null ? assignment.getPassed() : false);
                    result.put("percentage", assignment.getPercentage());
                    result.put("passingScore", assignment.getDreamRoleTest().getPassingScore());
                    result.put("startedAt", assignment.getStartedAt());
                    result.put("completedAt", assignment.getCompletedAt());
                    return result;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "testId", testId,
                "testName", testOpt.get().getTestName(),
                "results", results
            ));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Failed to get results: " + e.getMessage()));
        }
    }

    @GetMapping("/test/{testId}/results/unscheduled")
    public ResponseEntity<Map<String, Object>> getUnscheduledDreamRoleTestResults(@PathVariable Long testId) {
        try {
            Optional<DreamRoleTest> testOpt = dreamRoleTestRepository.findById(testId);
            if (testOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(ERROR_KEY, "Test not found"));
            }
            
            List<DreamRoleTestAssignment> assignments = assignmentRepository.findByDreamRoleTestId(testId);
            
            List<Map<String, Object>> results = new ArrayList<>();
            int scheduledCount = 0;
            
            for (DreamRoleTestAssignment assignment : assignments) {
                Application application = assignment.getApplication();
                // Only include candidates who have NOT been scheduled for interview
                if (application != null && !INTERVIEW_SCHEDULED_STATUS.equals(application.getStatus())) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("assignmentId", assignment.getId());
                    result.put("testId", assignment.getDreamRoleTest().getId());
                    result.put("testName", assignment.getDreamRoleTest().getTestName());
                    result.put("applicationId", application.getId());
                    result.put("candidateName", application.getSafeApplicantName());
                    result.put("candidateEmail", application.getSafeApplicantEmail());
                    result.put(STATUS_KEY, assignment.getStatus());
                    result.put("score", assignment.getScore() != null ? assignment.getScore() : 0);
                    result.put("correctAnswers", assignment.getCorrectAnswers() != null ? assignment.getCorrectAnswers() : 0);
                    result.put("totalQuestions", assignment.getTotalQuestions() != null ? assignment.getTotalQuestions() : 0);
                    result.put("passed", assignment.getPassed() != null ? assignment.getPassed() : false);
                    result.put("percentage", assignment.getPercentage());
                    result.put("passingScore", assignment.getDreamRoleTest().getPassingScore());
                    result.put("startedAt", assignment.getStartedAt());
                    result.put("completedAt", assignment.getCompletedAt());
                    results.add(result);
                } else {
                    scheduledCount++;
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("testId", testId);
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

    private Map<String, Object> createTestResponse(DreamRoleTest test) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", test.getId());
        response.put("testName", test.getTestName());
        response.put("description", test.getDescription());
        response.put("durationMinutes", test.getDurationMinutes());
        response.put("targetBranch", test.getTargetBranch());
        response.put("aptitudeQuestions", test.getAptitudeQuestions());
        response.put("technicalQuestions", test.getTechnicalQuestions());
        response.put("totalQuestions", test.getTotalQuestions());
        response.put("passingScore", test.getPassingScore());
        response.put("shuffleQuestions", test.getShuffleQuestions());
        response.put("isActive", test.getIsActive());
        response.put("createdAt", test.getCreatedAt());
        
        long technicalCount = questionRepository.findByDreamRoleTestId(test.getId()).size();
        response.put("technicalQuestionsAdded", technicalCount);
        response.put("technicalQuestionsRequired", 10);
        
        return response;
    }

    private Map<String, Object> createAssignmentResponse(DreamRoleTestAssignment assignment) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", assignment.getId());
        response.put("testId", assignment.getDreamRoleTest().getId());
        response.put("testName", assignment.getDreamRoleTest().getTestName());
        response.put("applicationId", assignment.getApplication().getId());
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
        response.put("passingScore", assignment.getDreamRoleTest().getPassingScore());
        return response;
    }
}