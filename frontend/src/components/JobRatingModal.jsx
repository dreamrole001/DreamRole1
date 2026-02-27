import React, { useState, useEffect } from 'react';
import { X, Star, Send } from 'lucide-react';
import api from '../services/api';

const JobRatingModal = ({ job, userRating, onClose, onSuccess }) => {
  const [rating, setRating] = useState(userRating?.rating || 0);
  const [comment, setComment] = useState(userRating?.comment || '');
  const [hoverRating, setHoverRating] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState('');

  const currentUser = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    if (userRating) {
      setRating(userRating.rating);
      setComment(userRating.comment || '');
    }
  }, [userRating]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (rating === 0) {
      setError('Please select a rating');
      return;
    }

    if (!currentUser) {
      setError('You must be logged in to rate jobs');
      return;
    }

    setIsSubmitting(true);
    setError('');

    try {
      await api.post(`/jobs/${job.id}/rate/${currentUser.id}`, {
        rating: rating,
        comment: comment
      });

      onSuccess();
    } catch (error) {
      console.error('Error rating job:', error);
      setError(error.response?.data?.error || 'Failed to submit rating. Please try again.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const renderStars = (forHover = false) => {
    const stars = [];
    const currentRating = forHover ? hoverRating : rating;

    for (let i = 1; i <= 5; i++) {
      stars.push(
        <button
          key={i}
          type="button"
          className={`p-1 transition-transform hover:scale-110 ${
            i <= currentRating ? 'text-yellow-400' : 'text-gray-300'
          }`}
          onClick={() => setRating(i)}
          onMouseEnter={() => setHoverRating(i)}
          onMouseLeave={() => setHoverRating(0)}
        >
          <Star 
            className="h-8 w-8" 
            fill={i <= currentRating ? 'currentColor' : 'none'}
          />
        </button>
      );
    }
    return stars;
  };

  const getRatingText = (ratingValue) => {
    const texts = {
      1: 'Poor',
      2: 'Fair',
      3: 'Good',
      4: 'Very Good',
      5: 'Excellent'
    };
    return texts[ratingValue] || 'Select Rating';
  };

  return (
    <div className="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">
      <div className="relative top-20 mx-auto p-5 border w-full max-w-md shadow-lg rounded-md bg-white">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">
            Rate This Job
          </h2>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
          >
            <X className="h-6 w-6" />
          </button>
        </div>

        {/* Job Info */}
        <div className="bg-gray-50 rounded-lg p-4 mb-6">
          <h3 className="font-semibold text-gray-800 text-lg">{job.title}</h3>
          <p className="text-gray-600">{job.company}</p>
          <p className="text-sm text-gray-500 mt-1">{job.location}</p>
        </div>

        {error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Rating Stars */}
          <div className="text-center">
            <div 
              className="flex justify-center space-x-2 mb-2"
              onMouseLeave={() => setHoverRating(0)}
            >
              {renderStars()}
            </div>
            <p className="text-lg font-semibold text-gray-700">
              {getRatingText(hoverRating || rating)}
            </p>
            {userRating && (
              <p className="text-sm text-gray-500 mt-1">
                You rated this job {userRating.rating} stars
              </p>
            )}
          </div>

          {/* Comment */}
          <div>
            <label htmlFor="comment" className="block text-sm font-medium text-gray-700 mb-2">
              Your Review {comment && `(${comment.length}/500)`}
            </label>
            <textarea
              id="comment"
              rows={4}
              value={comment}
              onChange={(e) => setComment(e.target.value.slice(0, 500))}
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-yellow-500 focus:border-transparent"
              placeholder="Share your thoughts about this job opportunity... (optional)"
            />
            <p className="text-xs text-gray-500 mt-1">
              Maximum 500 characters
            </p>
          </div>

          {/* Rating Guidelines */}
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <h4 className="font-semibold text-blue-800 mb-2">Rating Guidelines:</h4>
            <ul className="text-sm text-blue-700 space-y-1">
              <li>⭐️⭐️⭐️⭐️⭐️ - Excellent opportunity, great company</li>
              <li>⭐️⭐️⭐️⭐️ - Very good role and requirements</li>
              <li>⭐️⭐️⭐️ - Good job posting, clear description</li>
              <li>⭐️⭐️ - Fair opportunity, needs more details</li>
              <li>⭐️ - Poor description or unclear requirements</li>
            </ul>
          </div>

          {/* Submit Buttons */}
          <div className="flex space-x-4 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-yellow-500"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || rating === 0}
              className="flex-1 flex justify-center items-center px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-yellow-600 hover:bg-yellow-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-yellow-500 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSubmitting ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2"></div>
                  Submitting...
                </>
              ) : (
                <>
                  <Send className="h-4 w-4 mr-2" />
                  {userRating ? 'Update Rating' : 'Submit Rating'}
                </>
              )}
            </button>
          </div>
        </form>

        {/* Privacy Note */}
        <div className="mt-4 p-3 bg-gray-100 rounded-lg">
          <p className="text-xs text-gray-600 text-center">
            Your rating will be visible to other job seekers and help improve the quality of job postings.
            Your personal information will not be shared.
          </p>
        </div>
      </div>
    </div>
  );
};

export default JobRatingModal;