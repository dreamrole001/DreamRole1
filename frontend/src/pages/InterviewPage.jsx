import React, { useState, useEffect } from 'react';
import { 
  Users, Calendar, Clock, Eye, CheckCircle, XCircle, 
  Star, MapPin, Mail, Phone, Briefcase, FileText, 
  Download, TrendingUp, RefreshCw, Filter, ChevronDown,
  User, MessageCircle, Calendar as CalendarIcon, Target
} from 'lucide-react';
import api from '../services/api';
import JobApplicationModal from '../components/JobApplicationModal';

const InterviewPage = () => {
  const [applications, setApplications] = useState([]);
  const [filteredApplications, setFilteredApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeTab, setActiveTab] = useState('all'); // all, shortlisted, scheduled, rejected
  const [selectedApplication, setSelectedApplication] = useState(null);
  const [showDetailsModal, setShowDetailsModal] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [recruiterJobs, setRecruiterJobs] = useState([]);
  const [selectedJob, setSelectedJob] = useState('all');
  const [refreshing, setRefreshing] = useState(false);
  const [showApplicationModal, setShowApplicationModal] = useState(false);
  const [selectedJobForApplication, setSelectedJobForApplication] = useState(null);

  const currentUser = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    fetchRecruiterJobs();
    fetchAllApplications();
  }, []);

  useEffect(() => {
    filterApplications();
  }, [applications, activeTab, searchTerm, selectedJob]);

  const fetchRecruiterJobs = async () => {
    try {
      // First get recruiter profile
      const profileResponse = await api.get(`/recruiters/user/${currentUser.id}`);
      if (profileResponse.data.recruiter) {
        const recruiterId = profileResponse.data.recruiter.id;
        const jobsResponse = await api.get(`/recruiters/jobs/recruiter/${recruiterId}`);
        setRecruiterJobs(jobsResponse.data || []);
      }
    } catch (error) {
      console.error('Error fetching recruiter jobs:', error);
    }
  };

  const fetchAllApplications = async () => {
    try {
      setLoading(true);
      setError('');
      
      // Get recruiter profile
      const profileResponse = await api.get(`/recruiters/user/${currentUser.id}`);
      if (!profileResponse.data.recruiter) {
        setError('Recruiter profile not found');
        return;
      }
      
      const recruiterId = profileResponse.data.recruiter.id;
      let allApps = [];
      
      // Fetch jobs for this recruiter
      const jobsResponse = await api.get(`/recruiters/jobs/recruiter/${recruiterId}`);
      const jobs = jobsResponse.data || [];
      
      // Fetch applications for each job
      for (const job of jobs) {
        try {
          // Try enhanced endpoint first
          const appsResponse = await api.get(`/resume-matching/job/${job.id}/applications/sorted?recruiterId=${recruiterId}`);
          if (appsResponse.data && appsResponse.data.applications) {
            const appsWithJob = appsResponse.data.applications.map(app => ({
              ...app,
              job: job
            }));
            allApps = [...allApps, ...appsWithJob];
          }
        } catch (error) {
          console.log(`No applications for job ${job.id}`);
        }
      }
      
      // Sort by application date (newest first)
      allApps.sort((a, b) => new Date(b.applicationDate) - new Date(a.applicationDate));
      
      setApplications(allApps);
    } catch (error) {
      console.error('Error fetching applications:', error);
      setError('Failed to load applications: ' + (error.response?.data?.error || error.message));
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const filterApplications = () => {
    let filtered = [...applications];
    
    // Filter by job
    if (selectedJob !== 'all') {
      filtered = filtered.filter(app => app.job?.id === parseInt(selectedJob));
    }
    
    // Filter by status based on active tab
    if (activeTab !== 'all') {
      filtered = filtered.filter(app => {
        const status = app.status?.toLowerCase() || '';
        switch (activeTab) {
          case 'shortlisted':
            return status.includes('shortlisted');
          case 'scheduled':
            return status.includes('interview_scheduled') || status.includes('interview scheduled');
          case 'rejected':
            return status.includes('rejected');
          default:
            return true;
        }
      });
    }
    
    // Filter by search term
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(app => 
        app.applicantName?.toLowerCase().includes(term) ||
        app.applicantEmail?.toLowerCase().includes(term) ||
        app.job?.title?.toLowerCase().includes(term) ||
        app.job?.company?.toLowerCase().includes(term)
      );
    }
    
    setFilteredApplications(filtered);
  };

  const handleRefresh = () => {
    setRefreshing(true);
    fetchAllApplications();
  };

  const getStatusColor = (status) => {
    const statusLower = (status || '').toLowerCase();
    if (statusLower.includes('shortlist')) return 'bg-green-100 text-green-800 border-green-200';
    if (statusLower.includes('interview')) return 'bg-yellow-100 text-yellow-800 border-yellow-200';
    if (statusLower.includes('reject')) return 'bg-red-100 text-red-800 border-red-200';
    if (statusLower.includes('viewed')) return 'bg-purple-100 text-purple-800 border-purple-200';
    return 'bg-blue-100 text-blue-800 border-blue-200';
  };

  const getStatusIcon = (status) => {
    const statusLower = (status || '').toLowerCase();
    if (statusLower.includes('shortlist')) return <CheckCircle className="h-4 w-4" />;
    if (statusLower.includes('interview')) return <CalendarIcon className="h-4 w-4" />;
    if (statusLower.includes('reject')) return <XCircle className="h-4 w-4" />;
    if (statusLower.includes('viewed')) return <Eye className="h-4 w-4" />;
    return <Clock className="h-4 w-4" />;
  };

  const getMatchColor = (percentage) => {
    if (percentage >= 80) return 'bg-green-100 text-green-800';
    if (percentage >= 60) return 'bg-yellow-100 text-yellow-800';
    if (percentage >= 40) return 'bg-orange-100 text-orange-800';
    return 'bg-red-100 text-red-800';
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
      
      const applicantName = application.applicantName || 'candidate';
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

  const handleViewDetails = (application) => {
    setSelectedApplication(application);
    setShowDetailsModal(true);
  };

  const getTabCount = (tabType) => {
    if (tabType === 'all') return applications.length;
    
    return applications.filter(app => {
      const status = app.status?.toLowerCase() || '';
      switch (tabType) {
        case 'shortlisted':
          return status.includes('shortlisted');
        case 'scheduled':
          return status.includes('interview_scheduled') || status.includes('interview scheduled');
        case 'rejected':
          return status.includes('rejected');
        default:
          return false;
      }
    }).length;
  };

  const ApplicationCard = ({ application }) => {
    return (
      <div className="bg-white border border-gray-200 rounded-lg p-6 hover:shadow-lg transition-shadow">
        <div className="flex justify-between items-start mb-4">
          <div className="flex items-start space-x-4 flex-1">
            <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center flex-shrink-0">
              <User className="h-6 w-6 text-blue-600" />
            </div>
            <div className="flex-1">
              <div className="flex items-center space-x-3 mb-2">
                <h3 className="text-xl font-semibold text-gray-900">
                  {application.applicantName || 'Unknown Applicant'}
                </h3>
                
                {application.matchPercentage > 0 && (
                  <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getMatchColor(application.matchPercentage)}`}>
                    <TrendingUp className="h-4 w-4 mr-1" />
                    {application.matchPercentage}% Match
                  </span>
                )}
              </div>
              
              <p className="text-gray-600 mb-1">
                {application.job?.title} at {application.job?.company}
              </p>
              
              <div className="flex items-center space-x-4 text-sm text-gray-500">
                <div className="flex items-center">
                  <Mail className="h-4 w-4 mr-1" />
                  {application.applicantEmail || 'No email'}
                </div>
                {application.applicantPhone && (
                  <div className="flex items-center">
                    <Phone className="h-4 w-4 mr-1" />
                    {application.applicantPhone}
                  </div>
                )}
              </div>
            </div>
          </div>
          
          <div className="flex flex-col items-end space-y-2">
            <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border ${getStatusColor(application.status)}`}>
              {getStatusIcon(application.status)}
              <span className="ml-1">
                {application.status?.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) || 'Unknown'}
              </span>
            </span>
            
            <div className="flex space-x-2">
              <button
                onClick={() => handleViewDetails(application)}
                className="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center"
              >
                <Eye className="h-4 w-4 mr-1" />
                View Details
              </button>
              {application.hasResume && (
                <button
                  onClick={() => handleDownloadResume(application)}
                  className="text-green-600 hover:text-green-800 text-sm font-medium flex items-center"
                >
                  <Download className="h-4 w-4 mr-1" />
                  Resume
                </button>
              )}
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mt-4 pt-4 border-t border-gray-100">
          <div className="flex items-center text-sm text-gray-600">
            <Calendar className="h-4 w-4 mr-2 text-gray-400" />
            Applied: {formatDate(application.applicationDate)}
          </div>
          
          <div className="flex items-center text-sm text-gray-600">
            <Briefcase className="h-4 w-4 mr-2 text-gray-400" />
            Experience: {application.applicantExperience || 0} years
          </div>
          
          <div className="flex items-center text-sm text-gray-600">
            <MapPin className="h-4 w-4 mr-2 text-gray-400" />
            {application.job?.location || 'Location N/A'}
          </div>
        </div>

        {application.interviewDate && (
          <div className="mt-3 p-3 bg-yellow-50 border border-yellow-200 rounded-lg">
            <div className="flex items-center">
              <CalendarIcon className="h-5 w-5 text-yellow-600 mr-2" />
              <span className="font-medium text-yellow-800">Interview Scheduled:</span>
              <span className="ml-2 text-yellow-700">{formatDate(application.interviewDate)}</span>
              {application.interviewLocation && (
                <>
                  <span className="mx-2 text-yellow-400">•</span>
                  <span className="text-yellow-700">{application.interviewLocation}</span>
                </>
              )}
            </div>
          </div>
        )}

        {application.matchPercentage > 0 && (
          <div className="mt-3 flex flex-wrap gap-2">
            <div className="flex-1">
              <div className="flex justify-between text-sm mb-1">
                <span className="text-gray-600">Match Score</span>
                <span className={`font-semibold ${
                  application.matchPercentage >= 80 ? 'text-green-600' :
                  application.matchPercentage >= 60 ? 'text-yellow-600' : 'text-orange-600'
                }`}>{application.matchPercentage}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div 
                  className={`h-2 rounded-full ${
                    application.matchPercentage >= 80 ? 'bg-green-500' :
                    application.matchPercentage >= 60 ? 'bg-yellow-500' : 'bg-orange-500'
                  }`}
                  style={{ width: `${application.matchPercentage}%` }}
                ></div>
              </div>
            </div>
          </div>
        )}

        {/* Skills Preview */}
        {application.applicantSkills && (
          <div className="mt-3 flex flex-wrap gap-1">
            {parseSkills(application.applicantSkills).slice(0, 5).map((skill, index) => (
              <span key={index} className="bg-blue-100 text-blue-800 px-2 py-1 rounded text-xs">
                {skill}
              </span>
            ))}
            {parseSkills(application.applicantSkills).length > 5 && (
              <span className="text-gray-500 text-xs">
                +{parseSkills(application.applicantSkills).length - 5} more
              </span>
            )}
          </div>
        )}
      </div>
    );
  };

  const ApplicationDetailsModal = () => {
    if (!selectedApplication) return null;

    return (
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
        <div className="relative top-20 mx-auto p-5 border w-full max-w-4xl shadow-lg rounded-md bg-white">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl font-bold text-gray-900">Application Details</h2>
            <button 
              onClick={() => setShowDetailsModal(false)} 
              className="text-gray-400 hover:text-gray-600"
            >
              <XCircle className="h-6 w-6" />
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* Applicant Information */}
            <div className="bg-gray-50 rounded-lg p-4">
              <h3 className="font-semibold text-gray-800 mb-3 flex items-center">
                <User className="h-5 w-5 mr-2 text-blue-600" />
                Applicant Information
              </h3>
              <div className="space-y-2">
                <p><span className="font-medium">Name:</span> {selectedApplication.applicantName}</p>
                <p><span className="font-medium">Email:</span> {selectedApplication.applicantEmail}</p>
                {selectedApplication.applicantPhone && (
                  <p><span className="font-medium">Phone:</span> {selectedApplication.applicantPhone}</p>
                )}
              </div>
            </div>

            {/* Job Information */}
            <div className="bg-gray-50 rounded-lg p-4">
              <h3 className="font-semibold text-gray-800 mb-3 flex items-center">
                <Briefcase className="h-5 w-5 mr-2 text-green-600" />
                Job Information
              </h3>
              <div className="space-y-2">
                <p><span className="font-medium">Position:</span> {selectedApplication.job?.title}</p>
                <p><span className="font-medium">Company:</span> {selectedApplication.job?.company}</p>
                <p><span className="font-medium">Location:</span> {selectedApplication.job?.location}</p>
              </div>
            </div>

            {/* Application Status */}
            <div className="bg-gray-50 rounded-lg p-4">
              <h3 className="font-semibold text-gray-800 mb-3 flex items-center">
                <Clock className="h-5 w-5 mr-2 text-purple-600" />
                Application Status
              </h3>
              <div className="space-y-2">
                <p><span className="font-medium">Status:</span> 
                  <span className={`ml-2 px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(selectedApplication.status)}`}>
                    {selectedApplication.status?.replace(/_/g, ' ').toLowerCase()}
                  </span>
                </p>
                <p><span className="font-medium">Applied Date:</span> {formatDate(selectedApplication.applicationDate)}</p>
                {selectedApplication.viewedAt && (
                  <p><span className="font-medium">Viewed Date:</span> {formatDate(selectedApplication.viewedAt)}</p>
                )}
              </div>
            </div>

            {/* Interview Details */}
            {selectedApplication.interviewDate && (
              <div className="bg-yellow-50 rounded-lg p-4 border border-yellow-200">
                <h3 className="font-semibold text-yellow-800 mb-3 flex items-center">
                  <CalendarIcon className="h-5 w-5 mr-2" />
                  Interview Details
                </h3>
                <div className="space-y-2">
                  <p><span className="font-medium">Date:</span> {formatDate(selectedApplication.interviewDate)}</p>
                  {selectedApplication.interviewLocation && (
                    <p><span className="font-medium">Location:</span> {selectedApplication.interviewLocation}</p>
                  )}
                </div>
              </div>
            )}

            {/* Skills */}
            {selectedApplication.applicantSkills && (
              <div className="md:col-span-2 bg-gray-50 rounded-lg p-4">
                <h3 className="font-semibold text-gray-800 mb-3 flex items-center">
                  <Star className="h-5 w-5 mr-2 text-yellow-600" />
                  Skills & Technologies
                </h3>
                <div className="flex flex-wrap gap-2">
                  {parseSkills(selectedApplication.applicantSkills).map((skill, index) => (
                    <span key={index} className="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm">
                      {skill}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Cover Letter */}
            {selectedApplication.coverLetter && (
              <div className="md:col-span-2 bg-gray-50 rounded-lg p-4">
                <h3 className="font-semibold text-gray-800 mb-3 flex items-center">
                  <FileText className="h-5 w-5 mr-2 text-indigo-600" />
                  Cover Letter
                </h3>
                <p className="text-gray-700 whitespace-pre-wrap">{selectedApplication.coverLetter}</p>
              </div>
            )}

            {/* Recruiter Notes */}
            {selectedApplication.recruiterNotes && (
              <div className="md:col-span-2 bg-gray-50 rounded-lg p-4">
                <h3 className="font-semibold text-gray-800 mb-3 flex items-center">
                  <MessageCircle className="h-5 w-5 mr-2 text-teal-600" />
                  Recruiter Notes
                </h3>
                <p className="text-gray-700 whitespace-pre-wrap">{selectedApplication.recruiterNotes}</p>
              </div>
            )}
          </div>

          <div className="mt-6 flex justify-end">
            <button
              onClick={() => setShowDetailsModal(false)}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Close
            </button>
          </div>
        </div>
      </div>
    );
  };

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto py-8 px-4">
        <div className="text-center py-12">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading applications...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto py-8 px-4">
      {/* Header */}
      <div className="mb-8">
        <div className="flex justify-between items-center">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">Interview Management</h1>
            <p className="text-gray-600">
              Track and manage all your applications in one place
            </p>
          </div>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 transition-colors"
          >
            <RefreshCw className={`h-4 w-4 mr-2 ${refreshing ? 'animate-spin' : ''}`} />
            Refresh
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 bg-red-100 border border-red-400 text-red-700 rounded-lg">
          {error}
        </div>
      )}

      {/* Filters */}
      <div className="bg-white rounded-lg shadow-lg p-6 mb-6">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between space-y-4 lg:space-y-0">
          <div className="flex-1 max-w-md">
            <div className="relative">
              <input
                type="text"
                placeholder="Search by name, email, job title..."
                className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
              <Filter className="absolute left-3 top-2.5 h-5 w-5 text-gray-400" />
            </div>
          </div>
          
          <div className="flex space-x-4">
            <select
              value={selectedJob}
              onChange={(e) => setSelectedJob(e.target.value)}
              className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">All Jobs</option>
              {recruiterJobs.map(job => (
                <option key={job.id} value={job.id}>{job.title}</option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="-mb-px flex space-x-8">
          {[
            { id: 'all', label: 'All Applications', icon: Users },
            { id: 'shortlisted', label: 'Shortlisted', icon: CheckCircle },
            { id: 'scheduled', label: 'Interview Scheduled', icon: Calendar },
            { id: 'rejected', label: 'Rejected', icon: XCircle }
          ].map((tab) => {
            const Icon = tab.icon;
            const count = getTabCount(tab.id);
            
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex items-center space-x-2 py-4 px-1 border-b-2 font-medium text-sm ${
                  activeTab === tab.id
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                <Icon className="h-5 w-5" />
                <span>{tab.label}</span>
                <span className={`ml-2 px-2 py-0.5 rounded-full text-xs ${
                  activeTab === tab.id
                    ? 'bg-blue-100 text-blue-600'
                    : 'bg-gray-100 text-gray-600'
                }`}>
                  {count}
                </span>
              </button>
            );
          })}
        </nav>
      </div>

      {/* Applications List */}
      {filteredApplications.length === 0 ? (
        <div className="text-center py-12">
          <Users className="mx-auto h-16 w-16 text-gray-400 mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">No applications found</h3>
          <p className="text-gray-500">
            {activeTab === 'all' 
              ? "You don't have any applications yet."
              : `No ${activeTab} applications found.`}
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {filteredApplications.map((application) => (
            <ApplicationCard key={application.id} application={application} />
          ))}
        </div>
      )}

      {/* Details Modal */}
      {showDetailsModal && <ApplicationDetailsModal />}

      {/* Application Modal (for applying) */}
      {showApplicationModal && selectedJobForApplication && (
        <JobApplicationModal
          job={selectedJobForApplication}
          onClose={() => {
            setShowApplicationModal(false);
            setSelectedJobForApplication(null);
          }}
          onSuccess={() => {
            setShowApplicationModal(false);
            setSelectedJobForApplication(null);
            fetchAllApplications();
          }}
        />
      )}
    </div>
  );
};

export default InterviewPage;