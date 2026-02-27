// src/components/CameraPermission.jsx
import React, { useState, useEffect } from 'react';
import { Camera, VideoOff, Loader, CheckCircle, XCircle } from 'lucide-react';
import axios from 'axios';

const CameraPermission = ({ onPermissionGranted, onPermissionDenied }) => {
  const [checking, setChecking] = useState(false);
  const [status, setStatus] = useState(null);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);
  
  const PROCTORING_URL = 'http://localhost:5001';

  const checkProctoringService = async () => {
    try {
      const response = await axios.get(`${PROCTORING_URL}/health`);
      console.log('Proctoring service health:', response.data);
      return response.status === 200;
    } catch (error) {
      console.error('Proctoring service health check failed:', error);
      return false;
    }
  };

  const checkCamera = async () => {
    setChecking(true);
    setError(null);
    setStatus('checking');
    
    try {
      // Step 1: Check if proctoring service is running
      const serviceRunning = await checkProctoringService();
      if (!serviceRunning) {
        throw new Error('Proctoring service not running. Please start it on port 5001.');
      }
      
      // Step 2: Check browser camera permission with timeout
      console.log("Requesting camera permission...");
      const stream = await navigator.mediaDevices.getUserMedia({ 
        video: { 
          width: { ideal: 640 },
          height: { ideal: 480 },
          facingMode: 'user'
        } 
      }).catch(err => {
        console.error('Camera getUserMedia error:', err);
        throw err;
      });
      
      // Got permission, stop the stream
      stream.getTracks().forEach(track => {
        track.stop();
        console.log('Camera track stopped');
      });
      console.log("✅ Camera permission granted");
      
      // Step 3: Check proctoring service camera check
      console.log("Checking proctoring service camera...");
      const response = await axios.post(`${PROCTORING_URL}/check-camera`);
      console.log('Proctoring camera check response:', response.data);
      
      if (response.data.available) {
        setStatus('success');
        if (onPermissionGranted) onPermissionGranted();
      } else {
        setStatus('error');
        const errorMsg = response.data.error || 'Camera not available. Please check your camera connection.';
        setError(errorMsg);
        if (onPermissionDenied) onPermissionDenied(errorMsg);
      }
    } catch (err) {
      console.error("Camera error:", err);
      setStatus('error');
      
      let errorMessage = '';
      
      if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
        errorMessage = 'Camera access denied. Please allow camera access in your browser settings and refresh.';
      } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
        errorMessage = 'No camera found. Please connect a camera and try again.';
      } else if (err.name === 'NotReadableError' || err.name === 'TrackStartError') {
        errorMessage = 'Camera is already in use by another application. Please close other apps using camera.';
      } else if (err.name === 'OverconstrainedError') {
        errorMessage = 'Camera does not support required settings. Using default settings.';
        // Try again with default settings
        try {
          const fallbackStream = await navigator.mediaDevices.getUserMedia({ video: true });
          fallbackStream.getTracks().forEach(track => track.stop());
          setStatus('success');
          if (onPermissionGranted) onPermissionGranted();
          return;
        } catch (fallbackErr) {
          errorMessage = 'Could not access camera even with default settings.';
        }
      } else if (err.message && err.message.includes('Proctoring service not running')) {
        errorMessage = err.message;
      } else if (err.code === 'ECONNREFUSED' || err.message.includes('Network Error')) {
        errorMessage = 'Cannot connect to proctoring service. Make sure it\'s running on port 5001.';
      } else {
        errorMessage = `Camera error: ${err.message || 'Unknown error'}`;
      }
      
      setError(errorMessage);
      if (onPermissionDenied) onPermissionDenied(errorMessage);
    } finally {
      setChecking(false);
    }
  };

  useEffect(() => {
    checkCamera();
  }, []);

  const handleRetry = () => {
    setRetryCount(prev => prev + 1);
    checkCamera();
  };

  if (checking) {
    return (
      <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 text-center">
        <Loader className="h-8 w-8 animate-spin text-blue-600 mx-auto mb-3" />
        <p className="text-blue-800 font-medium">Checking camera access...</p>
        <p className="text-sm text-blue-600 mt-1">Please allow camera access when prompted</p>
      </div>
    );
  }

  if (status === 'success') {
    return (
      <div className="bg-green-50 border border-green-200 rounded-lg p-4">
        <div className="flex items-center">
          <CheckCircle className="h-5 w-5 text-green-600 mr-2 flex-shrink-0" />
          <div>
            <p className="text-green-800 font-medium">Camera ready!</p>
            <p className="text-sm text-green-600">You can now start the test</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-6">
        <VideoOff className="h-8 w-8 text-red-600 mx-auto mb-3" />
        <p className="text-red-800 font-medium text-center mb-2">Camera Required</p>
        <p className="text-sm text-red-600 text-center mb-4">{error}</p>
        <div className="space-y-2">
          <button
            onClick={handleRetry}
            className="w-full bg-red-600 text-white py-2 rounded-lg hover:bg-red-700 transition-colors"
          >
            Try Again
          </button>
          <p className="text-xs text-gray-500 text-center mt-2">
            Troubleshooting:<br/>
            1. Make sure camera is connected<br/>
            2. Close other apps using camera<br/>
            3. Check camera permissions in browser<br/>
            4. Restart proctoring service
          </p>
        </div>
      </div>
    );
  }

  return null;
};

export default CameraPermission;