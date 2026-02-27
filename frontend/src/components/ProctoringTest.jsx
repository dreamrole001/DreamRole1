// frontend/src/components/ProctoringTest.jsx
import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';

const ProctoringTest = () => {
  const [status, setStatus] = useState('Checking...');
  const [cameraStatus, setCameraStatus] = useState(null);
  const [videoStream, setVideoStream] = useState(null);
  const [proctoringUrl, setProctoringUrl] = useState('http://localhost:5001');
  const [testAssignmentId, setTestAssignmentId] = useState('24');
  const [videoFeedUrl, setVideoFeedUrl] = useState('');
  const [videoFeedError, setVideoFeedError] = useState(false);
  const videoRef = useRef(null);
  const imgRef = useRef(null);

  useEffect(() => {
    checkProctoringHealth();
  }, []);

  const checkProctoringHealth = async () => {
    try {
      const response = await axios.get(`${proctoringUrl}/health`);
      setStatus(`✅ Proctoring Service: ${response.data.status}`);
    } catch (error) {
      setStatus(`❌ Proctoring Service Error: ${error.message}`);
    }
  };

  const checkCamera = async () => {
    try {
      // First check browser camera access
      const stream = await navigator.mediaDevices.getUserMedia({ 
        video: { 
          width: 640, 
          height: 480,
          facingMode: 'user'
        } 
      });
      setVideoStream(stream);
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
      
      // Then check proctoring service
      const response = await axios.post(`${proctoringUrl}/check-camera`);
      setCameraStatus(`✅ Camera: ${response.data.available ? 'Available' : 'Not Available'}`);
    } catch (error) {
      setCameraStatus(`❌ Camera Error: ${error.message}`);
    }
  };

  const stopCamera = () => {
    if (videoStream) {
      videoStream.getTracks().forEach(track => track.stop());
      setVideoStream(null);
    }
  };

  const testVideoFeed = () => {
    const url = `${proctoringUrl}/video-feed/${testAssignmentId}`;
    setVideoFeedUrl(url);
    setVideoFeedError(false);
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <div className="max-w-4xl mx-auto px-4">
        <h1 className="text-3xl font-bold text-gray-800 mb-6">Proctoring System Test</h1>
        
        {/* Service Status */}
        <div className="bg-white rounded-lg shadow-lg p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">1. Service Status</h2>
          <div className="p-4 bg-gray-50 rounded-lg">
            <p className="font-mono text-lg">{status}</p>
          </div>
          <button
            onClick={checkProctoringHealth}
            className="mt-4 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700"
          >
            Refresh Status
          </button>
        </div>

        {/* Camera Test */}
        <div className="bg-white rounded-lg shadow-lg p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">2. Camera Test</h2>
          
          <div className="mb-4">
            <video 
              ref={videoRef}
              autoPlay 
              playsInline 
              muted
              className="w-full max-w-md border-2 border-gray-300 rounded-lg mx-auto"
              style={{ display: videoStream ? 'block' : 'none' }}
            />
          </div>

          <div className="flex space-x-4 mb-4">
            <button
              onClick={checkCamera}
              className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700"
            >
              Start Camera
            </button>
            <button
              onClick={stopCamera}
              className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700"
            >
              Stop Camera
            </button>
          </div>

          {cameraStatus && (
            <div className="p-4 bg-gray-50 rounded-lg">
              <p className="font-mono">{cameraStatus}</p>
            </div>
          )}
        </div>

        {/* Video Feed Test */}
        <div className="bg-white rounded-lg shadow-lg p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">3. Video Feed Test</h2>
          
          <div className="mb-4 flex space-x-4">
            <input
              type="text"
              value={testAssignmentId}
              onChange={(e) => setTestAssignmentId(e.target.value)}
              placeholder="Assignment ID"
              className="flex-1 px-4 py-2 border border-gray-300 rounded-lg"
            />
            <button
              onClick={testVideoFeed}
              className="bg-purple-600 text-white px-4 py-2 rounded-lg hover:bg-purple-700"
            >
              Load Video Feed
            </button>
          </div>

          {videoFeedUrl && (
            <div className="mt-4">
              <p className="text-sm text-gray-600 mb-2">Video Feed URL: {videoFeedUrl}</p>
              <div className="border-2 border-gray-300 rounded-lg overflow-hidden bg-black">
                <img
                  ref={imgRef}
                  src={`${videoFeedUrl}?t=${Date.now()}`}
                  alt="Video Feed"
                  className="w-full max-w-md mx-auto"
                  onError={() => setVideoFeedError(true)}
                  onLoad={() => setVideoFeedError(false)}
                />
              </div>
              {videoFeedError && (
                <p className="text-red-600 mt-2">❌ Failed to load video feed</p>
              )}
            </div>
          )}
        </div>

        {/* Debug Information */}
        <div className="bg-white rounded-lg shadow-lg p-6">
          <h2 className="text-xl font-semibold mb-4">4. Debug Information</h2>
          <div className="space-y-2 font-mono text-sm">
            <p><strong>Proctoring URL:</strong> {proctoringUrl}</p>
            <p><strong>Browser:</strong> {navigator.userAgent}</p>
            <p><strong>Camera Permission:</strong> {videoStream ? '✅ Granted' : '⏸️ Not requested'}</p>
            <p><strong>Video Feed Active:</strong> {videoFeedUrl ? '✅ Yes' : '⏸️ No'}</p>
          </div>
          
          <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
            <h3 className="font-semibold text-yellow-800 mb-2">Troubleshooting Steps:</h3>
            <ol className="list-decimal list-inside space-y-1 text-sm text-yellow-700">
              <li>Make sure proctoring service is running: <code className="bg-yellow-100 px-2 py-1 rounded">cd camera-proctoring &amp;&amp; python app.py</code></li>
              <li>Check if port 5001 is accessible: <code className="bg-yellow-100 px-2 py-1 rounded">curl http://localhost:5001/health</code></li>
              <li>Verify camera permissions in browser</li>
              <li>Check browser console for errors (F12)</li>
            </ol>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ProctoringTest;