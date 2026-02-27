// RecruiterDashboard.jsx
import React, { useState, useEffect } from 'react';
import { 
  Plus, Users, Eye, Edit, Trash2, Briefcase, Building, 
  Mail, Phone, Calendar, BookOpen, BarChart3, X 
} from 'lucide-react';
import api from '../services/api';
import JobPostForm from '../components/JobPostForm';
import ApplicationsList from '../components/ApplicationsList';
import TestManagement from '../components/TestManagement';

const RecruiterDashboard = () => {
  const [jobs, setJobs] = useState([]);
  const [showJobForm, setShowJobForm] = useState(false);
  const [editingJob, setEditingJob] = useState(null);
  const [selectedJob, setSelectedJob] = useState(null);
  const [showApplications, setShowApplications] = useState(false);
  const [showTestManagement, setShowTestManagement] = useState(false);
  const [showTestResults, setShowTestResults] = useState(false);
  const [testResults, setTestResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [recruiterProfile, setRecruiterProfile] = useState(null);
  const [activeTab, setActiveTab] = useState('jobs'); // jobs, tests, results

  const currentUser = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    fetchRecruiterJobs();
    fetchRecruiterProfile();
  }, []);

  const fetchRecruiterJobs = async () => {
    try {
      setLoading(true);
      console.log('Fetching recruiter jobs for user:', currentUser);
      
      // First get recruiter profile to get recruiter ID
      const profileResponse = await api.get(`/recruiters/user/${currentUser.id}`);
      console.log('Recruiter profile response:', profileResponse.data);
      
      if (profileResponse.data.recruiter) {
        const recruiterId = profileResponse.data.recruiter.id;
        console.log('Using recruiter ID:', recruiterId);
        
        const jobsResponse = await api.get(`/recruiters/jobs/recruiter/${recruiterId}`);
        console.log('Recruiter jobs response:', jobsResponse.data);
        setJobs(jobsResponse.data || []);
      } else {
        setError('Recruiter profile not found. Please complete your recruiter profile first.');
      }
    } catch (error) {
      console.error('Error fetching recruiter jobs:', error);
      setError('Failed to load jobs: ' + (error.response?.data?.error || error.message));
      setJobs([]);
    } finally {
      setLoading(false);
    }
  };

  const fetchRecruiterProfile = async () => {
    try {
      const response = await api.get(`/recruiters/user/${currentUser.id}`);
      if (response.data.recruiter) {
        setRecruiterProfile(response.data.recruiter);
      }
    } catch (error) {
      console.error('Error fetching recruiter profile:', error);
    }
  };

  const fetchTestResults = async () => {
    try {
      setLoading(true);
      const response = await api.get(`/aptitude-tests/recruiter/${recruiterProfile?.id}/results`);
      setTestResults(response.data.assignments || []);
      setShowTestResults(true);
    } catch (error) {
      console.error('Error fetching test results:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCreateJob = async (jobData) => {
    try {
      const currentUser = JSON.parse(localStorage.getItem('user'));
      
      // Make sure we have recruiter profile
      if (!recruiterProfile) {
        alert('Please complete your recruiter profile first');
        return;
      }

      const requestData = {
        ...jobData,
        recruiterId: recruiterProfile.id
      };

      console.log('Creating job with data:', requestData);
      
      const response = await api.post('/recruiters/jobs', requestData);
      console.log('Job creation response:', response.data);
      
      setShowJobForm(false);
      fetchRecruiterJobs(); // Refresh the list
      
      alert('Job posted successfully!');
    } catch (error) {
      console.error('Error creating job:', error);
      alert('Failed to create job: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleUpdateJob = async (jobData) => {
    try {
      const requestData = {
        ...jobData,
        recruiterId: recruiterProfile?.id
      };

      const response = await api.put(`/recruiters/jobs/${editingJob.id}`, requestData);
      console.log('Job update response:', response.data);
      
      setEditingJob(null);
      setShowJobForm(false);
      fetchRecruiterJobs(); // Refresh the list
    } catch (error) {
      console.error('Error updating job:', error);
      alert('Failed to update job: ' + (error.response?.data?.error || error.message));
    }
  };

  const handleDeleteJob = async (jobId) => {
    if (!window.confirm('Are you sure you want to delete this job posting?')) {
      return;
    }

    try {
      await api.delete(`/recruiters/jobs/${jobId}?recruiterId=${recruiterProfile?.id}`);
      fetchRecruiterJobs(); // Refresh the list
    } catch (error) {
      console.error('Error deleting job:', error);
      alert('Failed to delete job: ' + (error.response?.data?.error || error.message));
    }
  };

  const viewApplications = (job) => {
    console.log('Viewing applications for job:', job);
    setSelectedJob(job);
    setShowApplications(true);
  };

  const navigateToInterviewPage = () => {
    window.location.href = '/recruiter/interviews';
  };

  const parseSkills = (skillsJson) => {
    try {
      return skillsJson ? JSON.parse(skillsJson) : [];
    } catch {
      return [];
    }
  };

  const getStatusColor = (status) => {
    switch(status) {
      case 'PENDING':
        return 'bg-yellow-100 text-yellow-800';
      case 'COMPLETED':
        return 'bg-green-100 text-green-800';
      case 'PASSED':
        return 'bg-emerald-100 text-emerald-800';
      case 'FAILED':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  if (loading && activeTab !== 'tests') {
    return (
      <div className="max-w-7xl mx-auto py-8 px-4">
        <div className="animate-pulse">
          <div className="h-8 bg-gray-200 rounded w-1/4 mb-8"></div>
          <div className="space-y-4">
            {[...Array(3)].map((_, i) => (
              <div key={i} className="h-32 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto py-8 px-4">
      {/* Header */}
      <div className="mb-8">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h1 className="text-3xl font-bold text-gray-800">Recruiter Dashboard</h1>
            <p className="text-gray-600 mt-2">
              Manage your job postings, view applications, and conduct aptitude tests
            </p>
          </div>
          <div className="flex space-x-3">
            <button
              onClick={navigateToInterviewPage}
              className="bg-purple-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-purple-700 transition-colors flex items-center"
            >
              <Calendar className="h-5 w-5 mr-2" />
              Interview Management
            </button>
            <button
              onClick={() => {
                setActiveTab('tests');
                setShowTestManagement(true);
              }}
              className="bg-indigo-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-indigo-700 transition-colors flex items-center"
            >
              <BookOpen className="h-5 w-5 mr-2" />
              Test Management
            </button>
            <button
              onClick={() => setShowJobForm(true)}
              className="bg-blue-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors flex items-center"
            >
              <Plus className="h-5 w-5 mr-2" />
              Post New Job
            </button>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="border-b border-gray-200 mb-6">
          <nav className="-mb-px flex space-x-8">
            <button
              onClick={() => setActiveTab('jobs')}
              className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center ${
                activeTab === 'jobs'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <Briefcase className="h-5 w-5 mr-2" />
              My Jobs
            </button>
            <button
              onClick={() => {
                setActiveTab('tests');
                setShowTestManagement(true);
              }}
              className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center ${
                activeTab === 'tests'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <BookOpen className="h-5 w-5 mr-2" />
              Aptitude Tests
            </button>
            <button
              onClick={() => {
                setActiveTab('results');
                fetchTestResults();
              }}
              className={`py-4 px-1 border-b-2 font-medium text-sm flex items-center ${
                activeTab === 'results'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <BarChart3 className="h-5 w-5 mr-2" />
              Test Results
            </button>
          </nav>
        </div>

        {/* Recruiter Profile Info */}
        {recruiterProfile && (
          <div className="bg-white rounded-lg shadow-lg p-6 mb-6 border border-gray-200">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center">
                  <Building className="h-8 w-8 text-green-600" />
                </div>
                <div>
                  <h2 className="text-xl font-semibold text-gray-800">{recruiterProfile.companyName}</h2>
                  <p className="text-gray-600">{recruiterProfile.industry}</p>
                  <div className="flex items-center space-x-4 mt-2 text-sm text-gray-500">
                    {recruiterProfile.contactEmail && (
                      <div className="flex items-center">
                        <Mail className="h-4 w-4 mr-1" />
                        {recruiterProfile.contactEmail}
                      </div>
                    )}
                    {recruiterProfile.contactPhone && (
                      <div className="flex items-center">
                        <Phone className="h-4 w-4 mr-1" />
                        {recruiterProfile.contactPhone}
                      </div>
                    )}
                  </div>
                </div>
              </div>
              <div className="text-right">
                <p className="text-2xl font-bold text-blue-600">{jobs.length}</p>
                <p className="text-gray-600">Active Jobs</p>
              </div>
            </div>
          </div>
        )}
      </div>

      {error && (
        <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-6">
          {error}
        </div>
      )}

      {/* Jobs Tab Content */}
      {activeTab === 'jobs' && (
        <div className="space-y-6">
          {jobs.map((job) => (
            <div key={job.id} className="bg-white rounded-lg shadow-lg border border-gray-200 p-6">
              <div className="flex justify-between items-start mb-4">
                <div className="flex-1">
                  <h3 className="text-xl font-semibold text-gray-800 mb-2">{job.title}</h3>
                  <p className="text-lg text-gray-600 mb-2">{job.company}</p>
                  
                  <div className="flex flex-wrap gap-4 mb-3 text-sm text-gray-600">
                    <div className="flex items-center">
                      <Briefcase className="h-4 w-4 mr-1" />
                      {job.location}
                    </div>
                    <div className="flex items-center">
                      <Users className="h-4 w-4 mr-1" />
                      {job.experienceLevel}
                    </div>
                    <div className="flex items-center">
                      <span className="w-2 h-2 bg-green-500 rounded-full mr-2"></span>
                      {job.jobType}
                    </div>
                  </div>

                  <p className="text-gray-700 mb-4 line-clamp-2">
                    {job.description?.substring(0, 200)}...
                  </p>

                  <div className="flex flex-wrap gap-2 mb-4">
                    {parseSkills(job.requiredSkills).slice(0, 5).map((skill, index) => (
                      <span
                        key={index}
                        className="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm"
                      >
                        {skill}
                      </span>
                    ))}
                    {parseSkills(job.requiredSkills).length > 5 && (
                      <span className="bg-gray-100 text-gray-600 px-3 py-1 rounded-full text-sm">
                        +{parseSkills(job.requiredSkills).length - 5} more
                      </span>
                    )}
                  </div>
                </div>
                
                <div className="ml-4 flex flex-col space-y-2">
                  <button
                    onClick={() => viewApplications(job)}
                    className="inline-flex items-center px-4 py-2 bg-green-600 text-white rounded-md text-sm font-medium hover:bg-green-700 transition-colors"
                  >
                    <Eye className="h-4 w-4 mr-1" />
                    View Applications
                  </button>
                  
                  <div className="flex space-x-2">
                    <button
                      onClick={() => setEditingJob(job)}
                      className="flex-1 inline-flex items-center px-3 py-1 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
                    >
                      <Edit className="h-4 w-4 mr-1" />
                      Edit
                    </button>
                    <button
                      onClick={() => handleDeleteJob(job.id)}
                      className="flex-1 inline-flex items-center px-3 py-1 border border-red-300 rounded-md text-sm font-medium text-red-700 bg-white hover:bg-red-50"
                    >
                      <Trash2 className="h-4 w-4 mr-1" />
                      Delete
                    </button>
                  </div>
                </div>
              </div>

              <div className="flex justify-between items-center pt-4 border-t border-gray-200">
                <div className="text-sm text-gray-500">
                  Posted on {new Date(job.postedDate).toLocaleDateString()}
                </div>
                <div className={`px-3 py-1 rounded-full text-xs font-medium ${
                  job.isActive 
                    ? 'bg-green-100 text-green-800'
                    : 'bg-gray-100 text-gray-800'
                }`}>
                  {job.isActive ? 'Active' : 'Inactive'}
                </div>
              </div>
            </div>
          ))}

          {jobs.length === 0 && (
            <div className="text-center py-12">
              <Briefcase className="mx-auto h-16 w-16 text-gray-400 mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No jobs posted yet</h3>
              <p className="text-gray-500 mb-6">Get started by posting your first job opening.</p>
              <button
                onClick={() => setShowJobForm(true)}
                className="bg-blue-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors"
              >
                Post Your First Job
              </button>
            </div>
          )}
        </div>
      )}

      {/* Tests Tab Content */}
      {activeTab === 'tests' && showTestManagement && recruiterProfile && (
        <div className="bg-white rounded-lg shadow-lg p-6">
          <TestManagement recruiterId={recruiterProfile.id} />
        </div>
      )}

      {/* Results Tab Content */}
      {activeTab === 'results' && (
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h3 className="text-xl font-semibold mb-4">Test Results</h3>
          
          {testResults.length === 0 ? (
            <div className="text-center py-8">
              <BarChart3 className="mx-auto h-12 w-12 text-gray-400 mb-4" />
              <p className="text-gray-500">No test results available yet</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Candidate
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Test
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Score
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Status
                    </th>
                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                      Completed
                    </th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {testResults.map((result) => (
                    <tr key={result.assignmentId}>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm font-medium text-gray-900">
                          {result.candidateName}
                        </div>
                        <div className="text-sm text-gray-500">
                          {result.candidateEmail}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900">{result.testName}</div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-gray-900">
                          {result.score}/{result.totalQuestions} ({result.percentage}%)
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(result.status)}`}>
                          {result.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                        {result.completedAt ? new Date(result.completedAt).toLocaleDateString() : '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Job Form Modal */}
      {showJobForm && (
        <JobPostForm
          job={editingJob}
          onSubmit={editingJob ? handleUpdateJob : handleCreateJob}
          onCancel={() => {
            setShowJobForm(false);
            setEditingJob(null);
          }}
        />
      )}

      {/* Applications Modal */}
      {showApplications && selectedJob && (
        <ApplicationsList
          job={selectedJob}
          onClose={() => {
            setShowApplications(false);
            setSelectedJob(null);
          }}
        />
      )}
    </div>
  );
};

export default RecruiterDashboard;