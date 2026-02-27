import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Briefcase, Upload, Home, User, LogOut, Building, BarChart3, Calendar } from 'lucide-react';
import authService from '../services/auth';

// Add the isAdmin function here
const isAdmin = (user) => user && user.role === 'ROLE_ADMIN';

const Navbar = ({ onAuthChange }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const currentUser = authService.getCurrentUser();

  const handleLogout = () => {
    authService.logout();
    onAuthChange(null); // Notify parent component
    navigate('/');
  };

  // Regular user nav items
  const userNavItems = [
    { path: '/', icon: Home, label: 'Home' },
    { path: '/dashboard', icon: User, label: 'Dashboard' },
    { path: '/jobs', icon: Briefcase, label: 'Jobs' },
    { path: '/upload', icon: Upload, label: 'Upload Resume' },
  ];

  // Admin nav items
  const adminNavItems = [
    { path: '/', icon: Home, label: 'Home' },
    { path: '/admin/dashboard', icon: BarChart3, label: 'Admin Dashboard' },
  ];

  // Recruiter nav items
  const recruiterNavItems = [
    { path: '/', icon: Home, label: 'Home' },
    { path: '/recruiter/dashboard', icon: Building, label: 'Recruiter Dashboard' },
    { path: '/recruiter/interviews', icon: Calendar, label: 'Interview Management' },
    { path: '/jobs', icon: Briefcase, label: 'Jobs' },
  ];

  // Determine which nav items to show based on user role
  const navItems = isAdmin(currentUser) ? adminNavItems : 
                  currentUser?.role === 'ROLE_RECRUITER' ? recruiterNavItems : userNavItems;

  return (
    <nav className="bg-white shadow-lg border-b">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex justify-between items-center h-16">
          <div className="flex items-center">
            <Briefcase className="h-8 w-8 text-blue-600" />
            <span className="ml-2 text-xl font-bold text-gray-800">DreamRole</span>
          </div>
          
          {currentUser ? (
            <div className="flex items-center space-x-8">
              <div className="flex space-x-6">
                {navItems.map((item) => {
                  const Icon = item.icon;
                  const isActive = location.pathname === item.path;
                  
                  return (
                    <Link
                      key={item.path}
                      to={item.path}
                      className={`flex items-center space-x-1 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                        isActive
                          ? 'text-blue-600 bg-blue-50'
                          : 'text-gray-600 hover:text-blue-600 hover:bg-gray-50'
                      }`}
                    >
                      <Icon className="h-4 w-4" />
                      <span>{item.label}</span>
                    </Link>
                  );
                })}
              </div>
              
              <div className="flex items-center space-x-4">
                <span className="text-sm text-gray-700">
                  Welcome, {currentUser.fullName}
                  {currentUser.role === 'ROLE_RECRUITER' && (
                    <span className="ml-2 px-2 py-1 bg-green-100 text-green-800 text-xs rounded-full">
                      Recruiter
                    </span>
                  )}
                  {isAdmin(currentUser) && (
                    <span className="ml-2 px-2 py-1 bg-red-100 text-red-800 text-xs rounded-full">
                      Admin
                    </span>
                  )}
                </span>
                <button
                  onClick={handleLogout}
                  className="flex items-center space-x-1 px-3 py-2 rounded-md text-sm font-medium text-gray-600 hover:text-red-600 hover:bg-gray-50 transition-colors"
                >
                  <LogOut className="h-4 w-4" />
                  <span>Logout</span>
                </button>
              </div>
            </div>
          ) : (
            <div className="flex items-center space-x-4">
              <Link
                to="/login"
                className="px-4 py-2 text-sm font-medium text-gray-600 hover:text-blue-600 transition-colors"
              >
                Sign In
              </Link>
              <div className="flex space-x-2">
                <Link
                  to="/register"
                  className="px-4 py-2 text-sm font-medium text-white bg-blue-600 rounded-md hover:bg-blue-700 transition-colors"
                >
                  Sign Up
                </Link>
                <Link
                  to="/recruiter/register"
                  className="px-4 py-2 text-sm font-medium text-white bg-green-600 rounded-md hover:bg-green-700 transition-colors flex items-center"
                >
                  <Building className="h-4 w-4 mr-1" />
                  Recruiter
                </Link>
              </div>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;