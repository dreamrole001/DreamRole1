// src/components/ApplicationsList.jsx
import React, { useState, useEffect } from 'react';
import { 
  X, Mail, Phone, Calendar, Download, Star, BookOpen, User, 
  Briefcase, RefreshCw, Eye, Clock, CheckCircle, XCircle, 
  Calendar as CalendarIcon, MapPin, MessageCircle, TrendingUp,
  FileText, Percent, Target, Award, Sparkles
} from 'lucide-react';
import api from '../services/api';

const ApplicationsList = ({ job, onClose }) => {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedApplication, setSelectedApplication] = useState(null);
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [showInterviewModal, setShowInterviewModal] = useState(false);
  const [showTestModal, setShowTestModal] = useState(false);
  const [showTestResultsModal, setShowTestResultsModal] = useState(false);
  const [tests, setTests] = useState([]);
  const [dreamRoleTests, setDreamRoleTests] = useState([]);
  const [testType, setTestType] = useState('manual');
  const [selectedTestId, setSelectedTestId] = useState('');
  const [deadlineHours, setDeadlineHours] = useState(48);
  const [testResults, setTestResults] = useState(null);
  const [actionLoading, setActionLoading] = useState(null);
  const [recruiterId, setRecruiterId] = useState(null);

  useEffect(() => {
    fetchApplications();
    fetchRecruiterId();
  }, [job]);

  useEffect(() => {
    if (recruiterId) {
      fetchTests();
      fetchDreamRoleTests();
    }
  }, [recruiterId]);

  const fetchRecruiterId = async () => {
    try {
      const currentUser = JSON.parse(localStorage.getItem('user'));
      let rId = job.recruiter?.id;

      if (!rId && currentUser?.recruiterId) {
        rId = currentUser.recruiterId;
      }

      if (!rId && currentUser?.id) {
        try {
          const profileResponse = await api.get(`/recruiters/user/${currentUser.id}`);
          if (profileResponse.data.recruiter) {
            rId = profileResponse.data.recruiter.id;
          }
        } catch (profileError) {
          console.error('Error fetching recruiter profile:', profileError);
        }
      }
      
      setRecruiterId(rId);
      console.log('Recruiter ID set to:', rId);
    } catch (error) {
      console.error('Error getting recruiter ID:', error);
    }
  };

  const fetchApplications = async () => {
    try {
      setLoading(true);
      setError('');
      
      console.log('=== FETCHING APPLICATIONS ===');
      console.log('Job:', job);
      console.log('Job ID:', job.id);

      const currentUser = JSON.parse(localStorage.getItem('user'));
      let rId = job.recruiter?.id;

      if (!rId && currentUser?.recruiterId) {
        rId = currentUser.recruiterId;
      }

      if (!rId && currentUser?.id) {
        try {
          const profileResponse = await api.get(`/recruiters/user/${currentUser.id}`);
          if (profileResponse.data.recruiter) {
            rId = profileResponse.data.recruiter.id;
          }
        } catch (profileError) {
          console.error('Error fetching recruiter profile:', profileError);
        }
      }

      let endpoint;
      if (rId) {
        endpoint = `/resume-matching/job/${job.id}/applications/sorted?recruiterId=${rId}`;
      } else {
        endpoint = `/recruiters/jobs/${job.id}/applications/debug`;
      }

      const response = await api.get(endpoint);
      
      if (response.data && response.data.applications) {
        setApplications(response.data.applications);
        console.log('✅ Applications loaded');
      } else {
        setApplications([]);
      }
    } catch (error) {
      console.error('Error fetching applications:', error);
      setError('Failed to load applications: ' + (error.response?.data?.error || error.message));
      setApplications([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchTests = async () => {
    try {
      if (!recruiterId) {
        console.log('No recruiter ID available to fetch tests');
        return;
      }
      
      console.log('Fetching tests for recruiter:', recruiterId);
      const response = await api.get(`/aptitude-tests/recruiter/${recruiterId}`);
      console.log('Tests fetched:', response.data);
      setTests(response.data || []);
    } catch (error) {
      console.error('Error fetching tests:', error);
    }
  };

  const fetchDreamRoleTests = async () => {
    try {
      const response = await api.get(`/dream-role-tests/recruiter/${recruiterId}`);
      setDreamRoleTests(response.data || []);
    } catch (error) {
      console.error('Error fetching DreamRole tests:', error);
    }
  };

  const fetchTestResults = async (applicationId) => {
    try {
      const response = await api.get(`/aptitude-tests/application/${applicationId}`);
      if (response.data.hasTest) {
        setTestResults(response.data.assignment);
        setShowTestResultsModal(true);
      } else {
        alert('No test assigned to this candidate');
      }
    } catch (error) {
      console.error('Error fetching test results:', error);
      alert('Failed to fetch test results');
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'APPLICATION_SUBMITTED':
        return 'bg-blue-100 text-blue-800';
      case 'VIEWED_BY_RECRUITER':
        return 'bg-purple-100 text-purple-800';
      case 'SHORTLISTED':
        return 'bg-green-100 text-green-800';
      case 'TEST_ASSIGNED':
        return 'bg-orange-100 text-orange-800';
      case 'TEST_IN_PROGRESS':
        return 'bg-yellow-100 text-yellow-800';
      case 'TEST_COMPLETED':
        return 'bg-indigo-100 text-indigo-800';
      case 'TEST_PASSED':
        return 'bg-emerald-100 text-emerald-800';
      case 'TEST_FAILED':
        return 'bg-red-100 text-red-800';
      case 'INTERVIEW_SCHEDULED':
        return 'bg-yellow-100 text-yellow-800';
      case 'REJECTED':
        return 'bg-red-100 text-red-800';
      case 'OFFER_SENT':
        return 'bg-emerald-100 text-emerald-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const getMatchColor = (percentage) => {
    if (percentage >= 80) return 'bg-green-100 text-green-800 border-green-200';
    if (percentage >= 60) return 'bg-yellow-100 text-yellow-800 border-yellow-200';
    if (percentage >= 40) return 'bg-orange-100 text-orange-800 border-orange-200';
    return 'bg-red-100 text-red-800 border-red-200';
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'APPLICATION_SUBMITTED':
        return <Clock className="h-4 w-4" />;
      case 'VIEWED_BY_RECRUITER':
        return <Eye className="h-4 w-4" />;
      case 'SHORTLISTED':
        return <CheckCircle className="h-4 w-4" />;
      case 'TEST_ASSIGNED':
        return <BookOpen className="h-4 w-4" />;
      case 'TEST_IN_PROGRESS':
        return <BookOpen className="h-4 w-4" />;
      case 'TEST_COMPLETED':
        return <Award className="h-4 w-4" />;
      case 'TEST_PASSED':
        return <Award className="h-4 w-4" />;
      case 'TEST_FAILED':
        return <XCircle className="h-4 w-4" />;
      case 'INTERVIEW_SCHEDULED':
        return <CalendarIcon className="h-4 w-4" />;
      case 'REJECTED':
        return <XCircle className="h-4 w-4" />;
      case 'OFFER_SENT':
        return <Star className="h-4 w-4" />;
      default:
        return <Clock className="h-4 w-4" />;
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown date';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const parseSkills = (skillsJson) => {
    try {
      if (!skillsJson) return [];
      if (typeof skillsJson === 'string') {
        return JSON.parse(skillsJson);
      }
      return skillsJson;
    } catch (error) {
      console.error('Error parsing skills:', error);
      return [];
    }
  };

  const handleDownloadResume = async (application) => {
    try {
      const response = await api.get(`/resumes/download/${application.id}`, {
        responseType: 'blob'
      });
      
      const blob = new Blob([response.data]);
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      
      const applicantName = application.applicantName || application.applicant?.fullName || 'candidate';
      const sanitizedName = applicantName.replace(/[^a-zA-Z0-9]/g, '_');
      
      let fileExtension = '.pdf';
      const contentType = response.headers['content-type'];
      if (contentType) {
        if (contentType.includes('pdf')) fileExtension = '.pdf';
        else if (contentType.includes('word')) fileExtension = '.doc';
        else if (contentType.includes('plain')) fileExtension = '.txt';
      }
      
      link.download = `resume_${sanitizedName}${fileExtension}`;
      document.body.appendChild(link);
      link.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(link);
      
    } catch (error) {
      alert('Unable to download resume. Please try again.');
    }
  };

  const handleStatusUpdate = async (applicationId, action, data = {}) => {
    setActionLoading(applicationId);
    
    try {
      const currentUser = JSON.parse(localStorage.getItem('user'));
      let rId = job.recruiter?.id;

      if (!rId && currentUser?.recruiterId) {
        rId = currentUser.recruiterId;
      }

      if (!rId && currentUser?.id) {
        const profileResponse = await api.get(`/recruiters/user/${currentUser.id}`);
        if (profileResponse.data.recruiter) {
          rId = profileResponse.data.recruiter.id;
        }
      }

      if (!rId) {
        alert('Recruiter information not found');
        return;
      }

      let response;
      
      switch (action) {
        case 'view':
          response = await api.put(`/applications/${applicationId}/view?recruiterId=${rId}`);
          break;
        case 'shortlist':
          response = await api.put(`/applications/${applicationId}/shortlist?recruiterId=${rId}`, {
            notes: data.notes || 'Application shortlisted'
          });
          break;
        case 'assign-test':
          if (data.testType === 'dreamrole') {
            response = await api.post('/dream-role-tests/assign', {
              testId: data.testId,
              applicationId,
              deadlineHours: data.deadlineHours || 48
            });
          } else {
            response = await api.post('/aptitude-tests/assign', {
              testId: data.testId,
              applicationId,
              deadlineHours: data.deadlineHours || 48
            });
          }
          break;
        case 'schedule-interview':
          response = await api.put(`/applications/${applicationId}/schedule-interview?recruiterId=${rId}`, {
            interviewDate: data.interviewDate,
            interviewLocation: data.interviewLocation,
            notes: data.notes || 'Interview scheduled'
          });
          break;
        case 'reject':
          response = await api.put(`/applications/${applicationId}/reject?recruiterId=${rId}`, {
            rejectionReason: data.rejectionReason || 'Application rejected'
          });
          break;
        case 'send-offer':
          response = await api.put(`/applications/${applicationId}/send-offer?recruiterId=${rId}`, {
            offerDetails: data.offerDetails || 'Offer sent'
          });
          break;
        default:
          break;
      }

      if (response && response.data) {
        alert('Status updated successfully!');
        fetchApplications();
        setShowStatusModal(false);
        setShowInterviewModal(false);
        setShowTestModal(false);
      }
    } catch (error) {
      console.error('Error updating status:', error);
      alert('Failed to update status: ' + (error.response?.data?.error || error.message));
    } finally {
      setActionLoading(null);
    }
  };

  const handleAssignTest = (application) => {
    setSelectedApplication(application);
    setShowTestModal(true);
  };

  // Test Assignment Modal Component
  const TestAssignmentModal = () => {
    const [localTestId, setLocalTestId] = useState(selectedTestId);
    const [localDeadline, setLocalDeadline] = useState(deadlineHours);
    const [localTestType, setLocalTestType] = useState(testType);

    if (!selectedApplication) return null;

    return (
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
        <div className="relative top-20 mx-auto p-5 border w-full max-w-md shadow-lg rounded-md bg-white">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold">Assign Test</h3>
            <button 
              onClick={() => setShowTestModal(false)} 
              className="text-gray-400 hover:text-gray-600"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="space-y-4">
            {/* Test Type Selection */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Test Type
              </label>
              <div className="flex space-x-4">
                <label className="flex items-center">
                  <input
                    type="radio"
                    name="testType"
                    value="manual"
                    checked={localTestType === 'manual'}
                    onChange={() => {
                      setLocalTestType('manual');
                      setTestType('manual');
                      setLocalTestId('');
                      setSelectedTestId('');
                    }}
                    className="mr-2"
                  />
                  <span className="text-sm">Manual Test</span>
                </label>
                <label className="flex items-center">
                  <input
                    type="radio"
                    name="testType"
                    value="dreamrole"
                    checked={localTestType === 'dreamrole'}
                    onChange={() => {
                      setLocalTestType('dreamrole');
                      setTestType('dreamrole');
                      setLocalTestId('');
                      setSelectedTestId('');
                    }}
                    className="mr-2"
                  />
                  <span className="text-sm flex items-center">
                    <Sparkles className="h-4 w-4 text-purple-600 mr-1" />
                    DreamRole Test
                  </span>
                </label>
              </div>
            </div>

            {/* Test Selection Dropdown */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Select Test <span className="text-red-500">*</span>
              </label>
              
              {localTestType === 'manual' ? (
                <select
                  value={localTestId}
                  onChange={(e) => {
                    setLocalTestId(e.target.value);
                    setSelectedTestId(e.target.value);
                  }}
                  className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="">Choose a manual test</option>
                  {tests.map(test => (
                    <option key={test.id} value={test.id}>
                      {test.testName} ({test.totalQuestions || 0} questions • {test.durationMinutes} mins)
                    </option>
                  ))}
                </select>
              ) : (
                <select
                  value={localTestId}
                  onChange={(e) => {
                    setLocalTestId(e.target.value);
                    setSelectedTestId(e.target.value);
                  }}
                  className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-purple-500"
                >
                  <option value="">Choose a DreamRole test</option>
                  {dreamRoleTests.map(test => (
                    <option key={test.id} value={test.id}>
                      {test.testName} ({test.targetBranch}) - {test.technicalQuestionsAdded || 0}/10 tech
                    </option>
                  ))}
                </select>
              )}

              {localTestType === 'manual' && tests.length === 0 && (
                <div className="mt-2 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <p className="text-sm text-yellow-800 font-medium">No manual tests available</p>
                  <p className="text-xs text-yellow-600 mt-1">
                    Please create a manual test first.
                  </p>
                </div>
              )}

              {localTestType === 'dreamrole' && dreamRoleTests.length === 0 && (
                <div className="mt-2 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <p className="text-sm text-yellow-800 font-medium">No DreamRole tests available</p>
                  <p className="text-xs text-yellow-600 mt-1">
                    Please create a DreamRole test first.
                  </p>
                </div>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Deadline (hours) <span className="text-red-500">*</span>
              </label>
              <input
                type="number"
                min="1"
                max="168"
                value={localDeadline}
                onChange={(e) => {
                  setLocalDeadline(parseInt(e.target.value));
                  setDeadlineHours(parseInt(e.target.value));
                }}
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              <p className="text-xs text-gray-500 mt-1">
                Candidate must complete test within {localDeadline} hours
              </p>
            </div>

            {localTestId && (
              <div className={`p-3 rounded-lg ${localTestType === 'dreamrole' ? 'bg-purple-50' : 'bg-blue-50'}`}>
                <h4 className={`font-medium mb-2 ${localTestType === 'dreamrole' ? 'text-purple-800' : 'text-blue-800'}`}>
                  Test Details:
                </h4>
                {localTestType === 'manual' ? (
                  <>
                    <p className="text-sm text-blue-700">
                      {tests.find(t => t.id === parseInt(localTestId))?.testName}
                    </p>
                    <p className="text-xs text-blue-600 mt-1">
                      {tests.find(t => t.id === parseInt(localTestId))?.totalQuestions} questions • 
                      {tests.find(t => t.id === parseInt(localTestId))?.durationMinutes} minutes
                    </p>
                  </>
                ) : (
                  <>
                    <p className="text-sm text-purple-700">
                      {dreamRoleTests.find(t => t.id === parseInt(localTestId))?.testName}
                    </p>
                    <p className="text-xs text-purple-600 mt-1">
                      Branch: {dreamRoleTests.find(t => t.id === parseInt(localTestId))?.targetBranch} • 
                      40 Aptitude + {dreamRoleTests.find(t => t.id === parseInt(localTestId))?.technicalQuestionsAdded || 0} Technical
                    </p>
                  </>
                )}
              </div>
            )}
          </div>

          <div className="flex justify-end space-x-3 mt-6">
            <button
              onClick={() => setShowTestModal(false)}
              className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              onClick={() => handleStatusUpdate(selectedApplication.id, 'assign-test', {
                testId: localTestId,
                deadlineHours: localDeadline,
                testType: localTestType
              })}
              disabled={!localTestId || actionLoading === selectedApplication.id}
              className={`px-4 py-2 text-white rounded-md text-sm font-medium disabled:opacity-50 ${
                localTestType === 'dreamrole' 
                  ? 'bg-purple-600 hover:bg-purple-700' 
                  : 'bg-blue-600 hover:bg-blue-700'
              }`}
            >
              {actionLoading === selectedApplication.id ? 'Assigning...' : 'Assign Test'}
            </button>
          </div>
        </div>
      </div>
    );
  };

  // Test Results Modal Component
  const TestResultsModal = () => {
    if (!testResults) return null;

    return (
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
        <div className="relative top-20 mx-auto p-5 border w-full max-w-lg shadow-lg rounded-md bg-white">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold">Test Results</h3>
            <button 
              onClick={() => setShowTestResultsModal(false)} 
              className="text-gray-400 hover:text-gray-600"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="space-y-4">
            <div className="bg-gray-50 p-4 rounded-lg">
              <h4 className="font-medium text-gray-800 mb-2">{testResults.testName}</h4>
              
              <div className="grid grid-cols-2 gap-3 mb-3">
                <div className="text-center p-2 bg-blue-50 rounded">
                  <div className="text-2xl font-bold text-blue-600">{testResults.score || 0}</div>
                  <div className="text-xs text-gray-600">Score</div>
                </div>
                <div className="text-center p-2 bg-green-50 rounded">
                  <div className="text-2xl font-bold text-green-600">{testResults.totalQuestions || 0}</div>
                  <div className="text-xs text-gray-600">Total</div>
                </div>
              </div>

              <div className="mb-2">
                <div className="flex justify-between text-sm mb-1">
                  <span>Percentage</span>
                  <span className="font-bold">{testResults.percentage || 0}%</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div 
                    className={`h-2 rounded-full ${
                      testResults.passed ? 'bg-green-500' : 'bg-red-500'
                    }`}
                    style={{ width: `${testResults.percentage || 0}%` }}
                  ></div>
                </div>
              </div>

              <div className="mt-3 flex items-center justify-between">
                <span className="text-sm text-gray-600">Status</span>
                <span className={`px-2 py-1 rounded-full text-xs font-medium ${
                  testResults.passed 
                    ? 'bg-green-100 text-green-800'
                    : testResults.status === 'PENDING'
                    ? 'bg-yellow-100 text-yellow-800'
                    : 'bg-red-100 text-red-800'
                }`}>
                  {testResults.passed ? 'PASSED' : testResults.status}
                </span>
              </div>

              {testResults.completedAt && (
                <p className="text-xs text-gray-500 mt-2">
                  Completed: {formatDate(testResults.completedAt)}
                </p>
              )}
            </div>

            {testResults.passed && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-4">
                <p className="text-green-800 text-sm font-medium">
                  ✅ Candidate has passed the test. You can now schedule an interview.
                </p>
              </div>
            )}

            {testResults.status === 'PENDING' && (
              <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
                <p className="text-yellow-800 text-sm">
                  ⏳ Test is pending. Candidate needs to complete the test.
                </p>
              </div>
            )}
          </div>

          <div className="flex justify-end mt-6">
            <button
              onClick={() => setShowTestResultsModal(false)}
              className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm font-medium hover:bg-blue-700"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    );
  };

  // Interview Modal Component
  const InterviewModal = () => {
    const [interviewDate, setInterviewDate] = useState('');
    const [interviewTime, setInterviewTime] = useState('');
    const [interviewLocation, setInterviewLocation] = useState('');
    const [notes, setNotes] = useState('');
    const [errors, setErrors] = useState({});

    if (!selectedApplication) return null;

    const validateForm = () => {
      const newErrors = {};
      
      if (!interviewDate) {
        newErrors.interviewDate = 'Interview date is required';
      }
      
      if (!interviewTime) {
        newErrors.interviewTime = 'Interview time is required';
      }
      
      if (!interviewLocation.trim()) {
        newErrors.interviewLocation = 'Interview location is required';
      }
      
      setErrors(newErrors);
      return Object.keys(newErrors).length === 0;
    };

    const handleSubmit = () => {
      if (!validateForm()) return;

      const dateTimeString = `${interviewDate}T${interviewTime}:00`;
      
      const data = {
        interviewDate: dateTimeString,
        interviewLocation: interviewLocation.trim(),
        notes: notes || `Interview scheduled for ${new Date(dateTimeString).toLocaleString()} at ${interviewLocation}`
      };

      handleStatusUpdate(selectedApplication.id, 'schedule-interview', data);
    };

    const getMinDate = () => {
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 1);
      return tomorrow.toISOString().split('T')[0];
    };

    return (
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
        <div className="relative top-20 mx-auto p-5 border w-full max-w-md shadow-lg rounded-md bg-white">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold">Schedule Interview</h3>
            <button 
              onClick={() => setShowInterviewModal(false)} 
              className="text-gray-400 hover:text-gray-600"
            >
              <X className="h-5 w-5" />
            </button>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Interview Date <span className="text-red-500">*</span>
              </label>
              <input
                type="date"
                min={getMinDate()}
                value={interviewDate}
                onChange={(e) => setInterviewDate(e.target.value)}
                className={`w-full border rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.interviewDate ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.interviewDate && (
                <p className="text-red-500 text-xs mt-1">{errors.interviewDate}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Interview Time <span className="text-red-500">*</span>
              </label>
              <input
                type="time"
                value={interviewTime}
                onChange={(e) => setInterviewTime(e.target.value)}
                className={`w-full border rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  errors.interviewTime ? 'border-red-500' : 'border-gray-300'
                }`}
              />
              {errors.interviewTime && (
                <p className="text-red-500 text-xs mt-1">{errors.interviewTime}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Interview Location <span className="text-red-500">*</span>
              </label>
              <div className="flex items-center">
                <MapPin className="h-4 w-4 text-gray-400 mr-2" />
                <input
                  type="text"
                  value={interviewLocation}
                  onChange={(e) => setInterviewLocation(e.target.value)}
                  placeholder="Enter interview location (e.g., Main Office, Zoom, etc.)"
                  className={`w-full border rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                    errors.interviewLocation ? 'border-red-500' : 'border-gray-300'
                  }`}
                />
              </div>
              {errors.interviewLocation && (
                <p className="text-red-500 text-xs mt-1">{errors.interviewLocation}</p>
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Additional Notes</label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows="3"
                placeholder="Additional instructions for the candidate..."
              />
            </div>

            {(interviewDate && interviewTime && interviewLocation) && (
              <div className="p-3 bg-blue-50 border border-blue-200 rounded-md">
                <h4 className="font-medium text-blue-800 mb-2">Interview Preview:</h4>
                <p className="text-sm text-blue-700">
                  <strong>Date:</strong> {new Date(`${interviewDate}T${interviewTime}`).toLocaleDateString('en-US', { 
                    weekday: 'long', 
                    year: 'numeric', 
                    month: 'long', 
                    day: 'numeric' 
                  })}
                </p>
                <p className="text-sm text-blue-700">
                  <strong>Time:</strong> {new Date(`${interviewDate}T${interviewTime}`).toLocaleTimeString('en-US', {
                    hour: '2-digit',
                    minute: '2-digit'
                  })}
                </p>
                <p className="text-sm text-blue-700">
                  <strong>Location:</strong> {interviewLocation}
                </p>
              </div>
            )}
          </div>

          <div className="flex justify-end space-x-3 mt-6">
            <button
              onClick={() => setShowInterviewModal(false)}
              className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              onClick={handleSubmit}
              disabled={actionLoading === selectedApplication.id}
              className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
            >
              {actionLoading === selectedApplication.id ? 'Scheduling...' : 'Schedule Interview'}
            </button>
          </div>
        </div>
      </div>
    );
  };

  // Status Update Modal Component
  const StatusUpdateModal = () => {
    const [notes, setNotes] = useState('');
    const [rejectionReason, setRejectionReason] = useState('');
    const [offerDetails, setOfferDetails] = useState('');

    if (!selectedApplication) return null;

    const handleSubmit = () => {
      const action = showStatusModal === 'reject' ? 'reject' :
                    showStatusModal === 'offer' ? 'send-offer' : 'shortlist';
      
      const data = showStatusModal === 'reject' ? {
        rejectionReason: rejectionReason || 'Not selected'
      } : showStatusModal === 'offer' ? {
        offerDetails: offerDetails || 'Congratulations! We are pleased to offer you the position.'
      } : {
        notes: notes || 'Application shortlisted'
      };

      handleStatusUpdate(selectedApplication.id, action, data);
    };

    return (
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
        <div className="relative top-20 mx-auto p-5 border w-full max-w-md shadow-lg rounded-md bg-white">
          <div className="flex justify-between items-center mb-4">
            <h3 className="text-lg font-semibold">
              {showStatusModal === 'reject' && 'Reject Application'}
              {showStatusModal === 'offer' && 'Send Offer'}
              {showStatusModal === 'shortlist' && 'Shortlist Application'}
            </h3>
            <button onClick={() => setShowStatusModal(false)} className="text-gray-400 hover:text-gray-600">
              <X className="h-5 w-5" />
            </button>
          </div>

          {showStatusModal === 'reject' && (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Rejection Reason</label>
                <textarea
                  value={rejectionReason}
                  onChange={(e) => setRejectionReason(e.target.value)}
                  className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows="3"
                  placeholder="Reason for rejection..."
                />
              </div>
            </div>
          )}

          {showStatusModal === 'offer' && (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Offer Details</label>
                <textarea
                  value={offerDetails}
                  onChange={(e) => setOfferDetails(e.target.value)}
                  className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows="4"
                  placeholder="Offer details, salary, benefits, start date..."
                />
              </div>
            </div>
          )}

          {showStatusModal === 'shortlist' && (
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Notes</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows="3"
                  placeholder="Notes about why this candidate was shortlisted..."
                />
              </div>
            </div>
          )}

          <div className="flex justify-end space-x-3 mt-6">
            <button
              onClick={() => setShowStatusModal(false)}
              className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              onClick={handleSubmit}
              disabled={actionLoading === selectedApplication.id}
              className="px-4 py-2 bg-blue-600 text-white rounded-md text-sm font-medium hover:bg-blue-700 disabled:opacity-50"
            >
              {actionLoading === selectedApplication.id ? 'Updating...' : 'Update'}
            </button>
          </div>
        </div>
      </div>
    );
  };

  if (loading) {
    return (
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
        <div className="relative top-20 mx-auto p-5 border w-full max-w-4xl shadow-lg rounded-md bg-white">
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Loading applications...</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-6xl shadow-lg rounded-md bg-white">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h2 className="text-2xl font-bold text-gray-900">Applications for {job.title}</h2>
            <p className="text-gray-600">
              {applications.length} application{applications.length !== 1 ? 's' : ''} found
            </p>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 p-2">
            <X className="h-6 w-6" />
          </button>
        </div>

        <div className="flex justify-between items-center mb-6">
          <button
            onClick={fetchApplications}
            className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-md text-sm font-medium hover:bg-blue-700 transition-colors"
          >
            <RefreshCw className="h-4 w-4 mr-2" />
            Refresh
          </button>
          
          <div className="flex items-center space-x-2">
            <Target className="h-5 w-5 text-green-600" />
            <span className="text-sm text-gray-600">Sorted by AI Match Score</span>
          </div>
        </div>

        {error && (
          <div className="mb-6 p-4 bg-red-100 border border-red-400 text-red-700 rounded-lg">
            <div className="flex items-center">
              <Mail className="h-5 w-5 mr-2" />
              <span>{error}</span>
            </div>
          </div>
        )}

        {applications.length === 0 ? (
          <div className="text-center py-12">
            <Mail className="mx-auto h-16 w-16 text-gray-400 mb-4" />
            <h3 className="text-lg font-medium text-gray-900 mb-2">No applications yet</h3>
            <p className="text-gray-500">Applications will appear here when candidates apply to this job.</p>
          </div>
        ) : (
          <div className="space-y-6">
            {applications.map((application, index) => (
              <div key={application.id} className="border border-gray-200 rounded-lg p-6 hover:shadow-md transition-shadow">
                <div className="flex justify-between items-start mb-4">
                  <div className="flex items-start space-x-4 flex-1">
                    {index < 3 && (
                      <div className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center text-white font-bold text-sm ${
                        index === 0 ? 'bg-yellow-500' : 
                        index === 1 ? 'bg-gray-400' : 
                        'bg-orange-500'
                      }`}>
                        {index + 1}
                      </div>
                    )}
                    
                    <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center flex-shrink-0">
                      <User className="h-6 w-6 text-blue-600" />
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center space-x-3 mb-2">
                        <h3 className="text-xl font-semibold text-gray-900">
                          {application.applicantName || application.applicant?.fullName || 'Unknown Applicant'}
                        </h3>
                        
                        {application.matchPercentage > 0 && (
                          <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border ${getMatchColor(application.matchPercentage)}`}>
                            <TrendingUp className="h-4 w-4 mr-1" />
                            {application.matchPercentage}% Match
                          </span>
                        )}
                      </div>
                      
                      <p className="text-gray-600">
                        {application.applicantEmail || application.applicant?.email || 'No email provided'}
                      </p>
                      
                      <div className="mt-2">
                        <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(application.status)}`}>
                          {getStatusIcon(application.status)}
                          <span className="ml-1">
                            {application.status?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) || 'Unknown'}
                          </span>
                        </span>
                      </div>
                      
                      {application.applicantSkills && (
                        <div className="mt-2 flex flex-wrap gap-1">
                          {parseSkills(application.applicantSkills).slice(0, 4).map((skill, skillIndex) => (
                            <span
                              key={skillIndex}
                              className="bg-blue-100 text-blue-800 px-2 py-1 rounded text-xs font-medium"
                            >
                              {skill}
                            </span>
                          ))}
                          {parseSkills(application.applicantSkills).length > 4 && (
                            <span className="text-gray-500 text-xs">
                              +{parseSkills(application.applicantSkills).length - 4} more
                            </span>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                  
                  <div className="flex flex-col items-end space-y-2">
                    <div className="flex space-x-2">
                      {application.hasResume && (
                        <>
                          <button 
                            onClick={() => handleDownloadResume(application)}
                            className="inline-flex items-center px-3 py-1 border border-blue-300 rounded-md text-sm font-medium text-blue-700 bg-white hover:bg-blue-50"
                          >
                            <Download className="h-4 w-4 mr-1" />
                            Resume
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                </div>

                {/* Application Timeline */}
                <div className="mb-4">
                  <h4 className="font-semibold text-gray-800 mb-2 flex items-center">
                    <Clock className="h-4 w-4 mr-2" />
                    Application Timeline
                  </h4>
                  <div className="flex items-center space-x-4 text-sm text-gray-600">
                    <div className="flex items-center">
                      <Calendar className="h-4 w-4 mr-1" />
                      Applied: {formatDate(application.applicationDate)}
                    </div>
                    {application.viewedAt && (
                      <div className="flex items-center">
                        <Eye className="h-4 w-4 mr-1" />
                        Viewed: {formatDate(application.viewedAt)}
                      </div>
                    )}
                  </div>
                </div>

                {/* Detailed Match Analysis */}
                {application.matchPercentage > 0 && (
                  <div className="mb-4 p-4 bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-lg">
                    <h4 className="font-semibold text-blue-800 mb-3 flex items-center">
                      <Target className="h-4 w-4 mr-2" />
                      AI Resume Match Analysis
                    </h4>
                    
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-gray-700">Overall Match Score</span>
                        <div className="flex items-center space-x-2">
                          <div className="w-24 bg-gray-200 rounded-full h-2">
                            <div 
                              className={`h-2 rounded-full ${
                                application.matchPercentage >= 80 ? 'bg-green-500' :
                                application.matchPercentage >= 60 ? 'bg-yellow-500' :
                                application.matchPercentage >= 40 ? 'bg-orange-500' : 'bg-red-500'
                              }`}
                              style={{ width: `${application.matchPercentage}%` }}
                            ></div>
                          </div>
                          <span className={`text-sm font-bold ${
                            application.matchPercentage >= 80 ? 'text-green-600' :
                            application.matchPercentage >= 60 ? 'text-yellow-600' :
                            application.matchPercentage >= 40 ? 'text-orange-600' : 'text-red-600'
                          }`}>
                            {application.matchPercentage}%
                          </span>
                        </div>
                      </div>

                      <div className="flex items-center justify-between">
                        <span className="text-sm font-medium text-gray-700">Skills Alignment</span>
                        <span className="text-sm text-gray-600">
                          {application.matchedSkills ? parseSkills(application.matchedSkills).length : 0} skills matched
                        </span>
                      </div>
                    </div>

                    <div className="mt-3 grid grid-cols-1 md:grid-cols-2 gap-4">
                      <div>
                        <h5 className="text-sm font-medium text-green-700 mb-2">✅ Matched Skills</h5>
                        <div className="flex flex-wrap gap-1">
                          {application.matchedSkills && parseSkills(application.matchedSkills).slice(0, 6).map((skill, index) => (
                            <span key={index} className="bg-green-100 text-green-800 px-2 py-1 rounded text-xs">
                              {skill}
                            </span>
                          ))}
                          {application.matchedSkills && parseSkills(application.matchedSkills).length > 6 && (
                            <span className="text-green-600 text-xs">+{parseSkills(application.matchedSkills).length - 6} more</span>
                          )}
                        </div>
                      </div>

                      <div>
                        <h5 className="text-sm font-medium text-red-700 mb-2">❌ Missing Skills</h5>
                        <div className="flex flex-wrap gap-1">
                          {application.missingSkills && parseSkills(application.missingSkills).slice(0, 6).map((skill, index) => (
                            <span key={index} className="bg-red-100 text-red-800 px-2 py-1 rounded text-xs">
                              {skill}
                            </span>
                          ))}
                          {application.missingSkills && parseSkills(application.missingSkills).length > 6 && (
                            <span className="text-red-600 text-xs">+{parseSkills(application.missingSkills).length - 6} more</span>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* Qualifications */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-4">
                  <div className="space-y-3">
                    <h4 className="font-semibold text-gray-800 flex items-center">
                      <Briefcase className="h-4 w-4 mr-2" />
                      Contact Information
                    </h4>
                    <div className="space-y-2">
                      <div className="flex items-center text-sm text-gray-600">
                        <Mail className="h-4 w-4 mr-2" />
                        {application.applicantEmail || application.applicant?.email || 'No email'}
                      </div>
                      {(application.applicantPhone || application.applicant?.phone) && (
                        <div className="flex items-center text-sm text-gray-600">
                          <Phone className="h-4 w-4 mr-2" />
                          {application.applicantPhone || application.applicant?.phone}
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="space-y-3">
                    <h4 className="font-semibold text-gray-800 flex items-center">
                      <Star className="h-4 w-4 mr-2" />
                      Qualifications
                    </h4>
                    <div className="space-y-2">
                      {application.applicantExperience && (
                        <div className="flex items-center text-sm text-gray-600">
                          <div className="w-2 h-2 bg-yellow-500 rounded-full mr-2"></div>
                          {application.applicantExperience} years of experience
                        </div>
                      )}
                      {application.applicantEducation && (
                        <div className="flex items-center text-sm text-gray-600">
                          <BookOpen className="h-4 w-4 mr-2" />
                          {application.applicantEducation}
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                {/* All Skills */}
                {application.applicantSkills && (
                  <div className="mb-4">
                    <h4 className="font-semibold text-gray-800 mb-2">All Skills & Technologies</h4>
                    <div className="flex flex-wrap gap-2">
                      {parseSkills(application.applicantSkills).map((skill, index) => (
                        <span
                          key={index}
                          className="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-medium"
                        >
                          {skill}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                {/* Recruiter Actions */}
                <div className="mt-6 pt-4 border-t border-gray-200">
                  <h4 className="font-semibold text-gray-800 mb-3">Application Actions</h4>
                  <div className="flex flex-wrap gap-2">
                    {!application.viewedByRecruiter && (
                      <button
                        onClick={() => {
                          setSelectedApplication(application);
                          handleStatusUpdate(application.id, 'view');
                        }}
                        disabled={actionLoading === application.id}
                        className="inline-flex items-center px-3 py-2 border border-purple-300 text-purple-700 rounded-md text-sm font-medium hover:bg-purple-50 disabled:opacity-50"
                      >
                        <Eye className="h-4 w-4 mr-1" />
                        Mark as Viewed
                      </button>
                    )}
                    
                    <button
                      onClick={() => {
                        setSelectedApplication(application);
                        setShowStatusModal('shortlist');
                      }}
                      className="inline-flex items-center px-3 py-2 border border-green-300 text-green-700 rounded-md text-sm font-medium hover:bg-green-50"
                    >
                      <CheckCircle className="h-4 w-4 mr-1" />
                      Shortlist
                    </button>
                    
                    {application.status === 'SHORTLISTED' && (
                      <button
                        onClick={() => handleAssignTest(application)}
                        className="inline-flex items-center px-3 py-2 border border-blue-300 text-blue-700 rounded-md text-sm font-medium hover:bg-blue-50"
                      >
                        <BookOpen className="h-4 w-4 mr-1" />
                        Assign Test
                      </button>
                    )}

                    {(application.status === 'TEST_PASSED') && (
                      <button
                        onClick={() => {
                          setSelectedApplication(application);
                          setShowInterviewModal(true);
                        }}
                        className="inline-flex items-center px-3 py-2 border border-yellow-300 text-yellow-700 rounded-md text-sm font-medium hover:bg-yellow-50"
                      >
                        <CalendarIcon className="h-4 w-4 mr-1" />
                        Schedule Interview
                      </button>
                    )}

                    {(application.status === 'TEST_COMPLETED' || application.status === 'TEST_PASSED' || application.status === 'TEST_FAILED') && (
                      <button
                        onClick={() => fetchTestResults(application.id)}
                        className="inline-flex items-center px-3 py-2 border border-indigo-300 text-indigo-700 rounded-md text-sm font-medium hover:bg-indigo-50"
                      >
                        <Award className="h-4 w-4 mr-1" />
                        View Test Results
                      </button>
                    )}

                    <button
                      onClick={() => {
                        setSelectedApplication(application);
                        setShowStatusModal('reject');
                      }}
                      className="inline-flex items-center px-3 py-2 border border-red-300 text-red-700 rounded-md text-sm font-medium hover:bg-red-50"
                    >
                      <XCircle className="h-4 w-4 mr-1" />
                      Reject
                    </button>
                    
                    {application.hasResume && (
                      <button
                        onClick={() => handleDownloadResume(application)}
                        className="inline-flex items-center px-3 py-2 border border-blue-300 text-blue-700 rounded-md text-sm font-medium hover:bg-blue-50"
                      >
                        <FileText className="h-4 w-4 mr-1" />
                        Download Resume
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {/* Modals */}
        {showInterviewModal && <InterviewModal />}
        {showStatusModal && <StatusUpdateModal />}
        {showTestModal && <TestAssignmentModal />}
        {showTestResultsModal && <TestResultsModal />}
      </div>
    </div>
  );
};

export default ApplicationsList;