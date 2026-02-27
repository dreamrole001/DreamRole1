import React, { useState, useEffect } from 'react';
import { X, Star } from 'lucide-react';

const JobPostForm = ({ job, onSubmit, onCancel }) => {
  const [formData, setFormData] = useState({
    title: '',
    company: '',
    description: '',
    requiredSkills: '',
    preferredSkills: '',
    location: '',
    salaryRange: '',
    jobType: '',
    experienceLevel: ''
  });

  // FIXED: Proper useEffect to set form data when job prop changes
  useEffect(() => {
    if (job) {
      setFormData({
        title: job.title || '',
        company: job.company || '',
        description: job.description || '',
        requiredSkills: job.requiredSkills || '',
        preferredSkills: job.preferredSkills || '',
        location: job.location || '',
        salaryRange: job.salaryRange || '',
        jobType: job.jobType || '',
        experienceLevel: job.experienceLevel || ''
      });
    } else {
      // Reset form when creating new job
      setFormData({
        title: '',
        company: '',
        description: '',
        requiredSkills: '',
        preferredSkills: '',
        location: '',
        salaryRange: '',
        jobType: '',
        experienceLevel: ''
      });
    }
  }, [job]);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    // Validate required fields
    if (!formData.title || !formData.company || !formData.description || 
        !formData.location || !formData.jobType || !formData.experienceLevel) {
      alert('Please fill in all required fields');
      return;
    }
    
    // Validate skills format
    try {
      if (formData.requiredSkills) {
        JSON.parse(formData.requiredSkills);
      }
      if (formData.preferredSkills) {
        JSON.parse(formData.preferredSkills);
      }
    } catch (error) {
      alert('Skills must be in valid JSON format: ["skill1", "skill2"]');
      return;
    }
    
    onSubmit(formData);
  };

  const jobTypes = ['Full-time', 'Part-time', 'Contract', 'Internship', 'Remote'];
  const experienceLevels = ['Entry', 'Junior', 'Mid-level', 'Senior', 'Lead', 'Principal'];

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-4xl shadow-lg rounded-md bg-white">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">
            {job ? 'Edit Job Posting' : 'Post New Job'}
          </h2>
          <button
            onClick={onCancel}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        {/* New Feature Notice */}
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
          <h4 className="font-semibold text-yellow-800 mb-2 flex items-center">
            <Star className="h-4 w-4 mr-2" />
            New Feature: Job Ratings
          </h4>
          <p className="text-sm text-yellow-700">
            Job seekers can now rate your job postings. This helps improve visibility and provides valuable feedback.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
            <div>
              <label htmlFor="title" className="block text-sm font-medium text-gray-700">
                Job Title *
              </label>
              <input
                type="text"
                name="title"
                id="title"
                required
                value={formData.title}
                onChange={handleChange}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                placeholder="e.g., Senior Frontend Developer"
              />
            </div>

            <div>
              <label htmlFor="company" className="block text-sm font-medium text-gray-700">
                Company *
              </label>
              <input
                type="text"
                name="company"
                id="company"
                required
                value={formData.company}
                onChange={handleChange}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                placeholder="Company name"
              />
            </div>
          </div>

          <div>
            <label htmlFor="description" className="block text-sm font-medium text-gray-700">
              Job Description *
            </label>
            <textarea
              name="description"
              id="description"
              rows={4}
              required
              value={formData.description}
              onChange={handleChange}
              className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              placeholder="Detailed job description, responsibilities, and requirements..."
            />
          </div>

          <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
            <div>
              <label htmlFor="requiredSkills" className="block text-sm font-medium text-gray-700">
                Required Skills (JSON array) *
              </label>
              <textarea
                name="requiredSkills"
                id="requiredSkills"
                rows={3}
                required
                value={formData.requiredSkills}
                onChange={handleChange}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                placeholder='["JavaScript", "React", "Node.js"]'
              />
              <p className="mt-1 text-sm text-gray-500">Enter skills as a JSON array</p>
            </div>

            <div>
              <label htmlFor="preferredSkills" className="block text-sm font-medium text-gray-700">
                Preferred Skills (JSON array)
              </label>
              <textarea
                name="preferredSkills"
                id="preferredSkills"
                rows={3}
                value={formData.preferredSkills}
                onChange={handleChange}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                placeholder='["TypeScript", "AWS", "Docker"]'
              />
              <p className="mt-1 text-sm text-gray-500">Enter skills as a JSON array</p>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
            <div>
              <label htmlFor="location" className="block text-sm font-medium text-gray-700">
                Location *
              </label>
              <input
                type="text"
                name="location"
                id="location"
                required
                value={formData.location}
                onChange={handleChange}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                placeholder="e.g., Remote, New York, NY"
              />
            </div>

            <div>
              <label htmlFor="salaryRange" className="block text-sm font-medium text-gray-700">
                Salary Range
              </label>
              <input
                type="text"
                name="salaryRange"
                id="salaryRange"
                value={formData.salaryRange}
                onChange={handleChange}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                placeholder="e.g., $80,000 - $120,000"
              />
            </div>

            <div>
              <label htmlFor="jobType" className="block text-sm font-medium text-gray-700">
                Job Type *
              </label>
              <select
                name="jobType"
                id="jobType"
                required
                value={formData.jobType}
                onChange={handleChange}
                className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              >
                <option value="">Select job type</option>
                {jobTypes.map(type => (
                  <option key={type} value={type}>{type}</option>
                ))}
              </select>
            </div>
          </div>

          <div>
            <label htmlFor="experienceLevel" className="block text-sm font-medium text-gray-700">
              Experience Level *
            </label>
            <select
              name="experienceLevel"
              id="experienceLevel"
              required
              value={formData.experienceLevel}
              onChange={handleChange}
              className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
            >
              <option value="">Select experience level</option>
              {experienceLevels.map(level => (
                <option key={level} value={level}>{level}</option>
              ))}
            </select>
          </div>

          <div className="flex justify-end space-x-4 pt-6 border-t border-gray-200">
            <button
              type="button"
              onClick={onCancel}
              className="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
            >
              {job ? 'Update Job' : 'Post Job'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default JobPostForm;