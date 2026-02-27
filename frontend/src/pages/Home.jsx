import React from 'react';
import { Link } from 'react-router-dom';
import { Upload, Search, TrendingUp, Users, Building } from 'lucide-react';

const Home = () => {
  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="bg-gradient-to-br from-blue-600 to-purple-700 text-white">
        <div className="max-w-7xl mx-auto px-4 py-20">
          <div className="text-center">
            <h1 className="text-5xl font-bold mb-6">
              Find Your Dream Job with AI Power
            </h1>
            <p className="text-xl mb-8 text-blue-100 max-w-2xl mx-auto">
              Upload your resume and let our AI match you with perfect job opportunities. 
              Get personalized recommendations and accelerate your career journey.
            </p>
            <div className="flex justify-center space-x-4">
              <Link
                to="/upload"
                className="bg-white text-blue-600 px-8 py-3 rounded-lg font-semibold hover:bg-blue-50 transition-colors flex items-center"
              >
                <Upload className="h-5 w-5 mr-2" />
                Upload Resume
              </Link>
              <Link
                to="/jobs"
                className="border-2 border-white text-white px-8 py-3 rounded-lg font-semibold hover:bg-white hover:text-blue-600 transition-colors flex items-center"
              >
                <Search className="h-5 w-5 mr-2" />
                Browse Jobs
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4">
          <h2 className="text-3xl font-bold text-center mb-12 text-gray-800">
            How JobEra Works
          </h2>
          <div className="grid md:grid-cols-3 gap-8">
            <div className="text-center p-6">
              <div className="bg-blue-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Upload className="h-8 w-8 text-blue-600" />
              </div>
              <h3 className="text-xl font-semibold mb-3">Upload Your Resume</h3>
              <p className="text-gray-600">
                Upload your resume in any format. Our AI will extract your skills, 
                experience, and education automatically.
              </p>
            </div>
            
            <div className="text-center p-6">
              <div className="bg-green-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <TrendingUp className="h-8 w-8 text-green-600" />
              </div>
              <h3 className="text-xl font-semibold mb-3">AI-Powered Matching</h3>
              <p className="text-gray-600">
                Our advanced algorithm analyzes your profile and matches you with 
                the most relevant job opportunities.
              </p>
            </div>
            
            <div className="text-center p-6">
              <div className="bg-purple-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <Users className="h-8 w-8 text-purple-600" />
              </div>
              <h3 className="text-xl font-semibold mb-3">Get Hired Faster</h3>
              <p className="text-gray-600">
                Apply to recommended jobs directly through our platform and 
                track your applications in one place.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Recruiter Section */}
      <section className="py-16 bg-gradient-to-r from-green-500 to-emerald-600 text-white">
        <div className="max-w-7xl mx-auto px-4">
          <div className="text-center">
            <Building className="h-16 w-16 mx-auto mb-4" />
            <h2 className="text-3xl font-bold mb-4">Are You a Recruiter?</h2>
            <p className="text-xl mb-8 text-green-100 max-w-2xl mx-auto">
              Post jobs, find qualified candidates, and streamline your hiring process with our AI-powered platform.
            </p>
            <div className="flex justify-center space-x-4">
              <Link
                to="/recruiter/register"
                className="bg-white text-green-600 px-8 py-3 rounded-lg font-semibold hover:bg-green-50 transition-colors flex items-center"
              >
                <Building className="h-5 w-5 mr-2" />
                Register as Recruiter
              </Link>
              <Link
                to="/jobs"
                className="border-2 border-white text-white px-8 py-3 rounded-lg font-semibold hover:bg-white hover:text-green-600 transition-colors"
              >
                Browse Talent
              </Link>
            </div>
          </div>
        </div>
      </section>

      {/* Stats Section */}
      <section className="py-16 bg-gray-50">
        <div className="max-w-7xl mx-auto px-4">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
            <div>
              <div className="text-3xl font-bold text-blue-600 mb-2">10K+</div>
              <div className="text-gray-600">Job Postings</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-green-600 mb-2">5K+</div>
              <div className="text-gray-600">Companies</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-purple-600 mb-2">50K+</div>
              <div className="text-gray-600">Users</div>
            </div>
            <div>
              <div className="text-3xl font-bold text-orange-600 mb-2">15K+</div>
              <div className="text-gray-600">Successful Hires</div>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Home;