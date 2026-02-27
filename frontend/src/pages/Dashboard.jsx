// src/pages/Dashboard.jsx
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { 
  User, Briefcase, Upload, FileText, BarChart3, 
  BookOpen, Clock, Award, Calendar, MapPin, CheckCircle, XCircle,
  TrendingUp
} from 'lucide-react';
import UserApplications from './UserApplications';
import api from '../services/api';

const Dashboard = () => {
  const [activeTab, setActiveTab] = useState('overview');
  const [assignedTests, setAssignedTests] = useState([]);
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState({
    totalApplications: 0,
    pendingTests: 0,
    completedTests: 0,
    passedTests: 0,
    scheduledInterviews: 0
  });

  const currentUser = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    fetchUserData();
  }, []);

  const fetchUserData = async () => {
    try {
      setLoading(true);
      
      // Fetch user applications
      const applicationsResponse = await api.get(`/applications/user/${currentUser.id}`);
      const userApplications = applicationsResponse.data.applications || [];
      setApplications(userApplications);
      
      // Fetch assigned tests
      const testsWithDetails = [];
      let pendingCount = 0;
      let completedCount = 0;
      let passedCount = 0;
      let interviewCount = 0;
      
      for (const app of userApplications) {
        // Count interviews
        if (app.status === 'INTERVIEW_SCHEDULED') {
          interviewCount++;
        }
        
        // Check for manual tests
        try {
          const testResponse = await api.get(`/aptitude-tests/application/${app.id}`);
          if (testResponse.data.hasTest) {
            const testData = testResponse.data.assignment;
            testsWithDetails.push({
              id: testData.id,
              testId: testData.testId,
              testName: testData.testName,
              assignedAt: testData.assignedAt,
              startedAt: testData.startedAt,
              completedAt: testData.completedAt,
              deadline: testData.deadline,
              status: testData.status,
              score: testData.score,
              correctAnswers: testData.correctAnswers,
              totalQuestions: testData.totalQuestions,
              passed: testData.passed,
              percentage: testData.percentage,
              jobTitle: app.job?.title,
              company: app.job?.company,
              applicationId: app.id,
              applicationStatus: app.status,
              testType: 'manual',
              test: testData.test
            });
            
            if (testData.status === 'PENDING' || testData.status === 'IN_PROGRESS') {
              pendingCount++;
            } else if (testData.status === 'COMPLETED') {
              completedCount++;
              if (testData.passed) {
                passedCount++;
              }
            }
          }
        } catch (error) {
          console.log(`No manual test for application ${app.id}`);
        }
        
        // Check for DreamRole tests
        try {
          const dreamRoleResponse = await api.get(`/dream-role-tests/application/${app.id}`);
          if (dreamRoleResponse.data.hasTest) {
            const testData = dreamRoleResponse.data.assignment;
            testsWithDetails.push({
              id: testData.id,
              testId: testData.testId,
              testName: testData.testName,
              assignedAt: testData.assignedAt,
              startedAt: testData.startedAt,
              completedAt: testData.completedAt,
              deadline: testData.deadline,
              status: testData.status,
              score: testData.score,
              correctAnswers: testData.correctAnswers,
              totalQuestions: testData.totalQuestions,
              passed: testData.passed,
              percentage: testData.percentage,
              jobTitle: app.job?.title,
              company: app.job?.company,
              applicationId: app.id,
              applicationStatus: app.status,
              testType: 'dreamrole',
              test: testData.test
            });
            
            if (testData.status === 'PENDING' || testData.status === 'IN_PROGRESS') {
              pendingCount++;
            } else if (testData.status === 'COMPLETED') {
              completedCount++;
              if (testData.passed) {
                passedCount++;
              }
            }
          }
        } catch (error) {
          console.log(`No DreamRole test for application ${app.id}`);
        }
      }
      
      setAssignedTests(testsWithDetails);
      setStats({
        totalApplications: userApplications.length,
        pendingTests: pendingCount,
        completedTests: completedCount,
        passedTests: passedCount,
        scheduledInterviews: interviewCount
      });
      
    } catch (error) {
      console.error('Error fetching user data:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusBadge = (status) => {
    switch(status) {
      case 'PENDING':
        return <span className="px-2 py-1 bg-yellow-100 text-yellow-800 rounded-full text-xs">Pending</span>;
      case 'IN_PROGRESS':
        return <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded-full text-xs">In Progress</span>;
      case 'COMPLETED':
        return <span className="px-2 py-1 bg-green-100 text-green-800 rounded-full text-xs">Completed</span>;
      case 'EXPIRED':
        return <span className="px-2 py-1 bg-red-100 text-red-800 rounded-full text-xs">Expired</span>;
      default:
        return <span className="px-2 py-1 bg-gray-100 text-gray-800 rounded-full text-xs">{status}</span>;
    }
  };

  const getApplicationStatusBadge = (status) => {
    switch(status) {
      case 'APPLICATION_SUBMITTED':
        return <span className="px-2 py-1 bg-blue-100 text-blue-800 rounded-full text-xs">Submitted</span>;
      case 'VIEWED_BY_RECRUITER':
        return <span className="px-2 py-1 bg-purple-100 text-purple-800 rounded-full text-xs">Viewed</span>;
      case 'SHORTLISTED':
        return <span className="px-2 py-1 bg-green-100 text-green-800 rounded-full text-xs">Shortlisted</span>;
      case 'TEST_ASSIGNED':
        return <span className="px-2 py-1 bg-orange-100 text-orange-800 rounded-full text-xs">Test Assigned</span>;
      case 'TEST_PASSED':
        return <span className="px-2 py-1 bg-emerald-100 text-emerald-800 rounded-full text-xs">Test Passed</span>;
      case 'TEST_FAILED':
        return <span className="px-2 py-1 bg-red-100 text-red-800 rounded-full text-xs">Test Failed</span>;
      case 'INTERVIEW_SCHEDULED':
        return <span className="px-2 py-1 bg-yellow-100 text-yellow-800 rounded-full text-xs">Interview Scheduled</span>;
      case 'OFFER_SENT':
        return <span className="px-2 py-1 bg-indigo-100 text-indigo-800 rounded-full text-xs">Offer Sent</span>;
      case 'REJECTED':
        return <span className="px-2 py-1 bg-gray-100 text-gray-800 rounded-full text-xs">Rejected</span>;
      default:
        return <span className="px-2 py-1 bg-gray-100 text-gray-800 rounded-full text-xs">{status}</span>;
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatDateOnly = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('en-US', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

  const formatTimeOnly = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleTimeString('en-US', {
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const isDeadlineNear = (deadline) => {
    const deadlineDate = new Date(deadline);
    const now = new Date();
    const diffHours = (deadlineDate - now) / (1000 * 60 * 60);
    return diffHours > 0 && diffHours < 24;
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto py-8 px-4">
        <div className="text-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-2 text-gray-600">Loading your dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto py-8 px-4">
      {/* Welcome Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-800 mb-2">
          Welcome back, {currentUser?.fullName || 'User'}!
        </h1>
        <p className="text-gray-600">Track your job applications, tests, and interviews</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 mb-8">
        <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Applications</p>
              <p className="text-2xl font-bold text-blue-600">{stats.totalApplications}</p>
            </div>
            <Briefcase className="h-8 w-8 text-blue-400" />
          </div>
        </div>

        <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Pending Tests</p>
              <p className="text-2xl font-bold text-yellow-600">{stats.pendingTests}</p>
            </div>
            <Clock className="h-8 w-8 text-yellow-400" />
          </div>
        </div>

        <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Completed Tests</p>
              <p className="text-2xl font-bold text-green-600">{stats.completedTests}</p>
            </div>
            <CheckCircle className="h-8 w-8 text-green-400" />
          </div>
        </div>

        <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Tests Passed</p>
              <p className="text-2xl font-bold text-emerald-600">{stats.passedTests}</p>
            </div>
            <Award className="h-8 w-8 text-emerald-400" />
          </div>
        </div>

        <div className="bg-white p-4 rounded-lg shadow border border-gray-200">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500">Interviews</p>
              <p className="text-2xl font-bold text-purple-600">{stats.scheduledInterviews}</p>
            </div>
            <Calendar className="h-8 w-8 text-purple-400" />
          </div>
        </div>
      </div>

      {/* Tab Navigation */}
      <div className="border-b border-gray-200 mb-8">
        <nav className="-mb-px flex space-x-8">
          <button
            onClick={() => setActiveTab('overview')}
            className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center ${
              activeTab === 'overview'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            <BarChart3 className="h-5 w-5 mr-2" />
            Overview
          </button>
          <button
            onClick={() => setActiveTab('tests')}
            className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center ${
              activeTab === 'tests'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            <BookOpen className="h-5 w-5 mr-2" />
            My Tests
          </button>
          <button
            onClick={() => setActiveTab('applications')}
            className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center ${
              activeTab === 'applications'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            <FileText className="h-5 w-5 mr-2" />
            My Applications
          </button>
          <button
            onClick={() => setActiveTab('interviews')}
            className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center ${
              activeTab === 'interviews'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            <Calendar className="h-5 w-5 mr-2" />
            Interviews
          </button>
        </nav>
      </div>

      {/* Tab Content */}
      {activeTab === 'overview' && (
        <div>
          {/* Quick Actions */}
          <div className="grid md:grid-cols-3 gap-6 mb-8">
            <div className="bg-white p-6 rounded-lg shadow-lg border border-gray-200 hover:shadow-xl transition-shadow">
              <div className="flex items-center mb-4">
                <User className="h-8 w-8 text-blue-600 mr-3" />
                <h3 className="text-xl font-semibold">Profile</h3>
              </div>
              <p className="text-gray-600 mb-4">Complete your profile for better job matches</p>
              <button className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700 transition-colors">
                Update Profile
              </button>
            </div>

            <div className="bg-white p-6 rounded-lg shadow-lg border border-gray-200 hover:shadow-xl transition-shadow">
              <div className="flex items-center mb-4">
                <Briefcase className="h-8 w-8 text-green-600 mr-3" />
                <h3 className="text-xl font-semibold">Job Applications</h3>
              </div>
              <p className="text-gray-600 mb-4">Track your job applications and status</p>
              <Link to="/jobs" className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700 transition-colors inline-block">
                Browse Jobs
              </Link>
            </div>

            <div className="bg-white p-6 rounded-lg shadow-lg border border-gray-200 hover:shadow-xl transition-shadow">
              <div className="flex items-center mb-4">
                <Upload className="h-8 w-8 text-purple-600 mr-3" />
                <h3 className="text-xl font-semibold">Resume</h3>
              </div>
              <p className="text-gray-600 mb-4">Upload and analyze your resume</p>
              <Link to="/upload" className="bg-purple-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-purple-700 transition-colors inline-block">
                Upload Resume
              </Link>
            </div>
          </div>

          {/* Upcoming Interviews Section */}
          {applications.filter(app => app.status === 'INTERVIEW_SCHEDULED').length > 0 && (
            <div className="bg-white rounded-lg shadow-lg border border-gray-200 p-6 mb-8">
              <h2 className="text-2xl font-bold text-gray-800 mb-4 flex items-center">
                <Calendar className="h-6 w-6 text-purple-600 mr-2" />
                Upcoming Interviews
              </h2>
              <div className="space-y-4">
                {applications
                  .filter(app => app.status === 'INTERVIEW_SCHEDULED')
                  .slice(0, 3)
                  .map((app, index) => (
                    <div key={`interview-${app.id}-${index}`} className="border-l-4 border-purple-500 bg-purple-50 p-4 rounded-r-lg">
                      <div className="flex justify-between items-start">
                        <div>
                          <h3 className="font-semibold text-gray-800">{app.job?.title}</h3>
                          <p className="text-sm text-gray-600">{app.job?.company}</p>
                          <div className="mt-2 space-y-1">
                            <div className="flex items-center text-sm text-gray-700">
                              <Calendar className="h-4 w-4 mr-2 text-purple-600" />
                              <span className="font-medium">Date:</span>
                              <span className="ml-2">{formatDateOnly(app.interviewDate)}</span>
                            </div>
                            <div className="flex items-center text-sm text-gray-700">
                              <Clock className="h-4 w-4 mr-2 text-purple-600" />
                              <span className="font-medium">Time:</span>
                              <span className="ml-2">{formatTimeOnly(app.interviewDate)}</span>
                            </div>
                            {app.interviewLocation && (
                              <div className="flex items-center text-sm text-gray-700">
                                <MapPin className="h-4 w-4 mr-2 text-purple-600" />
                                <span className="font-medium">Location:</span>
                                <span className="ml-2">{app.interviewLocation}</span>
                              </div>
                            )}
                          </div>
                        </div>
                        <span className="px-3 py-1 bg-purple-200 text-purple-800 rounded-full text-xs font-medium">
                          Upcoming
                        </span>
                      </div>
                    </div>
                  ))}
              </div>
            </div>
          )}

          {/* Recent Tests */}
          {assignedTests.length > 0 && (
            <div className="bg-white rounded-lg shadow-lg border border-gray-200 p-6 mb-8">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold text-gray-800">Recent Tests</h2>
                <button 
                  onClick={() => setActiveTab('tests')}
                  className="text-blue-600 hover:text-blue-800 font-medium"
                >
                  View All
                </button>
              </div>
              
              <div className="space-y-4">
                {assignedTests.slice(0, 3).map((test, index) => (
                  <div key={`test-${test.id}-${index}`} className="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-shadow">
                    <div className="flex justify-between items-start">
                      <div className="flex-1">
                        <div className="flex items-center space-x-2">
                          <h3 className="font-semibold text-gray-800">{test.testName}</h3>
                          {test.testType === 'dreamrole' && (
                            <span className="px-2 py-0.5 bg-purple-100 text-purple-800 rounded-full text-xs">
                              DreamRole
                            </span>
                          )}
                        </div>
                        <p className="text-sm text-gray-600">For: {test.jobTitle} at {test.company}</p>
                        <div className="flex items-center space-x-4 mt-2 text-xs">
                          <span className="flex items-center">
                            <Clock className="h-3 w-3 mr-1 text-gray-400" />
                            Deadline: {formatDate(test.deadline)}
                          </span>
                          {getStatusBadge(test.status)}
                        </div>
                        {test.status === 'COMPLETED' && test.score !== null && (
                          <div className="mt-2">
                            <span className={`text-sm font-medium ${test.passed ? 'text-green-600' : 'text-red-600'}`}>
                              Score: {test.score}/{test.totalQuestions} ({test.percentage}%) - {test.passed ? 'PASSED' : 'FAILED'}
                            </span>
                          </div>
                        )}
                      </div>
                      {test.status === 'PENDING' && (
                        <Link
                          to={`/test/${test.id}`}
                          className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700"
                        >
                          Start Test
                        </Link>
                      )}
                      {test.status === 'IN_PROGRESS' && (
                        <Link
                          to={`/test/${test.id}`}
                          className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-blue-700"
                        >
                          Resume Test
                        </Link>
                      )}
                      {test.status === 'COMPLETED' && (
                        <Link
                          to={`/test-results/${test.id}?type=${test.testType}`}
                          className="text-blue-600 hover:text-blue-800 text-sm font-medium"
                        >
                          View Results
                        </Link>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Recent Applications */}
          {applications.length > 0 && (
            <div className="bg-white rounded-lg shadow-lg border border-gray-200 p-6">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-2xl font-bold text-gray-800">Recent Applications</h2>
                <button 
                  onClick={() => setActiveTab('applications')}
                  className="text-blue-600 hover:text-blue-800 font-medium"
                >
                  View All
                </button>
              </div>
              
              <div className="space-y-4">
                {applications.slice(0, 3).map((app, index) => (
                  <div key={`app-${app.id}-${index}`} className="border border-gray-200 rounded-lg p-4">
                    <div className="flex justify-between items-start">
                      <div>
                        <h3 className="font-semibold text-gray-800">{app.job?.title}</h3>
                        <p className="text-sm text-gray-600">{app.job?.company}</p>
                        <div className="flex items-center space-x-3 mt-2">
                          {getApplicationStatusBadge(app.status)}
                          <span className="text-xs text-gray-500">
                            Applied: {formatDate(app.applicationDate)}
                          </span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Tests Tab */}
      {activeTab === 'tests' && (
        <div className="bg-white rounded-lg shadow-lg border border-gray-200 p-6">
          <h2 className="text-2xl font-bold text-gray-800 mb-6">My Tests</h2>
          
          {assignedTests.length === 0 ? (
            <div className="text-center py-12">
              <BookOpen className="mx-auto h-16 w-16 text-gray-400 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No tests assigned yet</h3>
              <p className="text-gray-500">When recruiters assign you tests, they will appear here.</p>
              <Link
                to="/jobs"
                className="mt-4 inline-block bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700"
              >
                Browse Jobs
              </Link>
            </div>
          ) : (
            <div className="space-y-4">
              {assignedTests.map((test, index) => (
                <div key={`test-full-${test.id}-${index}`} className="border border-gray-200 rounded-lg p-6 hover:shadow-lg transition-shadow">
                  <div className="flex justify-between items-start">
                    <div className="flex-1">
                      <div className="flex items-center space-x-3 mb-2">
                        <h3 className="text-xl font-semibold text-gray-900">{test.testName}</h3>
                        {test.testType === 'dreamrole' && (
                          <span className="px-2 py-1 bg-purple-100 text-purple-800 rounded-full text-xs font-medium">
                            DreamRole
                          </span>
                        )}
                        {getStatusBadge(test.status)}
                      </div>
                      
                      <p className="text-gray-600 mb-2">
                        For: {test.jobTitle} at {test.company}
                      </p>
                      
                      <div className="grid grid-cols-2 gap-4 mt-4 text-sm">
                        <div>
                          <p className="text-gray-500">Assigned:</p>
                          <p className="font-medium">{formatDate(test.assignedAt)}</p>
                        </div>
                        <div>
                          <p className="text-gray-500">Deadline:</p>
                          <p className={`font-medium ${isDeadlineNear(test.deadline) ? 'text-red-600' : ''}`}>
                            {formatDate(test.deadline)}
                            {isDeadlineNear(test.deadline) && test.status === 'PENDING' && 
                              ' (Less than 24 hours left!)'}
                          </p>
                        </div>
                      </div>

                      {test.status === 'COMPLETED' && test.score !== null && (
                        <div className="mt-4 p-4 bg-gray-50 rounded-lg">
                          <div className="flex items-center justify-between">
                            <div>
                              <p className="text-sm text-gray-600">Your Score</p>
                              <p className="text-2xl font-bold text-blue-600">
                                {test.score}/{test.totalQuestions}
                              </p>
                            </div>
                            <div className="text-right">
                              <p className="text-sm text-gray-600">Percentage</p>
                              <p className={`text-2xl font-bold ${test.passed ? 'text-green-600' : 'text-red-600'}`}>
                                {test.percentage}%
                              </p>
                            </div>
                            <div>
                              <span className={`px-3 py-1 rounded-full text-sm ${
                                test.passed ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'
                              }`}>
                                {test.passed ? 'PASSED' : 'FAILED'}
                              </span>
                            </div>
                          </div>
                        </div>
                      )}
                    </div>
                    
                    <div className="ml-6">
                      {test.status === 'PENDING' && (
                        <Link
                          to={`/test/${test.id}`}
                          className="bg-green-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-green-700 flex items-center"
                        >
                          <Clock className="h-5 w-5 mr-2" />
                          Start Test
                        </Link>
                      )}
                      {test.status === 'IN_PROGRESS' && (
                        <Link
                          to={`/test/${test.id}`}
                          className="bg-blue-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-blue-700 flex items-center"
                        >
                          <BookOpen className="h-5 w-5 mr-2" />
                          Resume Test
                        </Link>
                      )}
                      {test.status === 'COMPLETED' && (
                        <Link
                          to={`/test-results/${test.id}?type=${test.testType}`}
                          className="bg-purple-600 text-white px-6 py-3 rounded-lg font-medium hover:bg-purple-700 flex items-center"
                        >
                          <Award className="h-5 w-5 mr-2" />
                          View Results
                        </Link>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Applications Tab */}
      {activeTab === 'applications' && (
        <div>
          <UserApplications />
        </div>
      )}

      {/* Interviews Tab */}
      {activeTab === 'interviews' && (
        <div className="bg-white rounded-lg shadow-lg border border-gray-200 p-6">
          <h2 className="text-2xl font-bold text-gray-800 mb-6">My Interviews</h2>
          
          {applications.filter(app => app.status === 'INTERVIEW_SCHEDULED').length === 0 ? (
            <div className="text-center py-12">
              <Calendar className="mx-auto h-16 w-16 text-gray-400 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No interviews scheduled</h3>
              <p className="text-gray-500">When recruiters schedule interviews, they will appear here.</p>
            </div>
          ) : (
            <div className="space-y-4">
              {applications
                .filter(app => app.status === 'INTERVIEW_SCHEDULED')
                .map((app, index) => (
                  <div key={`interview-full-${app.id}-${index}`} className="border-l-4 border-purple-500 bg-purple-50 p-6 rounded-r-lg">
                    <div className="flex justify-between items-start">
                      <div className="flex-1">
                        <div className="flex items-center justify-between mb-3">
                          <h3 className="text-xl font-semibold text-gray-900">{app.job?.title}</h3>
                          <span className="px-3 py-1 bg-purple-200 text-purple-800 rounded-full text-xs font-medium">
                            Scheduled
                          </span>
                        </div>
                        
                        <p className="text-gray-700 mb-4">at {app.job?.company}</p>
                        
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          <div className="bg-white p-4 rounded-lg">
                            <h4 className="font-semibold mb-3">Interview Details</h4>
                            <div className="space-y-2">
                              <div className="flex items-start">
                                <Calendar className="h-4 w-4 text-gray-400 mr-2 mt-0.5" />
                                <div>
                                  <p className="text-sm text-gray-500">Date</p>
                                  <p className="font-medium">{formatDateOnly(app.interviewDate)}</p>
                                </div>
                              </div>
                              <div className="flex items-start">
                                <Clock className="h-4 w-4 text-gray-400 mr-2 mt-0.5" />
                                <div>
                                  <p className="text-sm text-gray-500">Time</p>
                                  <p className="font-medium">{formatTimeOnly(app.interviewDate)}</p>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Dashboard;