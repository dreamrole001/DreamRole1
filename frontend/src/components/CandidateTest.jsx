// src/components/CandidateTest.jsx
import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { 
  Clock, AlertCircle, CheckCircle, XCircle, Loader, 
  ChevronLeft, ChevronRight, Flag, BookOpen, Camera,
  AlertTriangle, Video, VideoOff, Maximize2, Minimize2,
  RefreshCw, Wifi, WifiOff, Shield
} from 'lucide-react';
import api from '../services/api';
import axios from 'axios';
import CameraPreview from './CameraPreview';

const CandidateTest = () => {
  const { assignmentId } = useParams();
  const navigate = useNavigate();
  
  // ========== ID HANDLING ==========
  const proctoringId = assignmentId;
  const numericId = assignmentId ? assignmentId.toString().replace(/[^0-9]/g, '') : '';
  
  const [test, setTest] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [currentQuestion, setCurrentQuestion] = useState(0);
  const [answers, setAnswers] = useState({});
  const [flaggedQuestions, setFlaggedQuestions] = useState(new Set());
  const [timeLeft, setTimeLeft] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [testStarted, setTestStarted] = useState(false);
  const [showInstructions, setShowInstructions] = useState(true);
  const [testType, setTestType] = useState('');
  
  // Proctoring state
  const PROCTORING_URL = 'http://localhost:5001';
  const [cameraReady, setCameraReady] = useState(false);
  const [cameraChecking, setCameraChecking] = useState(false);
  const [cameraPermissionDenied, setCameraPermissionDenied] = useState(false);
  const [proctoringActive, setProctoringActive] = useState(false);
  const [showCameraPreview, setShowCameraPreview] = useState(false);
  const [warningCount, setWarningCount] = useState(0);
  const [violations, setViolations] = useState([]);
  const [testTerminated, setTestTerminated] = useState(false);
  const [terminationReason, setTerminationReason] = useState('');
  const [proctoringError, setProctoringError] = useState('');
  const [networkStatus, setNetworkStatus] = useState('connected');
  const [isTestCompleted, setIsTestCompleted] = useState(false);
  const [mobileFrames, setMobileFrames] = useState(0);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [fullscreenWarning, setFullscreenWarning] = useState(false);
  
  // Refs
  const statusIntervalRef = useRef(null);
  const submitTimeoutRef = useRef(null);
  const testContainerRef = useRef(null);
  const visibilityTimeoutRef = useRef(null);

  // Check proctoring service health
  useEffect(() => {
    checkProctoringHealth();
    
    const handleOnline = () => setNetworkStatus('connected');
    const handleOffline = () => setNetworkStatus('disconnected');
    
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
      cleanupIntervals();
      
      // CRITICAL: Always stop proctoring and turn off camera on unmount
      if (proctoringActive) {
        console.log('Component unmounting, stopping proctoring...');
        stopProctoring();
      }
      
      // Exit fullscreen on unmount
      if (document.fullscreenElement) {
        document.exitFullscreen();
      }
    };
  }, []);

  // Also stop proctoring when test is completed
  useEffect(() => {
    if (isTestCompleted && proctoringActive) {
      console.log('Test completed, stopping proctoring...');
      stopProctoring();
    }
  }, [isTestCompleted]);

  useEffect(() => {
    if (numericId) {
      loadTest();
    }
  }, [numericId]);

  useEffect(() => {
    if (timeLeft > 0 && testStarted && !testTerminated && !isTestCompleted) {
      const timer = setInterval(() => {
        setTimeLeft(prev => {
          if (prev <= 1) {
            clearInterval(timer);
            handleAutoSubmit();
            return 0;
          }
          return prev - 1;
        });
      }, 1000);
      return () => clearInterval(timer);
    }
  }, [timeLeft, testStarted, testTerminated, isTestCompleted]);

  // Poll for test status
  useEffect(() => {
    if (proctoringActive && !isTestCompleted) {
      statusIntervalRef.current = setInterval(async () => {
        try {
          const response = await axios.get(`${PROCTORING_URL}/test-status/${proctoringId}`);
          if (response.data.active) {
            setMobileFrames(response.data.mobile_frames || 0);
            
            // Check for termination
            if (response.data.shouldTerminate) {
              const reason = response.data.termination_reason || 'Test terminated due to proctoring violations';
              handleTestTermination(reason, response.data.violations || []);
            }
          }
        } catch (error) {
          console.error('Status poll error:', error);
        }
      }, 2000);
      
      return () => {
        if (statusIntervalRef.current) {
          clearInterval(statusIntervalRef.current);
        }
      };
    }
  }, [proctoringActive, proctoringId, isTestCompleted]);

  // FULLSCREEN DETECTION AND ENFORCEMENT
  useEffect(() => {
    if (testStarted && !testTerminated && !isTestCompleted) {
      // Enter fullscreen when test starts
      enterFullscreen();
      
      // Listen for fullscreen change events
      const handleFullscreenChange = () => {
        const isCurrentlyFullscreen = !!document.fullscreenElement;
        setIsFullscreen(isCurrentlyFullscreen);
        
        if (!isCurrentlyFullscreen && testStarted && !testTerminated && !isTestCompleted) {
          // User exited fullscreen - show warning and try to re-enter
          setFullscreenWarning(true);
          
          // Try to re-enter fullscreen
          enterFullscreen();
          
          // If still not in fullscreen after 2 seconds, terminate
          if (visibilityTimeoutRef.current) {
            clearTimeout(visibilityTimeoutRef.current);
          }
          
          visibilityTimeoutRef.current = setTimeout(() => {
            if (!document.fullscreenElement && testStarted && !testTerminated && !isTestCompleted) {
              reportViolation('fullscreen_exit', 'Exited fullscreen mode');
            }
          }, 2000);
        }
      };
      
      document.addEventListener('fullscreenchange', handleFullscreenChange);
      document.addEventListener('webkitfullscreenchange', handleFullscreenChange);
      document.addEventListener('mozfullscreenchange', handleFullscreenChange);
      document.addEventListener('MSFullscreenChange', handleFullscreenChange);
      
      return () => {
        document.removeEventListener('fullscreenchange', handleFullscreenChange);
        document.removeEventListener('webkitfullscreenchange', handleFullscreenChange);
        document.removeEventListener('mozfullscreenchange', handleFullscreenChange);
        document.removeEventListener('MSFullscreenChange', handleFullscreenChange);
        if (visibilityTimeoutRef.current) {
          clearTimeout(visibilityTimeoutRef.current);
        }
      };
    }
  }, [testStarted, testTerminated, isTestCompleted]);

  // TAB SWITCH / VISIBILITY CHANGE DETECTION
  useEffect(() => {
    if (testStarted && !testTerminated && !isTestCompleted) {
      const handleVisibilityChange = () => {
        if (document.hidden) {
          // User switched to another tab
          console.warn('⚠️ Tab switch detected!');
          reportViolation('tab_switch', 'Switched to another tab during test');
        }
      };
      
      document.addEventListener('visibilitychange', handleVisibilityChange);
      
      return () => {
        document.removeEventListener('visibilitychange', handleVisibilityChange);
      };
    }
  }, [testStarted, testTerminated, isTestCompleted]);

  // KEY COMBINATION DETECTION (Ctrl+C, Ctrl+V, etc.)
  useEffect(() => {
    if (testStarted && !testTerminated && !isTestCompleted) {
      const handleKeyDown = (e) => {
        // Block common keyboard shortcuts
        const blockedKeys = [
          e.ctrlKey && e.key === 'c', // Ctrl+C
          e.ctrlKey && e.key === 'v', // Ctrl+V
          e.ctrlKey && e.key === 'x', // Ctrl+X
          e.ctrlKey && e.key === 'a', // Ctrl+A
          e.ctrlKey && e.key === 'f', // Ctrl+F
          e.ctrlKey && e.key === 'p', // Ctrl+P
          e.ctrlKey && e.key === 's', // Ctrl+S
          e.ctrlKey && e.key === 't', // Ctrl+T
          e.ctrlKey && e.key === 'w', // Ctrl+W
          e.altKey && e.key === 'Tab', // Alt+Tab
          e.altKey && e.key === 'F4', // Alt+F4
          e.key === 'Escape', // Escape
          e.key === 'F11' && !isFullscreen, // F11 (fullscreen toggle)
          e.metaKey && (e.key === 'c' || e.key === 'v' || e.key === 'x') // Mac keys
        ];
        
        if (blockedKeys.some(Boolean)) {
          e.preventDefault();
          e.stopPropagation();
          
          // Report the violation
          let violationMessage = 'Forbidden key combination used';
          if (e.ctrlKey && e.key === 'c') violationMessage = 'Ctrl+C (copy) detected';
          else if (e.ctrlKey && e.key === 'v') violationMessage = 'Ctrl+V (paste) detected';
          else if (e.ctrlKey && e.key === 'x') violationMessage = 'Ctrl+X (cut) detected';
          else if (e.key === 'Escape') violationMessage = 'Escape key pressed';
          else if (e.key === 'F11') violationMessage = 'F11 key pressed';
          
          reportViolation('key_violation', violationMessage);
        }
      };
      
      window.addEventListener('keydown', handleKeyDown, true);
      
      return () => {
        window.removeEventListener('keydown', handleKeyDown, true);
      };
    }
  }, [testStarted, testTerminated, isTestCompleted, isFullscreen]);

  // RIGHT-CLICK PREVENTION
  useEffect(() => {
    if (testStarted && !testTerminated && !isTestCompleted) {
      const handleContextMenu = (e) => {
        e.preventDefault();
        reportViolation('right_click', 'Right-click detected');
        return false;
      };
      
      document.addEventListener('contextmenu', handleContextMenu);
      
      return () => {
        document.removeEventListener('contextmenu', handleContextMenu);
      };
    }
  }, [testStarted, testTerminated, isTestCompleted]);

  const enterFullscreen = () => {
    const element = testContainerRef.current;
    if (element) {
      try {
        if (element.requestFullscreen) {
          element.requestFullscreen();
        } else if (element.webkitRequestFullscreen) {
          element.webkitRequestFullscreen();
        } else if (element.mozRequestFullScreen) {
          element.mozRequestFullScreen();
        } else if (element.msRequestFullscreen) {
          element.msRequestFullscreen();
        }
      } catch (err) {
        console.error('Fullscreen error:', err);
      }
    }
  };

  const reportViolation = async (type, message) => {
    try {
      const response = await axios.post(`${PROCTORING_URL}/report-violation/${proctoringId}`, {
        type,
        message
      });
      
      if (response.data.terminated) {
        handleTestTermination(response.data.reason, [{ type, message }]);
      }
    } catch (error) {
      console.error('Error reporting violation:', error);
      // If can't report to backend, still terminate locally
      handleTestTermination(message, [{ type, message }]);
    }
  };

  const cleanupIntervals = () => {
    if (statusIntervalRef.current) {
      clearInterval(statusIntervalRef.current);
      statusIntervalRef.current = null;
    }
    if (submitTimeoutRef.current) {
      clearTimeout(submitTimeoutRef.current);
      submitTimeoutRef.current = null;
    }
    if (visibilityTimeoutRef.current) {
      clearTimeout(visibilityTimeoutRef.current);
      visibilityTimeoutRef.current = null;
    }
  };

  const checkProctoringHealth = async () => {
    try {
      await axios.get(`${PROCTORING_URL}/health`, { timeout: 2000 });
      console.log('✅ Proctoring service is running');
    } catch (error) {
      setProctoringError('Cannot connect to proctoring service. Make sure it\'s running on port 5001.');
    }
  };

  const loadTest = async () => {
    try {
      setLoading(true);
      
      // Try DreamRole test first
      try {
        const response = await api.get(`/dream-role-tests/start/${numericId}`);
        console.log('DreamRole test loaded:', response.data);
        setTest(response.data.assignment);
        setQuestions(response.data.questions || []);
        setTestType('dreamrole');
        setTimeLeft(response.data.assignment.test?.durationMinutes * 60 || 3600);
        setLoading(false);
        return;
      } catch (dreamRoleError) {
        console.log('Not a DreamRole test, trying manual...');
      }
      
      // Try manual test
      const response = await api.get(`/aptitude-tests/start/${numericId}`);
      console.log('Manual test loaded:', response.data);
      setTest(response.data.assignment);
      setQuestions(response.data.questions || []);
      setTestType('manual');
      setTimeLeft(response.data.assignment.test?.durationMinutes * 60 || 3600);
      
    } catch (error) {
      console.error('Load test error:', error);
      setError(`Failed to load test. Test ID: ${assignmentId}`);
    } finally {
      setLoading(false);
    }
  };

  const checkCamera = async () => {
    setCameraChecking(true);
    setProctoringError('');
    
    try {
      // Check browser camera permission
      const stream = await navigator.mediaDevices.getUserMedia({ 
        video: { 
          width: 640, 
          height: 480,
          facingMode: 'user'
        } 
      });
      stream.getTracks().forEach(track => track.stop());
      console.log('✅ Camera permission granted');
      
      // Check proctoring service camera access
      const response = await axios.post(`${PROCTORING_URL}/check-camera`);
      
      if (response.data.available) {
        setCameraReady(true);
      } else {
        setProctoringError('Camera not available. Please check your camera connection.');
      }
    } catch (err) {
      console.error('Camera error:', err);
      if (err.name === 'NotAllowedError' || err.message.includes('permission')) {
        setCameraPermissionDenied(true);
        setProctoringError('Camera access denied. Please allow camera access in your browser.');
      } else if (err.name === 'NotFoundError' || err.message.includes('not found')) {
        setProctoringError('No camera found. Please connect a camera.');
      } else {
        setProctoringError('Could not access camera. Please try again.');
      }
    } finally {
      setCameraChecking(false);
    }
  };

  const startProctoring = async () => {
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}');
      if (!user.id) {
        setProctoringError('User not found. Please log in again.');
        return;
      }
      
      const response = await axios.post(`${PROCTORING_URL}/start-test`, {
        assignmentId: proctoringId,
        userId: user.id,
        testName: test?.testName || 'Aptitude Test'
      });
      
      if (response.data.message) {
        setProctoringActive(true);
        setShowCameraPreview(true);
        setTestStarted(true);
        setShowInstructions(false);
        setIsTestCompleted(false);
      }
    } catch (error) {
      console.error('Start proctoring error:', error);
      setProctoringError('Failed to start proctoring. Please try again.');
    }
  };

  const stopProctoring = async () => {
    try {
      console.log('Stopping proctoring for test:', proctoringId);
      
      // Make multiple attempts to stop proctoring
      for (let i = 0; i < 3; i++) {
        try {
          const response = await axios.post(`${PROCTORING_URL}/stop-test/${proctoringId}`);
          console.log(`✅ Attempt ${i + 1}: Proctoring stopped successfully`, response.data);
          break;
        } catch (error) {
          console.log(`❌ Attempt ${i + 1} failed:`, error.message);
          if (i === 2) {
            console.error('All attempts to stop proctoring failed');
          }
          // Wait a bit before retrying
          await new Promise(resolve => setTimeout(resolve, 500));
        }
      }
      
    } catch (error) {
      console.error('Stop proctoring error:', error);
    } finally {
      // Always update local state regardless of API success
      setProctoringActive(false);
      setShowCameraPreview(false);
      
      // Also try to stop any local media streams
      if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        navigator.mediaDevices.getUserMedia({ video: true })
          .then(stream => {
            stream.getTracks().forEach(track => {
              track.stop();
              console.log('Local camera track stopped');
            });
          })
          .catch(err => console.log('No local stream to stop'));
      }
    }
  };

  const handleTestTermination = (reason, violationList = []) => {
    console.log('Test terminated:', reason, violationList);
    setTestTerminated(true);
    setTerminationReason(reason);
    setViolations(prev => [...prev, ...violationList]);
    setIsTestCompleted(true);
    setProctoringActive(false);
    setShowCameraPreview(false);
    cleanupIntervals();
    
    // Stop proctoring immediately
    stopProctoring();
    
    // Exit fullscreen
    if (document.fullscreenElement) {
      document.exitFullscreen();
    }
    
    // Auto-submit the test with whatever answers were given
    submitTestOnTermination();
  };

  const submitTestOnTermination = async () => {
    try {
      const endpoint = testType === 'dreamrole' 
        ? '/dream-role-tests/submit' 
        : '/aptitude-tests/submit';
      
      const response = await api.post(endpoint, { 
        assignmentId: numericId, 
        answers,
        terminated: true,
        terminationReason: terminationReason
      });
      
      console.log('✅ Test auto-submitted after termination');
      
      // Navigate to results with test type
      setTimeout(() => {
        navigate(`/test-results/${assignmentId}?type=${testType}`);
      }, 3000);
      
    } catch (error) {
      console.error('Error auto-submitting after termination:', error);
      // Still navigate to results even if auto-submit fails
      setTimeout(() => {
        navigate(`/test-results/${assignmentId}?type=${testType}`);
      }, 3000);
    }
  };

  const handleTestCompletion = () => {
    console.log('Test completed normally');
    setIsTestCompleted(true);
    setProctoringActive(false);
    setShowCameraPreview(false);
    cleanupIntervals();
    
    // Stop proctoring before navigating
    stopProctoring().then(() => {
      // Exit fullscreen
      if (document.fullscreenElement) {
        document.exitFullscreen();
      }
      
      // Navigate to results with test type
      navigate(`/test-results/${assignmentId}?type=${testType}`);
    });
  };

  const handleStartTest = async () => {
    await checkCamera();
    if (cameraReady && !cameraPermissionDenied && !proctoringError) {
      await startProctoring();
    }
  };

  const handleAnswer = (questionId, answer) => {
    setAnswers(prev => ({ ...prev, [questionId]: answer }));
  };

  const toggleFlagQuestion = (questionId) => {
    setFlaggedQuestions(prev => {
      const newSet = new Set(prev);
      if (newSet.has(questionId)) {
        newSet.delete(questionId);
      } else {
        newSet.add(questionId);
      }
      return newSet;
    });
  };

  const handleSubmit = async () => {
    if (Object.keys(answers).length < questions.length) {
      if (!window.confirm(`You have answered ${Object.keys(answers).length} out of ${questions.length} questions. Submit anyway?`)) {
        return;
      }
    }
    
    setSubmitting(true);
    
    try {
      const endpoint = testType === 'dreamrole' 
        ? '/dream-role-tests/submit' 
        : '/aptitude-tests/submit';
      
      const response = await api.post(endpoint, { 
        assignmentId: numericId, 
        answers 
      });
      
      console.log('Test submission response:', response.data);
      
      // Mark test as completed first
      setIsTestCompleted(true);
      
      // Stop proctoring before navigating
      await stopProctoring();
      
      // Exit fullscreen
      if (document.fullscreenElement) {
        document.exitFullscreen();
      }
      
      // Navigate to results with test type
      navigate(`/test-results/${assignmentId}?type=${testType}`);
      
    } catch (error) {
      console.error('Submit error:', error);
      setError('Failed to submit test. Please try again.');
      setSubmitting(false);
    }
  };

  const handleAutoSubmit = async () => {
    console.log('Auto-submitting test due to time expiry...');
    try {
      const endpoint = testType === 'dreamrole' 
        ? '/dream-role-tests/submit' 
        : '/aptitude-tests/submit';
      
      const response = await api.post(endpoint, { 
        assignmentId: numericId, 
        answers 
      });
      
      console.log('Auto-submit response:', response.data);
      
      // Mark test as completed
      setIsTestCompleted(true);
      
      // Stop proctoring before navigating
      await stopProctoring();
      
      // Exit fullscreen
      if (document.fullscreenElement) {
        document.exitFullscreen();
      }
      
      // Navigate to results with test type
      navigate(`/test-results/${assignmentId}?type=${testType}`);
      
    } catch (error) {
      console.error('Auto-submit failed:', error);
      
      // Even if submission fails, try to stop proctoring
      await stopProctoring();
      
      // Still try to navigate
      navigate(`/test-results/${assignmentId}?type=${testType}`);
    }
  };

  const formatTime = (seconds) => {
    if (isNaN(seconds) || seconds < 0) return "00:00:00";
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const getAnsweredCount = () => Object.keys(answers).length;

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <Loader className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  if (proctoringError) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <div className="bg-red-100 border border-red-400 text-red-700 p-6 rounded-lg max-w-md">
          <VideoOff className="h-12 w-12 mx-auto mb-4" />
          <p className="text-center mb-4">{proctoringError}</p>
          <button
            onClick={() => navigate('/dashboard')}
            className="w-full bg-red-600 text-white py-2 rounded-lg hover:bg-red-700"
          >
            Go to Dashboard
          </button>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <div className="bg-red-100 border border-red-400 text-red-700 p-6 rounded-lg max-w-md">
          <AlertCircle className="h-12 w-12 mx-auto mb-4" />
          <p className="text-center mb-4">{error}</p>
          <button
            onClick={() => navigate('/dashboard')}
            className="w-full bg-red-600 text-white py-2 rounded-lg hover:bg-red-700"
          >
            Go to Dashboard
          </button>
        </div>
      </div>
    );
  }

  if (testTerminated) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="bg-red-100 border border-red-400 text-red-700 p-8 rounded-lg max-w-md text-center">
          <XCircle className="h-16 w-16 mx-auto mb-4 text-red-600" />
          <h2 className="text-2xl font-bold mb-3">Test Terminated</h2>
          <p className="text-lg mb-4">{terminationReason}</p>
          <div className="bg-red-50 p-4 rounded-lg mb-4">
            <p className="font-semibold mb-2">Violations:</p>
            <ul className="text-sm text-left list-disc pl-5">
              {violations.map((v, i) => (
                <li key={i}>{v.message}</li>
              ))}
            </ul>
          </div>
          <p className="text-sm text-gray-600 mb-4">Redirecting to results page...</p>
          <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-red-600 mx-auto"></div>
        </div>
      </div>
    );
  }

  if (showInstructions) {
    return (
      <div className="min-h-screen bg-gray-50 py-12">
        <div className="max-w-2xl mx-auto px-4">
          <div className="bg-white rounded-lg shadow-lg p-8">
            <div className="text-center mb-6">
              <BookOpen className="h-12 w-12 text-blue-600 mx-auto mb-4" />
              <h1 className="text-2xl font-bold mb-2">{test?.testName || 'Aptitude Test'}</h1>
              <p className="text-sm text-gray-500">Test ID: {assignmentId}</p>
              {testType && (
                <span className={`mt-2 inline-block px-3 py-1 rounded-full text-xs font-medium ${
                  testType === 'dreamrole' ? 'bg-purple-100 text-purple-800' : 'bg-blue-100 text-blue-800'
                }`}>
                  {testType === 'dreamrole' ? 'DreamRole Test' : 'Manual Test'}
                </span>
              )}
            </div>

            <div className="bg-blue-50 p-4 rounded-lg mb-6">
              <p><strong>Questions:</strong> {questions.length}</p>
              <p><strong>Duration:</strong> {test?.test?.durationMinutes || 60} minutes</p>
            </div>

            <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
              <p className="font-semibold mb-2 flex items-center">
                <Shield className="h-5 w-5 mr-2 text-yellow-600" />
                Proctoring Rules
              </p>
              <ul className="text-sm space-y-2 list-disc pl-5">
                <li className="text-red-600 font-semibold">Test will be in FULLSCREEN mode</li>
                <li className="text-red-600">Switching tabs/windows will TERMINATE the test immediately</li>
                <li className="text-red-600">Ctrl+C, Ctrl+V, and other shortcuts will TERMINATE the test</li>
                <li className="text-red-600">Right-click is disabled</li>
                <li className="text-red-600">Mobile phone detection (3 frames) will TERMINATE the test</li>
                <li className="text-red-600">Multiple people detection (3 frames) will TERMINATE the test</li>
                <li className="text-gray-700">Camera is required throughout the test</li>
              </ul>
            </div>

            {cameraChecking && (
              <div className="bg-blue-50 p-4 rounded-lg mb-4 text-center">
                <Loader className="h-5 w-5 animate-spin mx-auto mb-2" />
                <p>Checking camera...</p>
              </div>
            )}

            {!cameraReady && !cameraChecking && !cameraPermissionDenied && !proctoringError && (
              <button
                onClick={handleStartTest}
                className="w-full bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
              >
                Check Camera & Start Test
              </button>
            )}

            {cameraReady && (
              <button
                onClick={startProctoring}
                className="w-full bg-green-600 text-white py-3 rounded-lg font-semibold hover:bg-green-700 transition-colors"
              >
                Start Test (Fullscreen Mode)
              </button>
            )}

            {cameraPermissionDenied && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                <p className="text-red-700 text-center">
                  Camera access denied. Please allow camera access in your browser and refresh the page.
                </p>
              </div>
            )}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div 
      ref={testContainerRef}
      className="min-h-screen bg-gray-50"
    >
      {/* Fullscreen Warning */}
      {fullscreenWarning && (
        <div className="fixed top-0 left-0 right-0 bg-yellow-500 text-white py-2 px-4 text-center z-50 animate-pulse">
          ⚠️ Test must be in fullscreen mode. Please exit fullscreen to continue.
        </div>
      )}
      
      {/* Header */}
      <div className="bg-white shadow sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-xl font-bold">{test?.testName}</h1>
              <p className="text-sm text-gray-600">
                Question {currentQuestion + 1} of {questions.length}
              </p>
            </div>
            
            <div className="flex items-center space-x-4">
              <div className="flex items-center px-4 py-2 rounded-lg bg-blue-100 text-blue-700">
                <Clock className="h-5 w-5 mr-2" />
                <span className="font-mono font-bold">{formatTime(timeLeft)}</span>
              </div>
              
              <div className="text-sm">
                <span className="font-medium">Answered:</span>
                <span className="ml-1">{getAnsweredCount()}/{questions.length}</span>
              </div>
              
              {isFullscreen && (
                <span className="bg-green-100 text-green-700 px-2 py-1 rounded-full text-xs flex items-center">
                  <Maximize2 className="h-3 w-3 mr-1" />
                  Fullscreen
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Question Area */}
          <div className="lg:col-span-3">
            {questions.length > 0 && (
              <div className="bg-white rounded-lg shadow-lg p-6">
                <div className="mb-4 flex justify-between">
                  <span className="bg-blue-100 px-3 py-1 rounded-full text-sm">
                    Q{currentQuestion + 1}/{questions.length}
                  </span>
                  <button
                    onClick={() => toggleFlagQuestion(questions[currentQuestion].id)}
                    className="text-gray-600 hover:text-gray-800"
                  >
                    <Flag className="h-5 w-5" />
                  </button>
                </div>
                
                <h3 className="text-lg font-semibold mb-6">
                  {questions[currentQuestion]?.question}
                </h3>
                
                <div className="space-y-3">
                  {['A', 'B', 'C', 'D'].map(option => (
                    <button
                      key={option}
                      onClick={() => handleAnswer(questions[currentQuestion].id, option)}
                      className={`w-full p-4 text-left rounded-lg border-2 transition-colors ${
                        answers[questions[currentQuestion].id] === option
                          ? 'border-blue-600 bg-blue-50'
                          : 'border-gray-200 hover:border-blue-300'
                      }`}
                    >
                      <span className="font-bold mr-3">{option}.</span>
                      {questions[currentQuestion][`option${option}`]}
                    </button>
                  ))}
                </div>
                
                <div className="flex justify-between mt-6">
                  <button
                    onClick={() => setCurrentQuestion(prev => Math.max(0, prev - 1))}
                    disabled={currentQuestion === 0}
                    className="px-6 py-2 bg-gray-200 rounded-lg disabled:opacity-50 hover:bg-gray-300 transition-colors"
                  >
                    Previous
                  </button>
                  
                  {currentQuestion < questions.length - 1 ? (
                    <button
                      onClick={() => setCurrentQuestion(prev => prev + 1)}
                      className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                    >
                      Next
                    </button>
                  ) : (
                    <button
                      onClick={handleSubmit}
                      disabled={submitting}
                      className="px-8 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors disabled:opacity-50"
                    >
                      {submitting ? 'Submitting...' : 'Submit'}
                    </button>
                  )}
                </div>
              </div>
            )}
          </div>

          {/* Question Navigator */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-lg shadow-lg p-4 sticky top-24">
              <h4 className="font-semibold mb-3">Quick Navigation</h4>
              
              <div className="grid grid-cols-4 gap-2 mb-4">
                {questions.map((q, index) => (
                  <button
                    key={q.id}
                    onClick={() => setCurrentQuestion(index)}
                    className={`h-10 rounded-lg text-sm font-medium transition-colors ${
                      currentQuestion === index
                        ? 'bg-blue-600 text-white'
                        : answers[q.id]
                        ? 'bg-green-100 text-green-800 hover:bg-green-200'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                  >
                    {index + 1}
                  </button>
                ))}
              </div>

              <div className="border-t pt-3">
                <p className="text-sm">
                  <span className="font-medium">Progress:</span> {getAnsweredCount()}/{questions.length}
                </p>
                <p className="text-sm mt-1">
                  <span className="font-medium">Flagged:</span> {flaggedQuestions.size}
                </p>
                <p className="text-sm mt-1 text-purple-600">
                  <span className="font-medium">Mobile:</span> {mobileFrames}/3
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Camera Preview */}
      {showCameraPreview && !isTestCompleted && (
        <CameraPreview 
          assignmentId={proctoringId}
          warningCount={warningCount}
          violations={violations}
          isTestCompleted={isTestCompleted}
          onClose={() => setShowCameraPreview(false)}
          mobileFrames={mobileFrames}
        />
      )}
    </div>
  );
};

export default CandidateTest;