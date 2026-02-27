import React, { useState, useEffect } from 'react';
import { Search, MapPin, DollarSign, Calendar, Bookmark, Share2, Star, TrendingUp, Clock } from 'lucide-react';
import api from '../services/api';
import JobApplicationModal from '../components/JobApplicationModal';
import JobRatingModal from '../components/JobRatingModal';

const Jobs = () => {
  const [jobs, setJobs] = useState([]);
  const [filteredJobs, setFilteredJobs] = useState([]);
  const [recommendedJobs, setRecommendedJobs] = useState([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedJob, setSelectedJob] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [showRecommended, setShowRecommended] = useState(false);
  const [userSkills, setUserSkills] = useState([]);
  const [showApplicationModal, setShowApplicationModal] = useState(false);
  const [selectedJobForApplication, setSelectedJobForApplication] = useState(null);
  const [showRatingModal, setShowRatingModal] = useState(false);
  const [userRatings, setUserRatings] = useState({});
  const [sortBy, setSortBy] = useState('date'); // 'date', 'rating', 'relevance'

  const currentUser = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    fetchJobs();
    fetchRecommendedJobs();
    fetchUserRatings();
  }, []);

  useEffect(() => {
    let filtered = showRecommended && recommendedJobs.length > 0 ? recommendedJobs : jobs;
    
    // Apply search filter
    if (searchTerm) {
      filtered = filtered.filter(job =>
        job.title?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        job.company?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        job.description?.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }
    
    // Apply sorting
    filtered = sortJobs(filtered, sortBy);
    
    setFilteredJobs(filtered);
  }, [searchTerm, jobs, recommendedJobs, showRecommended, sortBy]);

  const fetchJobs = async () => {
    try {
      setIsLoading(true);
      setError('');
      console.log('Fetching all jobs from backend...');
      
      const response = await api.get('/jobs');
      console.log('Backend jobs response:', response.data);
      
      if (response.data && response.data.length > 0) {
        const transformedJobs = response.data.map(job => ({
          id: job.id,
          jobId: job.id,
          title: job.title,
          company: job.company,
          description: job.description,
          location: job.location,
          salaryRange: job.salaryRange,
          jobType: job.jobType,
          experienceLevel: job.experienceLevel,
          requiredSkills: job.requiredSkills,
          preferredSkills: job.preferredSkills,
          postedDate: job.postedDate,
          averageRating: job.averageRating || 0,
          totalRatings: job.totalRatings || 0,
          matchScore: null,
          matchedSkills: [],
          missingSkills: []
        }));
        
        console.log('Transformed backend jobs:', transformedJobs);
        setJobs(transformedJobs);
        setFilteredJobs(sortJobs(transformedJobs, sortBy));
      } else {
        setJobs([]);
        setFilteredJobs([]);
        setError('No jobs found in the database.');
      }
    } catch (error) {
      console.error('Error fetching jobs from backend:', error);
      setError('Failed to load jobs from backend. Please check if the backend is running.');
      setJobs([]);
      setFilteredJobs([]);
    } finally {
      setIsLoading(false);
    }
  };

  // FIXED: Updated fetchRecommendedJobs to handle complete job data
  const fetchRecommendedJobs = async () => {
    try {
      const response = await api.get('/jobs/recommendations/1/detailed');
      const data = response.data;
      
      if (data.detailedRecommendations) {
        // Transform the recommended jobs to include all necessary fields
        const transformedRecommendations = data.detailedRecommendations.map(job => ({
          id: job.jobId,
          jobId: job.jobId,
          title: job.title || 'No Title',
          company: job.company || 'No Company',
          description: job.description || 'No description available',
          location: job.location || 'Location not specified',
          salaryRange: job.salaryRange || 'Salary not specified',
          jobType: job.jobType || 'Full-time',
          experienceLevel: job.experienceLevel || 'Not specified',
          requiredSkills: job.requiredSkills || '[]',
          preferredSkills: job.preferredSkills || '[]',
          postedDate: job.postedDate || new Date().toISOString(),
          averageRating: job.averageRating || 0,
          totalRatings: job.totalRatings || 0,
          matchScore: job.matchScore || null,
          matchedSkills: job.matchedSkills || [],
          missingSkills: job.missingSkills || []
        }));
        
        setRecommendedJobs(transformedRecommendations);
        setUserSkills(data.userSkills || []);
      }
    } catch (error) {
      console.error('Error fetching recommendations:', error);
      // Continue without recommendations
    }
  };

  const fetchUserRatings = async () => {
    if (!currentUser) return;
    
    try {
      const ratings = {};
      // For each job, check if user has rated it
      const jobsToCheck = showRecommended && recommendedJobs.length > 0 ? recommendedJobs : jobs;
      
      for (const job of jobsToCheck) {
        try {
          const response = await api.get(`/jobs/${job.id}/rating/user/${currentUser.id}`);
          if (response.data.hasRated) {
            ratings[job.id] = response.data.rating;
          }
        } catch (error) {
          console.error(`Error fetching rating for job ${job.id}:`, error);
        }
      }
      
      setUserRatings(ratings);
    } catch (error) {
      console.error('Error fetching user ratings:', error);
    }
  };

  const sortJobs = (jobs, sortType) => {
    const jobsCopy = [...jobs];
    
    switch (sortType) {
      case 'date':
        return jobsCopy.sort((a, b) => new Date(b.postedDate) - new Date(a.postedDate));
      case 'rating':
        return jobsCopy.sort((a, b) => (b.averageRating || 0) - (a.averageRating || 0));
      case 'relevance':
        return jobsCopy.sort((a, b) => (b.matchScore || 0) - (a.matchScore || 0));
      default:
        return jobsCopy;
    }
  };

  const applyForJob = async (job) => {
    setSelectedJobForApplication(job);
    setShowApplicationModal(true);
  };

  const rateJob = async (job) => {
    setSelectedJob(job);
    setShowRatingModal(true);
  };

  const handleRatingSuccess = () => {
    fetchJobs(); // Refresh jobs to get updated ratings
    fetchUserRatings(); // Refresh user ratings
    setShowRatingModal(false);
  };

  const handleApplicationSuccess = () => {
    alert('Application submitted successfully!');
    setShowApplicationModal(false);
    setSelectedJobForApplication(null);
  };

  const parseSkills = (skillsJson) => {
    try {
      return skillsJson ? JSON.parse(skillsJson) : [];
    } catch {
      return [];
    }
  };

  const getMatchColor = (score) => {
    if (score >= 80) return 'bg-green-100 text-green-800';
    if (score >= 60) return 'bg-blue-100 text-blue-800';
    if (score >= 40) return 'bg-yellow-100 text-yellow-800';
    return 'bg-gray-100 text-gray-800';
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Unknown date';
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now - date);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays} days ago`;
    if (diffDays < 30) return `${Math.floor(diffDays / 7)} weeks ago`;
    return date.toLocaleDateString();
  };

  const isRecentJob = (dateString) => {
    if (!dateString) return false;
    const date = new Date(dateString);
    const now = new Date();
    const diffTime = Math.abs(now - date);
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays <= 7;
  };

  const renderStars = (rating, size = 'sm') => {
    const stars = [];
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 >= 0.5;
    
    for (let i = 0; i < 5; i++) {
      if (i < fullStars) {
        stars.push(<Star key={i} className={`h-${size === 'sm' ? '3' : '4'} w-${size === 'sm' ? '3' : '4'} fill-yellow-400 text-yellow-400`} />);
      } else if (i === fullStars && hasHalfStar) {
        stars.push(<Star key={i} className={`h-${size === 'sm' ? '3' : '4'} w-${size === 'sm' ? '3' : '4'} fill-yellow-400 text-yellow-400`} />);
      } else {
        stars.push(<Star key={i} className={`h-${size === 'sm' ? '3' : '4'} w-${size === 'sm' ? '3' : '4'} text-gray-300`} />);
      }
    }
    
    return stars;
  };

  if (isLoading) {
    return (
      <div className="max-w-7xl mx-auto py-8 px-4">
        <div className="animate-pulse">
          <div className="h-8 bg-gray-200 rounded w-1/4 mb-8"></div>
          <div className="space-y-4">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-32 bg-gray-200 rounded"></div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  const displayJobs = filteredJobs;

  return (
    <div className="max-w-7xl mx-auto py-8 px-4">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-800 mb-2">
          {showRecommended && recommendedJobs.length > 0 ? 'Recommended Jobs For You' : 'All Jobs'}
        </h1>
        <p className="text-gray-600">
          {showRecommended && recommendedJobs.length > 0 
            ? 'Jobs that match your skills and experience' 
            : `Discover ${filteredJobs.length} jobs that match your skills and aspirations`}
        </p>
      </div>

      {/* Toggle between All Jobs and Recommended */}
      {recommendedJobs.length > 0 && (
        <div className="flex space-x-4 mb-6">
          <button
            onClick={() => setShowRecommended(false)}
            className={`px-4 py-2 rounded-lg font-semibold transition-colors ${
              !showRecommended 
                ? 'bg-blue-600 text-white' 
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            All Jobs ({jobs.length})
          </button>
          <button
            onClick={() => setShowRecommended(true)}
            className={`px-4 py-2 rounded-lg font-semibold transition-colors flex items-center ${
              showRecommended 
                ? 'bg-green-600 text-white' 
                : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
            }`}
          >
            <TrendingUp className="h-4 w-4 mr-2" />
            Recommended for You ({recommendedJobs.length})
          </button>
        </div>
      )}

      {/* User Skills Display */}
      {showRecommended && userSkills.length > 0 && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
          <h3 className="font-semibold text-blue-800 mb-2">Your Skills:</h3>
          <div className="flex flex-wrap gap-2">
            {userSkills.map((skill, index) => (
              <span key={index} className="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm">
                {skill}
              </span>
            ))}
          </div>
        </div>
      )}

      {/* Search and Sort Bar */}
      <div className="bg-white rounded-lg shadow-lg p-6 mb-8">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between space-y-4 lg:space-y-0">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 h-5 w-5" />
            <input
              type="text"
              placeholder="Search jobs by title, company, or keyword..."
              className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
          
          <div className="flex space-x-4">
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="date">Sort by Date</option>
              <option value="rating">Sort by Rating</option>
              <option value="relevance">Sort by Relevance</option>
            </select>
            
            <button className="bg-blue-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-blue-700 transition-colors">
              Search
            </button>
          </div>
        </div>
      </div>

      {error && (
        <div className="bg-yellow-100 border border-yellow-400 text-yellow-700 px-4 py-3 rounded mb-4">
          {error}
        </div>
      )}

      <div className="grid lg:grid-cols-3 gap-8">
        {/* Jobs List */}
        <div className="lg:col-span-2 space-y-4">
          {displayJobs.map((job) => (
            <div
              key={job.jobId || job.id}
              className="bg-white rounded-lg shadow-lg border border-gray-200 p-6 hover:shadow-xl transition-shadow cursor-pointer"
              onClick={() => setSelectedJob(job)}
            >
              <div className="flex justify-between items-start mb-4">
                <div className="flex-1">
                  <div className="flex items-center space-x-2 mb-1">
                    <h3 className="text-xl font-semibold text-gray-800">{job.title}</h3>
                    {job.matchScore && (
                      <span className={`px-2 py-1 rounded-full text-xs font-semibold ${getMatchColor(job.matchScore)}`}>
                        {job.matchScore}% Match
                      </span>
                    )}
                    {isRecentJob(job.postedDate) && (
                      <span className="px-2 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-800 flex items-center">
                        <Clock className="h-3 w-3 mr-1" />
                        New
                      </span>
                    )}
                  </div>
                  <p className="text-lg text-gray-600 mb-2">{job.company}</p>
                </div>
                <div className="flex space-x-2">
                  <button className="p-2 text-gray-400 hover:text-blue-600 transition-colors">
                    <Bookmark className="h-5 w-5" />
                  </button>
                  <button className="p-2 text-gray-400 hover:text-green-600 transition-colors">
                    <Share2 className="h-5 w-5" />
                  </button>
                </div>
              </div>

              <div className="flex flex-wrap gap-4 mb-4 text-sm text-gray-600">
                <div className="flex items-center">
                  <MapPin className="h-4 w-4 mr-1" />
                  {job.location}
                </div>
                <div className="flex items-center">
                  <DollarSign className="h-4 w-4 mr-1" />
                  {job.salaryRange}
                </div>
                <div className="flex items-center">
                  <Calendar className="h-4 w-4 mr-1" />
                  {job.jobType}
                </div>
                <div className="flex items-center">
                  <Clock className="h-4 w-4 mr-1" />
                  {formatDate(job.postedDate)}
                </div>
              </div>

              {/* Rating Section */}
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center space-x-2">
                  <div className="flex items-center space-x-1">
                    {renderStars(job.averageRating)}
                  </div>
                                    <span className="text-sm text-gray-600">
                    ({job.totalRatings || 0} ratings)
                  </span>
                </div>
                {currentUser && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      rateJob(job);
                    }}
                    className="text-sm text-blue-600 hover:text-blue-800 font-medium"
                  >
                    {userRatings[job.id] ? 'Update Rating' : 'Rate Job'}
                  </button>
                )}
              </div>

              <p className="text-gray-700 mb-4 line-clamp-2">
                {job.description?.substring(0, 200)}...
              </p>

              {/* Matched Skills */}
              {job.matchedSkills && job.matchedSkills.length > 0 && (
                <div className="mb-3">
                  <p className="text-sm text-green-600 font-semibold mb-2">✓ Matched Skills:</p>
                  <div className="flex flex-wrap gap-2">
                    {job.matchedSkills.slice(0, 5).map((skill, index) => (
                      <span key={index} className="bg-green-100 text-green-800 px-2 py-1 rounded text-xs">
                        {skill}
                      </span>
                    ))}
                    {job.matchedSkills.length > 5 && (
                      <span className="bg-gray-100 text-gray-600 px-2 py-1 rounded text-xs">
                        +{job.matchedSkills.length - 5} more
                      </span>
                    )}
                  </div>
                </div>
              )}

              {/* Missing Skills */}
              {job.missingSkills && job.missingSkills.length > 0 && (
                <div className="mb-3">
                  <p className="text-sm text-yellow-600 font-semibold mb-2">Skills to Learn:</p>
                  <div className="flex flex-wrap gap-2">
                    {job.missingSkills.slice(0, 3).map((skill, index) => (
                      <span key={index} className="bg-yellow-100 text-yellow-800 px-2 py-1 rounded text-xs">
                        {skill}
                      </span>
                    ))}
                    {job.missingSkills.length > 3 && (
                      <span className="bg-gray-100 text-gray-600 px-2 py-1 rounded text-xs">
                        +{job.missingSkills.length - 3} more
                      </span>
                    )}
                  </div>
                </div>
              )}

              <div className="flex justify-between items-center">
                <div className="flex flex-wrap gap-2">
                  {parseSkills(job.requiredSkills).slice(0, 3).map((skill, index) => (
                    <span
                      key={index}
                      className="bg-blue-100 text-blue-800 px-2 py-1 rounded text-xs"
                    >
                      {skill}
                    </span>
                  ))}
                  {parseSkills(job.requiredSkills).length > 3 && (
                    <span className="bg-gray-100 text-gray-600 px-2 py-1 rounded text-xs">
                      +{parseSkills(job.requiredSkills).length - 3} more
                    </span>
                  )}
                </div>
                <div className="flex space-x-2">
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      rateJob(job);
                    }}
                    className="bg-yellow-600 text-white px-3 py-2 rounded-lg text-sm hover:bg-yellow-700 transition-colors flex items-center"
                  >
                    <Star className="h-4 w-4 mr-1" />
                    Rate
                  </button>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      applyForJob(job);
                    }}
                    className="bg-green-600 text-white px-4 py-2 rounded-lg text-sm hover:bg-green-700 transition-colors"
                  >
                    Apply Now
                  </button>
                </div>
              </div>
            </div>
          ))}

          {displayJobs.length === 0 && (
            <div className="text-center py-12">
              <p className="text-gray-500 text-lg">No jobs found matching your criteria.</p>
              {showRecommended && (
                <p className="text-gray-400 mt-2">
                  Try uploading your resume to get personalized recommendations.
                </p>
              )}
            </div>
          )}
        </div>

        {/* Job Details Sidebar */}
        {selectedJob && (
          <div className="bg-white rounded-lg shadow-lg border border-gray-200 p-6 sticky top-8">
            {selectedJob.matchScore && (
              <div className="mb-4 p-3 bg-blue-50 rounded-lg">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-blue-800">Match Score</span>
                  <span className={`px-3 py-1 rounded-full text-sm font-bold ${getMatchColor(selectedJob.matchScore)}`}>
                    {selectedJob.matchScore}%
                  </span>
                </div>
              </div>
            )}

            {/* Rating Section in Sidebar */}
            <div className="mb-4 p-3 bg-yellow-50 rounded-lg">
              <div className="flex items-center justify-between mb-2">
                <span className="font-semibold text-yellow-800">Job Rating</span>
                <div className="flex items-center space-x-2">
                  <div className="flex items-center space-x-1">
                    {renderStars(selectedJob.averageRating, 'md')}
                  </div>
                  <span className="text-sm text-yellow-700">
                    {selectedJob.averageRating ? selectedJob.averageRating.toFixed(1) : '0.0'} ({selectedJob.totalRatings || 0} ratings)
                  </span>
                </div>
              </div>
              {currentUser && (
                <button
                  onClick={() => rateJob(selectedJob)}
                  className="w-full bg-yellow-600 text-white py-2 rounded-lg font-semibold hover:bg-yellow-700 transition-colors flex items-center justify-center"
                >
                  <Star className="h-4 w-4 mr-2" />
                  {userRatings[selectedJob.id] ? 'Update Your Rating' : 'Rate This Job'}
                </button>
              )}
            </div>

            <h2 className="text-2xl font-bold text-gray-800 mb-2">{selectedJob.title}</h2>
            <p className="text-xl text-gray-600 mb-4">{selectedJob.company}</p>

            <div className="space-y-3 mb-6">
              <div className="flex items-center text-gray-600">
                <MapPin className="h-5 w-5 mr-3" />
                <span>{selectedJob.location}</span>
              </div>
              <div className="flex items-center text-gray-600">
                <DollarSign className="h-5 w-5 mr-3" />
                <span>{selectedJob.salaryRange}</span>
              </div>
              <div className="flex items-center text-gray-600">
                <Calendar className="h-5 w-5 mr-3" />
                <span>{selectedJob.jobType} • {selectedJob.experienceLevel}</span>
              </div>
              <div className="flex items-center text-gray-600">
                <Clock className="h-5 w-5 mr-3" />
                <span>Posted {formatDate(selectedJob.postedDate)}</span>
              </div>
            </div>

            <div className="mb-6">
              <h3 className="font-semibold text-gray-800 mb-3">Required Skills</h3>
              <div className="flex flex-wrap gap-2">
                {parseSkills(selectedJob.requiredSkills).map((skill, index) => (
                  <span
                    key={index}
                    className="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm"
                  >
                    {skill}
                  </span>
                ))}
              </div>
            </div>

            {selectedJob.preferredSkills && parseSkills(selectedJob.preferredSkills).length > 0 && (
              <div className="mb-6">
                <h3 className="font-semibold text-gray-800 mb-3">Preferred Skills</h3>
                <div className="flex flex-wrap gap-2">
                  {parseSkills(selectedJob.preferredSkills).map((skill, index) => (
                    <span
                      key={index}
                      className="bg-green-100 text-green-800 px-3 py-1 rounded-full text-sm"
                    >
                      {skill}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Matched Skills Section */}
            {selectedJob.matchedSkills && selectedJob.matchedSkills.length > 0 && (
              <div className="mb-6">
                <h3 className="font-semibold text-green-700 mb-3 flex items-center">
                  <Star className="h-4 w-4 mr-2" />
                  Your Matched Skills
                </h3>
                <div className="flex flex-wrap gap-2">
                  {selectedJob.matchedSkills.map((skill, index) => (
                    <span
                      key={index}
                      className="bg-green-100 text-green-800 px-3 py-1 rounded-full text-sm"
                    >
                      {skill}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Missing Skills Section */}
            {selectedJob.missingSkills && selectedJob.missingSkills.length > 0 && (
              <div className="mb-6">
                <h3 className="font-semibold text-yellow-700 mb-3">Skills to Improve</h3>
                <div className="flex flex-wrap gap-2">
                  {selectedJob.missingSkills.map((skill, index) => (
                    <span
                      key={index}
                      className="bg-yellow-100 text-yellow-800 px-3 py-1 rounded-full text-sm"
                    >
                      {skill}
                    </span>
                  ))}
                </div>
              </div>
            )}

            <div className="mb-6">
              <h3 className="font-semibold text-gray-800 mb-3">Job Description</h3>
              <p className="text-gray-700 whitespace-pre-line">{selectedJob.description}</p>
            </div>

            <div className="space-y-3">
              <button
                onClick={() => applyForJob(selectedJob)}
                className="w-full bg-green-600 text-white py-3 rounded-lg font-semibold hover:bg-green-700 transition-colors"
              >
                Apply for This Job
              </button>
              
              <button
                onClick={() => rateJob(selectedJob)}
                className="w-full bg-yellow-600 text-white py-3 rounded-lg font-semibold hover:bg-yellow-700 transition-colors flex items-center justify-center"
              >
                <Star className="h-4 w-4 mr-2" />
                Rate This Job
              </button>
              
              <button className="w-full border border-gray-300 text-gray-700 py-3 rounded-lg font-semibold hover:bg-gray-50 transition-colors">
                Save for Later
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Application Modal */}
      {showApplicationModal && selectedJobForApplication && (
        <JobApplicationModal
          job={selectedJobForApplication}
          onClose={() => {
            setShowApplicationModal(false);
            setSelectedJobForApplication(null);
          }}
          onSuccess={handleApplicationSuccess}
        />
      )}

      {/* Rating Modal */}
      {showRatingModal && selectedJob && (
        <JobRatingModal
          job={selectedJob}
          userRating={userRatings[selectedJob.id]}
          onClose={() => {
            setShowRatingModal(false);
            setSelectedJob(null);
          }}
          onSuccess={handleRatingSuccess}
        />
      )}
    </div>
  );
};

export default Jobs;