import React, { useState, useEffect } from 'react';
import { X, Upload, FileText, CheckCircle, Loader, Plus, X as XIcon } from 'lucide-react';
import api from '../services/api';

const JobApplicationModal = ({ job, onClose, onSuccess }) => {
  const [formData, setFormData] = useState({
    coverLetter: 'I am very interested in this position and believe my skills are a great match.',
    skills: [],
    experience: '',
    education: ''
  });
  const [newSkill, setNewSkill] = useState('');
  const [resumeFile, setResumeFile] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoadingResume, setIsLoadingResume] = useState(true);
  const [error, setError] = useState('');
  const [jobId, setJobId] = useState('');
  const [hasResume, setHasResume] = useState(false);
  const [fetchAttempted, setFetchAttempted] = useState(false);

  const currentUser = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    console.log('Job object received:', job);
    const possibleJobId = job?.id || job?.jobId || job?.ID;
    if (possibleJobId) {
      setJobId(possibleJobId);
      fetchUserLatestResume();
    } else {
      setError('Job ID is missing. Cannot submit application.');
      setIsLoadingResume(false);
      setFetchAttempted(true);
    }
  }, [job]);

  // Fetch user's latest resume data
  const fetchUserLatestResume = async () => {
    if (!currentUser || !currentUser.id) {
      console.log('No current user found');
      setIsLoadingResume(false);
      setFetchAttempted(true);
      return;
    }

    try {
      setIsLoadingResume(true);
      console.log('Fetching latest resume for user:', currentUser.id);
      
      const response = await api.get(`/resumes/user/${currentUser.id}/latest`);
      console.log('Resume API response:', response);
      
      if (response.data && response.data.id) {
        const resumeData = response.data;
        console.log('✅ Resume data received:', resumeData);
        
        // Parse skills from JSON
        let parsedSkills = [];
        if (resumeData.skills) {
          try {
            console.log('Raw skills data:', resumeData.skills);
            
            if (typeof resumeData.skills === 'string') {
              try {
                parsedSkills = JSON.parse(resumeData.skills);
                console.log('Parsed from JSON string:', parsedSkills);
              } catch (jsonError) {
                console.log('JSON parse failed, trying text extraction');
                const skillsText = resumeData.skills.replace(/[\[\]"]/g, '');
                parsedSkills = skillsText.split(',').map(s => s.trim()).filter(s => s);
                console.log('Extracted from text:', parsedSkills);
              }
            } else if (Array.isArray(resumeData.skills)) {
              parsedSkills = resumeData.skills;
              console.log('Skills already array:', parsedSkills);
            }
            
            if (Array.isArray(parsedSkills)) {
              parsedSkills = parsedSkills
                .filter(skill => skill && typeof skill === 'string')
                .map(skill => skill.trim());
            }
          } catch (e) {
            console.error('Error processing skills:', e);
            parsedSkills = [];
          }
        }
        
        console.log(`✅ Found ${parsedSkills.length} skills from resume:`, parsedSkills);
        
        // Get experience and education
        let experienceValue = resumeData.experienceYears;
        if (experienceValue === null || experienceValue === undefined) {
          experienceValue = 0;
        }
        
        let educationValue = resumeData.educationLevel;
        if (!educationValue) {
          educationValue = "Bachelor's Degree";
        }
        
        console.log('Experience from resume:', experienceValue);
        console.log('Education from resume:', educationValue);
        
        setFormData({
          coverLetter: formData.coverLetter,
          skills: parsedSkills.length > 0 ? parsedSkills : ['JavaScript', 'React', 'Node.js'],
          experience: experienceValue.toString(),
          education: educationValue
        });
        
        setHasResume(parsedSkills.length > 0);
        console.log('✅ Form populated with resume data successfully');
      } else {
        console.log('No resume data found, using defaults');
        setDefaultFormData();
      }
    } catch (error) {
      console.error('❌ Error fetching resume:', error);
      console.log('Error details:', error.response?.data || error.message);
      
      // Check if it's a 404 (no resume found) or 500 (server error)
      if (error.response?.status === 404) {
        console.log('No resume found for user (404)');
        setDefaultFormData();
      } else if (error.response?.status === 500) {
        console.log('Server error when fetching resume');
        setDefaultFormData();
        setError('Server error when loading resume. Using default values.');
      } else {
        setDefaultFormData();
      }
    } finally {
      setIsLoadingResume(false);
      setFetchAttempted(true);
    }
  };

  const setDefaultFormData = () => {
    setFormData({
      coverLetter: formData.coverLetter,
      skills: ['JavaScript', 'React', 'Node.js'],
      experience: '2',
      education: "Bachelor's Degree"
    });
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleAddSkill = () => {
    if (newSkill.trim()) {
      const skillToAdd = newSkill.trim();
      if (!formData.skills.includes(skillToAdd)) {
        setFormData({
          ...formData,
          skills: [...formData.skills, skillToAdd]
        });
        setNewSkill('');
      }
    }
  };

  const handleRemoveSkill = (skillToRemove) => {
    setFormData({
      ...formData,
      skills: formData.skills.filter(skill => skill !== skillToRemove)
    });
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && newSkill.trim()) {
      e.preventDefault();
      handleAddSkill();
    }
  };

  const handleResumeUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    
    setResumeFile(file);
    
    try {
      setIsLoadingResume(true);
      console.log('Uploading new resume:', file.name);
      
      const formData = new FormData();
      formData.append('file', file);
      
      const uploadResponse = await api.post(`/resumes/upload/${currentUser.id}`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      console.log('Upload response:', uploadResponse);
      
      if (uploadResponse.data) {
        alert('Resume uploaded successfully! Fetching updated data...');
        
        // Wait a moment for processing
        setTimeout(async () => {
          try {
            const resumeResponse = await api.get(`/resumes/user/${currentUser.id}/latest`);
            console.log('Updated resume data:', resumeResponse);
            
            if (resumeResponse.data && resumeResponse.data.id) {
              const resumeData = resumeResponse.data;
              
              // Parse skills
              let parsedSkills = [];
              if (resumeData.skills) {
                try {
                  if (typeof resumeData.skills === 'string') {
                    parsedSkills = JSON.parse(resumeData.skills);
                  } else if (Array.isArray(resumeData.skills)) {
                    parsedSkills = resumeData.skills;
                  }
                } catch (e) {}
              }
              
              setFormData(prev => ({
                ...prev,
                skills: parsedSkills.length > 0 ? parsedSkills : prev.skills,
                experience: resumeData.experienceYears?.toString() || prev.experience,
                education: resumeData.educationLevel || prev.education
              }));
              
              setHasResume(true);
              alert(`Resume processed! Found ${parsedSkills.length} skills.`);
            }
          } catch (fetchError) {
            console.error('Error fetching updated resume:', fetchError);
            alert('Resume uploaded but couldn\'t fetch updated data. Please refresh the page.');
          } finally {
            setIsLoadingResume(false);
          }
        }, 2000);
      }
    } catch (error) {
      console.error('Error uploading resume:', error);
      setError('Failed to upload resume. Please try again.');
      setResumeFile(null);
      setIsLoadingResume(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!jobId) {
      setError('Job ID is missing. Cannot submit application.');
      return;
    }

    if (formData.skills.length === 0) {
      setError('Please add at least one skill');
      return;
    }

    setIsSubmitting(true);
    setError('');

    try {
      console.log('=== SUBMITTING APPLICATION ===');
      console.log('Job ID:', jobId);
      console.log('User ID:', currentUser?.id);
      console.log('Skills:', formData.skills);
      console.log('Experience:', formData.experience);
      console.log('Education:', formData.education);

      const applicationData = {
        coverLetter: formData.coverLetter,
        applicantSkills: JSON.stringify(formData.skills),
        applicantExperience: parseInt(formData.experience) || 0,
        applicantEducation: formData.education,
        resumeFilePath: resumeFile ? `/uploads/resumes/${resumeFile.name}` : '',
        resumeParsedText: 'Resume content'
      };

      const response = await api.post(`/jobs/${jobId}/apply-with-resume/${currentUser.id}`, applicationData);
      
      console.log('✅ Application successful!', response.data);
      
      if (onSuccess) onSuccess();
      onClose();
      
    } catch (error) {
      console.error('❌ Application failed:', error);
      setError(error.response?.data?.error || 'Application failed. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  // Manual refresh function
  const handleManualRefresh = () => {
    setIsLoadingResume(true);
    fetchUserLatestResume();
  };

  if (!jobId) {
    return (
      <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
        <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-2xl font-bold text-gray-900">Application Error</h2>
            <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
              <X className="h-6 w-6" />
            </button>
          </div>
          <div className="bg-red-100 border border-red-400 text-red-700 p-4 rounded">
            <strong>Missing Job ID:</strong> Cannot submit application because the job ID is missing.
          </div>
          <div className="mt-4 flex justify-end">
            <button onClick={onClose} className="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 bg-white hover:bg-gray-50">
              Close
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-2xl shadow-lg rounded-md bg-white">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">Apply for {job.title}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            <X className="h-6 w-6" />
          </button>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
            {error}
          </div>
        )}

        {isLoadingResume && (
          <div className="mb-4 p-4 bg-blue-50 border border-blue-200 rounded-lg flex items-center">
            <Loader className="h-5 w-5 animate-spin text-blue-600 mr-3" />
            <span className="text-blue-700">Loading your resume data...</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Resume Upload Section */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Upload Resume (Optional but Recommended)
            </label>
            <div className="border-2 border-dashed border-gray-300 rounded-lg p-6 text-center">
              <input
                type="file"
                id="resume-upload"
                className="hidden"
                accept=".pdf,.doc,.docx,.txt"
                onChange={handleResumeUpload}
                disabled={isLoadingResume}
              />
              <label htmlFor="resume-upload" className={`cursor-pointer ${isLoadingResume ? 'opacity-50' : ''}`}>
                <div className="flex flex-col items-center">
                  <Upload className="h-8 w-8 text-gray-400 mb-2" />
                  {resumeFile ? (
                    <div className="flex items-center space-x-2">
                      <FileText className="h-6 w-6 text-green-600" />
                      <div className="text-left">
                        <p className="font-semibold text-gray-800">{resumeFile.name}</p>
                        <p className="text-sm text-gray-500">
                          {(resumeFile.size / 1024 / 1024).toFixed(2)} MB
                        </p>
                      </div>
                    </div>
                  ) : (
                    <>
                      <p className="text-sm font-semibold text-gray-700">Click to upload resume</p>
                      <p className="text-xs text-gray-500 mt-1">PDF, DOC, DOCX, TXT (Max 10MB)</p>
                    </>
                  )}
                </div>
              </label>
            </div>
            {hasResume && !resumeFile && (
              <p className="text-sm text-green-600 mt-2 flex items-center">
                <CheckCircle className="h-4 w-4 mr-1" />
                Your resume data loaded: {formData.skills.length} skills found
              </p>
            )}
            {!hasResume && !isLoadingResume && fetchAttempted && (
              <p className="text-sm text-yellow-600 mt-2 flex items-center">
                No resume found. Using default skills.
              </p>
            )}
          </div>

          {/* Cover Letter */}
          <div>
            <label htmlFor="coverLetter" className="block text-sm font-medium text-gray-700">
              Cover Letter *
            </label>
            <textarea
              id="coverLetter"
              name="coverLetter"
              rows={4}
              required
              value={formData.coverLetter}
              onChange={handleChange}
              className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              placeholder="Why are you interested in this position?"
            />
          </div>

          {/* Skills Section */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Your Skills * ({formData.skills.length} skills found)
            </label>
            
            {/* Skills Tags */}
            <div className="flex flex-wrap gap-2 mb-3 min-h-[80px] p-3 bg-gray-50 rounded-lg border border-gray-200 max-h-48 overflow-y-auto">
              {formData.skills.map((skill, index) => (
                <span
                  key={index}
                  className="inline-flex items-center bg-blue-100 text-blue-800 px-3 py-1.5 rounded-full text-sm font-medium"
                >
                  {skill}
                  <button
                    type="button"
                    onClick={() => handleRemoveSkill(skill)}
                    className="ml-2 text-blue-600 hover:text-blue-800 focus:outline-none"
                  >
                    <XIcon className="h-3 w-3" />
                  </button>
                </span>
              ))}
              {formData.skills.length === 0 && (
                <p className="text-gray-500 text-sm w-full text-center py-2">
                  No skills added yet. Add your skills below.
                </p>
              )}
            </div>

            {/* Add New Skill */}
            <div className="flex space-x-2">
              <input
                type="text"
                value={newSkill}
                onChange={(e) => setNewSkill(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="Enter a skill (e.g., JavaScript)"
                className="flex-1 border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                disabled={isLoadingResume}
              />
              <button
                type="button"
                onClick={handleAddSkill}
                disabled={isLoadingResume || !newSkill.trim()}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <Plus className="h-5 w-5" />
              </button>
            </div>
            <p className="text-xs text-gray-500 mt-1">
              Add your skills one by one. Click + to add, or press Enter.
            </p>
          </div>

          {/* Experience and Education */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="experience" className="block text-sm font-medium text-gray-700">
                Years of Experience *
              </label>
              <input
                type="number"
                id="experience"
                name="experience"
                required
                value={formData.experience}
                onChange={handleChange}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                min="0"
                max="50"
                step="0.5"
                disabled={isLoadingResume}
              />
            </div>

            <div>
              <label htmlFor="education" className="block text-sm font-medium text-gray-700">
                Education Level *
              </label>
              <select
                id="education"
                name="education"
                required
                value={formData.education}
                onChange={handleChange}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                disabled={isLoadingResume}
              >
                <option value="">Select education</option>
                <option value="High School">High School</option>
                <option value="Associate Degree">Associate Degree</option>
                <option value="Bachelor's Degree">Bachelor's Degree</option>
                <option value="Master's Degree">Master's Degree</option>
                <option value="PhD">PhD</option>
              </select>
            </div>
          </div>

          {/* Data Source Info */}
          {hasResume && formData.skills.length > 0 && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-4">
              <h4 className="font-semibold text-green-800 mb-2 flex items-center">
                <CheckCircle className="h-4 w-4 mr-2" />
                Resume Data Loaded Successfully
              </h4>
              <p className="text-sm text-green-700">
                Your skills ({formData.skills.length} skills), experience ({formData.experience} years), 
                and education have been automatically extracted from your resume. You can edit them if needed.
              </p>
            </div>
          )}

          {/* Submit Buttons */}
          <div className="flex justify-end space-x-4 pt-6 border-t border-gray-200">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || isLoadingResume}
              className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
            >
              {isSubmitting ? (
                <>
                  <Loader className="h-4 w-4 animate-spin mr-2" />
                  Submitting...
                </>
              ) : (
                'Submit Application'
              )}
            </button>
          </div>
        </form>

        {/* Manual refresh button for testing */}
        <div className="mt-4 text-center">
          <button
            type="button"
            onClick={handleManualRefresh}
            className="text-xs text-blue-600 hover:text-blue-800"
          >
            ↻ Refresh Resume Data
          </button>
        </div>
      </div>
    </div>
  );
};

export default JobApplicationModal;