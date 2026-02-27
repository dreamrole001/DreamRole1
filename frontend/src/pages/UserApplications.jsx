import React, { useState, useEffect } from 'react';
import { Briefcase, Calendar, Clock, Eye, CheckCircle, XCircle, Star, MapPin, ChevronDown, ChevronUp, RefreshCw, AlertCircle } from 'lucide-react';
import api from '../services/api';

const UserApplications = () => {
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [expandedApplication, setExpandedApplication] = useState(null);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    fetchUserApplications();
  }, []);

  const fetchUserApplications = async () => {
    try {
      setLoading(true);
      setError('');
      const currentUser = JSON.parse(localStorage.getItem('user'));
      
      if (!currentUser || !currentUser.id) {
        setError('User not found. Please log in again.');
        setLoading(false);
        return;
      }

      console.log('Fetching applications for user:', currentUser.id);
      
      const response = await api.get(`/applications/user/${currentUser.id}`);
      console.log('Applications response:', response.data);
      
      if (response.data && response.data.applications) {
        setApplications(response.data.applications);
      } else {
        setApplications([]);
        setError('No applications data received from server');
      }
    } catch (error) {
      console.error('Error fetching user applications:', error);
      const errorMessage = error.response?.data?.error || error.message || 'Failed to load applications';
      setError(errorMessage);
      setApplications([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const handleRefresh = () => {
    setRefreshing(true);
    fetchUserApplications();
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'APPLICATION_SUBMITTED':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'VIEWED_BY_RECRUITER':
        return 'bg-purple-100 text-purple-800 border-purple-200';
      case 'SHORTLISTED':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'INTERVIEW_SCHEDULED':
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
      case 'REJECTED':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'OFFER_SENT':
        return 'bg-emerald-100 text-emerald-800 border-emerald-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'APPLICATION_SUBMITTED':
        return <Clock className="h-4 w-4" />;
      case 'VIEWED_BY_RECRUITER':
        return <Eye className="h-4 w-4" />;
      case 'SHORTLISTED':
        return <CheckCircle className="h-4 w-4" />;
      case 'INTERVIEW_SCHEDULED':
        return <Calendar className="h-4 w-4" />;
      case 'REJECTED':
        return <XCircle className="h-4 w-4" />;
      case 'OFFER_SENT':
        return <Star className="h-4 w-4" />;
      default:
        return <Clock className="h-4 w-4" />;
    }
  };

  const getStatusDescription = (status) => {
    switch (status) {
      case 'APPLICATION_SUBMITTED':
        return 'Your application has been submitted and is awaiting review.';
      case 'VIEWED_BY_RECRUITER':
        return 'A recruiter has viewed your application.';
      case 'SHORTLISTED':
        return 'Your application has been shortlisted for further consideration.';
      case 'INTERVIEW_SCHEDULED':
        return 'An interview has been scheduled. Check the details below.';
      case 'REJECTED':
        return 'Unfortunately, your application was not selected.';
      case 'OFFER_SENT':
        return 'Congratulations! An offer has been sent to you.';
      default:
        return 'Application status unknown.';
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown date';
    try {
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      return 'Invalid date';
    }
  };

  const toggleApplicationDetails = (applicationId) => {
    setExpandedApplication(expandedApplication === applicationId ? null : applicationId);
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

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto p-6">
        <div className="text-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
          <p className="mt-4 text-gray-600">Loading your applications...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 mb-2">My Applications</h1>
          <p className="text-gray-600">Track the status of all your job applications in real-time</p>
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

      {error && (
        <div className="mb-6 p-4 bg-red-100 border border-red-400 text-red-700 rounded-lg flex items-start">
          <AlertCircle className="h-5 w-5 mr-2 mt-0.5 flex-shrink-0" />
          <div>
            <p className="font-medium">Error loading applications</p>
            <p className="text-sm mt-1">{error}</p>
            <button
              onClick={handleRefresh}
              className="mt-2 px-3 py-1 bg-red-600 text-white text-sm rounded hover:bg-red-700 transition-colors"
            >
              Try Again
            </button>
          </div>
        </div>
      )}

      {applications.length === 0 && !error ? (
        <div className="text-center py-12">
          <Briefcase className="mx-auto h-16 w-16 text-gray-400 mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">No applications yet</h3>
          <p className="text-gray-500 mb-4">You haven't applied to any jobs yet.</p>
          <a
            href="/jobs"
            className="inline-flex items-center px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <Briefcase className="h-4 w-4 mr-2" />
            Browse Jobs
          </a>
        </div>
      ) : (
        <div className="space-y-4">
          {applications.map((application) => (
            <div key={application.id} className="border border-gray-200 rounded-lg bg-white shadow-sm hover:shadow-md transition-shadow">
              {/* Application Header */}
              <div className="p-6">
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <h3 className="text-xl font-semibold text-gray-900 mb-1">
                      {application.job?.title || 'Unknown Position'}
                    </h3>
                    <p className="text-gray-600 mb-2">
                      {application.job?.company || 'Unknown Company'}
                      {application.job?.location && ` • ${application.job.location}`}
                      {application.job?.jobType && ` • ${application.job.jobType}`}
                    </p>
                    <div className="flex items-center space-x-4 text-sm text-gray-500">
                      <div className="flex items-center">
                        <Calendar className="h-4 w-4 mr-1" />
                        Applied: {formatDate(application.applicationDate)}
                      </div>
                      {application.statusUpdatedAt && (
                        <div className="flex items-center">
                          <Clock className="h-4 w-4 mr-1" />
                          Updated: {formatDate(application.statusUpdatedAt)}
                        </div>
                      )}
                    </div>
                  </div>
                  
                  <div className="flex flex-col items-end space-y-2">
                    <span className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium border ${getStatusColor(application.status)}`}>
                      {getStatusIcon(application.status)}
                      <span className="ml-1">
                        {application.status ? application.status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase()) : 'Unknown Status'}
                      </span>
                    </span>
                    
                    <button
                      onClick={() => toggleApplicationDetails(application.id)}
                      className="text-blue-600 hover:text-blue-800 text-sm font-medium flex items-center"
                    >
                      {expandedApplication === application.id ? (
                        <>
                          Hide Details <ChevronUp className="h-4 w-4 ml-1" />
                        </>
                      ) : (
                        <>
                          View Details <ChevronDown className="h-4 w-4 ml-1" />
                        </>
                      )}
                    </button>
                  </div>
                </div>

                {/* Status Description */}
                <div className="mt-3 p-3 bg-gray-50 rounded-lg">
                  <p className="text-sm text-gray-700">
                    {getStatusDescription(application.status)}
                  </p>
                </div>
              </div>

              {/* Expanded Details */}
              {expandedApplication === application.id && (
                <div className="border-t border-gray-200 p-6 bg-gray-50">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* Application Details */}
                    <div>
                      <h4 className="font-semibold text-gray-800 mb-3">Application Details</h4>
                      <div className="space-y-2 text-sm">
                        <div>
                          <span className="font-medium text-gray-700">Applied Date:</span>
                          <span className="ml-2 text-gray-600">{formatDate(application.applicationDate)}</span>
                        </div>
                        {application.viewedAt && (
                          <div>
                            <span className="font-medium text-gray-700">Viewed by Recruiter:</span>
                            <span className="ml-2 text-gray-600">{formatDate(application.viewedAt)}</span>
                          </div>
                        )}
                        {application.statusUpdatedAt && (
                          <div>
                            <span className="font-medium text-gray-700">Last Status Update:</span>
                            <span className="ml-2 text-gray-600">{formatDate(application.statusUpdatedAt)}</span>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Qualifications */}
                    <div>
                      <h4 className="font-semibold text-gray-800 mb-3">Your Qualifications</h4>
                      <div className="space-y-2 text-sm">
                        {application.applicantExperience && (
                          <div>
                            <span className="font-medium text-gray-700">Experience:</span>
                            <span className="ml-2 text-gray-600">{application.applicantExperience} years</span>
                          </div>
                        )}
                        {application.applicantEducation && (
                          <div>
                            <span className="font-medium text-gray-700">Education:</span>
                            <span className="ml-2 text-gray-600">{application.applicantEducation}</span>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Interview Details */}
                    {application.interviewDate && (
                      <div className="md:col-span-2">
                        <h4 className="font-semibold text-gray-800 mb-3">Interview Scheduled</h4>
                        <div className="bg-white border border-gray-200 rounded-lg p-4">
                          <div className="space-y-2 text-sm">
                            <div className="flex items-center">
                              <Calendar className="h-4 w-4 mr-2 text-gray-500" />
                              <span className="text-gray-600">{formatDate(application.interviewDate)}</span>
                            </div>
                            {application.interviewLocation && (
                              <div className="flex items-center">
                                <MapPin className="h-4 w-4 mr-2 text-gray-500" />
                                <span className="text-gray-600">{application.interviewLocation}</span>
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Skills */}
                    {application.applicantSkills && (
                      <div className="md:col-span-2">
                        <h4 className="font-semibold text-gray-800 mb-2">Skills & Technologies</h4>
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

                    {/* Recruiter Notes */}
                    {application.recruiterNotes && (
                      <div className="md:col-span-2">
                        <h4 className="font-semibold text-gray-800 mb-2">Recruiter Notes</h4>
                        <div className="bg-white border border-gray-200 rounded-lg p-4">
                          <p className="text-sm text-gray-700 whitespace-pre-wrap">{application.recruiterNotes}</p>
                        </div>
                      </div>
                    )}

                    {/* Cover Letter */}
                    {application.coverLetter && (
                      <div className="md:col-span-2">
                        <h4 className="font-semibold text-gray-800 mb-2">Your Cover Letter</h4>
                        <div className="bg-white border border-gray-200 rounded-lg p-4">
                          <p className="text-sm text-gray-700 whitespace-pre-wrap">{application.coverLetter}</p>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default UserApplications;