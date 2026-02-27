// src/pages/TestResults.jsx
import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { Award, CheckCircle, XCircle, Home, Download, Clock, Calendar, AlertCircle, AlertTriangle } from 'lucide-react';
import api from '../services/api';

const TestResults = () => {
  const { assignmentId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  
  // Get test type from URL query parameter
  const queryParams = new URLSearchParams(location.search);
  const urlTestType = queryParams.get('type');
  
  const [results, setResults] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [testType, setTestType] = useState(urlTestType || '');
  const [terminationReason, setTerminationReason] = useState('');

  useEffect(() => {
    if (assignmentId) {
      console.log('Fetching results for assignment:', assignmentId, 'type:', urlTestType);
      fetchResults();
    } else {
      setError('Invalid test ID');
      setLoading(false);
    }
  }, [assignmentId, urlTestType]);

  const fetchResults = async () => {
    try {
      setLoading(true);
      
      // If we have the test type from URL, use it directly
      if (urlTestType === 'dreamrole') {
        try {
          console.log('Fetching DreamRole test results for ID:', assignmentId);
          const response = await api.get(`/dream-role-tests/results/${assignmentId}`);
          console.log('DreamRole test results:', response.data);
          setResults(response.data);
          setTestType('dreamrole');
          setTerminationReason(response.data.assignment?.terminationReason || '');
          setLoading(false);
          return;
        } catch (error) {
          console.error('DreamRole test fetch failed:', error);
        }
      } else if (urlTestType === 'manual') {
        try {
          console.log('Fetching manual test results for ID:', assignmentId);
          const response = await api.get(`/aptitude-tests/results/${assignmentId}`);
          console.log('Manual test results:', response.data);
          setResults(response.data);
          setTestType('manual');
          setTerminationReason(response.data.assignment?.terminationReason || '');
          setLoading(false);
          return;
        } catch (error) {
          console.error('Manual test fetch failed:', error);
        }
      }
      
      // If URL doesn't have type or the specific type failed, try both
      console.log('No type specified or fetch failed, trying both endpoints...');
      
      // Try DreamRole test first
      try {
        const dreamRoleResponse = await api.get(`/dream-role-tests/results/${assignmentId}`);
        console.log('DreamRole test results found:', dreamRoleResponse.data);
        setResults(dreamRoleResponse.data);
        setTestType('dreamrole');
        setTerminationReason(dreamRoleResponse.data.assignment?.terminationReason || '');
        setLoading(false);
        return;
      } catch (dreamRoleError) {
        console.log('Not a DreamRole test');
      }
      
      // Try manual test
      try {
        const manualResponse = await api.get(`/aptitude-tests/results/${assignmentId}`);
        console.log('Manual test results found:', manualResponse.data);
        setResults(manualResponse.data);
        setTestType('manual');
        setTerminationReason(manualResponse.data.assignment?.terminationReason || '');
        setLoading(false);
        return;
      } catch (manualError) {
        console.log('Not a manual test');
      }
      
      // If we get here, no results found
      setError('Test results not found');
      
    } catch (error) {
      console.error('Error fetching test results:', error);
      setError(error.response?.data?.error || 'Failed to load results');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (e) {
      return 'Invalid date';
    }
  };

  const handleDownloadResults = () => {
    if (!results) return;
    
    const { assignment, candidateName, candidateEmail, jobTitle } = results;
    const content = `
TEST RESULTS
============

Candidate: ${candidateName || 'Unknown'}
Email: ${candidateEmail || 'Unknown'}
Job: ${jobTitle || 'Unknown'}
Test: ${assignment?.testName || 'Unknown'}
Date: ${formatDate(assignment?.completedAt)}
Test Type: ${testType === 'dreamrole' ? 'DreamRole Test' : 'Manual Test'}
${terminationReason ? `Termination Reason: ${terminationReason}` : ''}

SCORE SUMMARY
-------------
Score: ${assignment?.score || 0}/${assignment?.totalQuestions || 0}
Percentage: ${assignment?.percentage || 0}%
Status: ${assignment?.passed ? 'PASSED' : 'FAILED'}

DETAILS
-------
Started: ${formatDate(assignment?.startedAt)}
Completed: ${formatDate(assignment?.completedAt)}
Correct Answers: ${assignment?.correctAnswers || 0}
Total Questions: ${assignment?.totalQuestions || 0}
Passing Score: ${assignment?.passingScore || 60}%
    `;

    const blob = new Blob([content], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `test-results-${candidateName?.replace(/\s+/g, '_') || 'candidate'}.txt`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  };

  const handleGoToDashboard = () => {
    navigate('/dashboard');
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-2 text-gray-600">Loading your results...</p>
        </div>
      </div>
    );
  }

  if (error || !results) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <div className="bg-red-100 border border-red-400 text-red-700 px-6 py-4 rounded-lg max-w-md">
          <AlertCircle className="h-12 w-12 mx-auto mb-3" />
          <p className="text-center font-medium mb-2">{error || 'Results not found'}</p>
          <button
            onClick={handleGoToDashboard}
            className="mt-2 w-full bg-red-600 text-white py-2 rounded-lg hover:bg-red-700"
          >
            Go to Dashboard
          </button>
        </div>
      </div>
    );
  }

  const { assignment, candidateName, candidateEmail, jobTitle } = results;
  
  // Ensure we have the correct data
  const score = assignment?.score || 0;
  const totalQuestions = assignment?.totalQuestions || 0;
  const percentage = assignment?.percentage || 0;
  const passed = assignment?.passed || false;
  const passingScore = assignment?.passingScore || 60;

  console.log('Rendering results:', {
    testType,
    testName: assignment?.testName,
    score,
    totalQuestions,
    percentage,
    passed,
    passingScore
  });

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-3xl mx-auto px-4">
        {/* Header */}
        <div className="mb-6">
          <button
            onClick={handleGoToDashboard}
            className="flex items-center text-gray-600 hover:text-gray-800"
          >
            <Home className="h-4 w-4 mr-1" />
            Back to Dashboard
          </button>
        </div>

        {/* Results Card */}
        <div className="bg-white rounded-lg shadow-xl overflow-hidden">
          {/* Header with status */}
          <div className={`p-8 text-center ${
            passed ? 'bg-gradient-to-r from-green-600 to-green-500' : 'bg-gradient-to-r from-red-600 to-red-500'
          } text-white`}>
            <div className="inline-block p-4 rounded-full bg-white bg-opacity-20 mb-4">
              {passed ? (
                <Award className="h-16 w-16" />
              ) : (
                <XCircle className="h-16 w-16" />
              )}
            </div>
            <h1 className="text-3xl font-bold mb-2">
              {passed ? 'Congratulations!' : 'Test Results'}
            </h1>
            <p className="text-lg opacity-90">
              {passed ? 'You have successfully passed the test!' : 'You did not pass this time.'}
            </p>
            <div className="mt-2 flex justify-center space-x-2">
              {testType === 'dreamrole' && (
                <span className="bg-purple-600 text-white px-3 py-1 rounded-full text-sm">
                  DreamRole Test
                </span>
              )}
              {testType === 'manual' && (
                <span className="bg-blue-600 text-white px-3 py-1 rounded-full text-sm">
                  Manual Test
                </span>
              )}
            </div>
            {terminationReason && (
              <div className="mt-4 bg-red-700 bg-opacity-30 p-3 rounded-lg">
                <p className="text-sm font-semibold flex items-center justify-center">
                  <AlertTriangle className="h-4 w-4 mr-2" />
                  Test Terminated: {terminationReason}
                </p>
              </div>
            )}
          </div>

          {/* Candidate Info */}
          <div className="p-6 border-b border-gray-200">
            <h2 className="text-lg font-semibold mb-4">Candidate Information</h2>
            <div className="grid grid-cols-2 gap-4">
              <div className="bg-gray-50 p-3 rounded-lg">
                <p className="text-sm text-gray-500">Name</p>
                <p className="font-semibold text-lg">{candidateName || 'Unknown'}</p>
              </div>
              <div className="bg-gray-50 p-3 rounded-lg">
                <p className="text-sm text-gray-500">Email</p>
                <p className="font-semibold">{candidateEmail || 'Unknown'}</p>
              </div>
              <div className="bg-gray-50 p-3 rounded-lg">
                <p className="text-sm text-gray-500">Job Applied</p>
                <p className="font-semibold">{jobTitle || 'Unknown'}</p>
              </div>
              <div className="bg-gray-50 p-3 rounded-lg">
                <p className="text-sm text-gray-500">Test Name</p>
                <p className="font-semibold">{assignment?.testName || 'Unknown'}</p>
              </div>
            </div>
          </div>

          {/* Score Breakdown */}
          <div className="p-6">
            <h2 className="text-lg font-semibold mb-6">Score Breakdown</h2>
            
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8">
              <div className="bg-blue-50 rounded-lg p-4 text-center">
                <p className="text-sm text-gray-600 mb-1">Your Score</p>
                <p className="text-4xl font-bold text-blue-600">
                  {score}/{totalQuestions}
                </p>
              </div>
              <div className="bg-purple-50 rounded-lg p-4 text-center">
                <p className="text-sm text-gray-600 mb-1">Percentage</p>
                <p className={`text-4xl font-bold ${passed ? 'text-green-600' : 'text-red-600'}`}>
                  {percentage}%
                </p>
              </div>
              <div className="bg-yellow-50 rounded-lg p-4 text-center">
                <p className="text-sm text-gray-600 mb-1">Passing Score</p>
                <p className="text-4xl font-bold text-yellow-600">
                  {passingScore}%
                </p>
              </div>
            </div>

            {/* Progress Bar */}
            <div className="mb-8">
              <div className="flex justify-between text-sm mb-2">
                <span>Your Score</span>
                <span className="font-medium">{percentage}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-4">
                <div 
                  className={`h-4 rounded-full ${
                    passed ? 'bg-green-500' : 'bg-red-500'
                  }`}
                  style={{ width: `${percentage}%` }}
                ></div>
              </div>
              <div className="flex justify-between text-xs text-gray-500 mt-1">
                <span>0%</span>
                <span>Passing: {passingScore}%</span>
                <span>100%</span>
              </div>
            </div>

            {/* Result Message */}
            <div className={`p-6 rounded-lg ${
              passed ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'
            } mb-6`}>
              <div className="flex items-start">
                {passed ? (
                  <CheckCircle className="h-6 w-6 text-green-600 mr-3 mt-0.5" />
                ) : (
                  <XCircle className="h-6 w-6 text-red-600 mr-3 mt-0.5" />
                )}
                <div>
                  <p className={`text-lg font-semibold ${passed ? 'text-green-800' : 'text-red-800'}`}>
                    {passed 
                      ? 'You have successfully passed the test!' 
                      : `You scored ${percentage}%, which is below the passing score of ${passingScore}%.`}
                  </p>
                  {!passed && score > 0 && (
                    <p className="text-red-700 mt-2">
                      You got {score} out of {totalQuestions} questions correct.
                    </p>
                  )}
                  {terminationReason && (
                    <p className="text-red-700 mt-2 flex items-center">
                      <AlertTriangle className="h-4 w-4 mr-2" />
                      Test was terminated due to: {terminationReason}
                    </p>
                  )}
                </div>
              </div>
            </div>

            {/* Test Details */}
            <div className="bg-gray-50 rounded-lg p-6">
              <h3 className="font-semibold mb-4">Test Details</h3>
              <div className="grid grid-cols-2 gap-4">
                <div className="flex items-center">
                  <Calendar className="h-5 w-5 text-gray-400 mr-2" />
                  <div>
                    <p className="text-sm text-gray-500">Started</p>
                    <p className="font-medium">{formatDate(assignment?.startedAt)}</p>
                  </div>
                </div>
                <div className="flex items-center">
                  <Clock className="h-5 w-5 text-gray-400 mr-2" />
                  <div>
                    <p className="text-sm text-gray-500">Completed</p>
                    <p className="font-medium">{formatDate(assignment?.completedAt)}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Actions */}
          <div className="p-6 bg-gray-50 border-t border-gray-200">
            <div className="flex space-x-4">
              <button
                onClick={handleGoToDashboard}
                className="flex-1 bg-blue-600 text-white py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
              >
                Go to Dashboard
              </button>
              <button
                onClick={handleDownloadResults}
                className="flex-1 border-2 border-blue-600 text-blue-600 py-3 rounded-lg font-semibold hover:bg-blue-50 transition-colors flex items-center justify-center"
              >
                <Download className="h-5 w-5 mr-2" />
                Download Results
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default TestResults;