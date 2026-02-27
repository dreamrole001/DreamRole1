// src/components/CameraPreview.jsx - SEPARATE COMPONENT
import React, { useState, useEffect, useRef } from 'react';
import { Camera, AlertTriangle, Maximize2, Minimize2, RefreshCw, XCircle, Wifi, WifiOff, Loader, CheckCircle } from 'lucide-react';

const CameraPreview = ({ 
  assignmentId, 
  warningCount = 0, 
  violations = [],
  onClose,
  isTestCompleted = false,
  mobileFrames = 0
}) => {
  const [isMinimized, setIsMinimized] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const [hasError, setHasError] = useState(false);
  const [retryCount, setRetryCount] = useState(0);
  const [isVisible, setIsVisible] = useState(true);
  const [isLoading, setIsLoading] = useState(true);
  const [networkStatus, setNetworkStatus] = useState('connected');
  const [feedActive, setFeedActive] = useState(false);
  
  const imgRef = useRef(null);
  const refreshIntervalRef = useRef(null);
  
  const PROCTORING_URL = 'http://localhost:5001';
  const videoFeedUrl = `${PROCTORING_URL}/video-feed/${assignmentId}`;

  // Network status monitoring
  useEffect(() => {
    const handleOnline = () => setNetworkStatus('connected');
    const handleOffline = () => setNetworkStatus('disconnected');
    
    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);
    
    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // Auto-refresh video feed
  useEffect(() => {
    if (!assignmentId || isTestCompleted) return;

    const refreshFeed = () => {
      if (imgRef.current && !hasError) {
        const timestamp = Date.now();
        imgRef.current.src = `${videoFeedUrl}?t=${timestamp}`;
      }
    };

    // Initial load
    refreshFeed();
    
    // Refresh every 3 seconds for smooth feed
    refreshIntervalRef.current = setInterval(refreshFeed, 3000);

    return () => {
      if (refreshIntervalRef.current) {
        clearInterval(refreshIntervalRef.current);
      }
    };
  }, [assignmentId, hasError, isTestCompleted, videoFeedUrl]);

  // Auto-retry on error
  useEffect(() => {
    if (hasError && retryCount < 3 && !isTestCompleted) {
      const timer = setTimeout(() => {
        setHasError(false);
        setRetryCount(prev => prev + 1);
        setIsLoading(true);
        if (imgRef.current) {
          imgRef.current.src = `${videoFeedUrl}?t=${Date.now()}`;
        }
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [hasError, retryCount, videoFeedUrl, isTestCompleted]);

  const refreshImage = () => {
    setHasError(false);
    setIsLoading(true);
    if (imgRef.current) {
      imgRef.current.src = `${videoFeedUrl}?t=${Date.now()}`;
    }
  };

  const toggleMinimize = () => {
    setIsMinimized(!isMinimized);
    if (!isMinimized) {
      setIsExpanded(false);
    }
  };

  const toggleExpand = () => {
    setIsExpanded(!isExpanded);
    if (!isExpanded) {
      setIsMinimized(false);
    }
  };

  const handleClose = () => {
    setIsVisible(false);
    if (onClose) onClose();
  };

  if (!isVisible) return null;

  // Size classes
  let widthClass = 'w-80';
  let heightClass = 'h-48';
  
  if (isMinimized) {
    widthClass = 'w-48';
    heightClass = 'h-36';
  } else if (isExpanded) {
    widthClass = 'w-96';
    heightClass = 'h-64';
  }

  // Warning color based on mobile frames
  const getWarningColor = () => {
    if (mobileFrames === 0) return 'bg-green-500';
    if (mobileFrames === 1) return 'bg-yellow-500';
    if (mobileFrames === 2) return 'bg-orange-500';
    return 'bg-red-500';
  };

  return (
    <div className={`fixed bottom-4 right-4 ${widthClass} bg-black rounded-lg overflow-hidden border-2 border-blue-500 shadow-2xl z-[9999] transition-all duration-300`}>
      {/* Header */}
      <div className="bg-gradient-to-r from-blue-600 to-purple-600 text-white text-xs py-2 px-3 flex justify-between items-center">
        <span className="flex items-center font-medium">
          <Camera className="h-3 w-3 mr-1" />
          {isMinimized ? 'Camera' : 'Live Proctoring'}
        </span>
        <div className="flex items-center space-x-2">
          {/* Network Status */}
          {networkStatus === 'connected' ? (
            <Wifi className="h-3 w-3 text-green-300" />
          ) : (
            <WifiOff className="h-3 w-3 text-yellow-300" />
          )}
          
          {/* Refresh Button */}
          <button 
            onClick={refreshImage}
            className="hover:bg-white/20 rounded p-1 transition-colors"
            title="Refresh feed"
            disabled={isTestCompleted}
          >
            <RefreshCw className={`h-3 w-3 ${isLoading ? 'animate-spin' : ''}`} />
          </button>
          
          {/* Expand Button */}
          {!isMinimized && (
            <button 
              onClick={toggleExpand} 
              className="hover:bg-white/20 rounded p-1 transition-colors"
              title={isExpanded ? "Shrink" : "Expand"}
            >
              {isExpanded ? <Minimize2 className="h-3 w-3" /> : <Maximize2 className="h-3 w-3" />}
            </button>
          )}
          
          {/* Minimize Button */}
          <button 
            onClick={toggleMinimize} 
            className="hover:bg-white/20 rounded p-1 transition-colors"
            title={isMinimized ? "Expand" : "Minimize"}
          >
            {isMinimized ? <Maximize2 className="h-3 w-3" /> : <Minimize2 className="h-3 w-3" />}
          </button>
          
          {/* Close Button */}
          <button 
            onClick={handleClose} 
            className="hover:bg-white/20 rounded p-1 transition-colors"
            title="Close"
          >
            <XCircle className="h-3 w-3" />
          </button>
          
          {/* REC Indicator */}
          <span className="bg-green-500 text-white px-2 py-0.5 rounded-full text-[10px] font-bold animate-pulse">
            REC
          </span>
          
          {/* Warning Count */}
          {!isMinimized && warningCount > 0 && (
            <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
              warningCount === 1 ? 'bg-yellow-500' :
              warningCount === 2 ? 'bg-orange-500' : 'bg-red-500'
            }`}>
              W: {warningCount}/3
            </span>
          )}
        </div>
      </div>
      
      {/* Video Feed */}
      <div className="relative bg-gray-900">
        {!hasError && !isTestCompleted ? (
          <>
            {/* Loading Spinner */}
            {isLoading && (
              <div className={`absolute inset-0 bg-gray-900/75 flex items-center justify-center z-10 ${heightClass}`}>
                <Loader className="h-8 w-8 animate-spin text-blue-500" />
              </div>
            )}
            
            {/* Video Image */}
            <img 
              ref={imgRef}
              src={`${videoFeedUrl}?t=${Date.now()}`}
              alt="Camera Feed"
              className={`w-full ${heightClass} object-cover bg-gray-900`}
              onLoad={() => {
                console.log('✅ Feed loaded');
                setHasError(false);
                setIsLoading(false);
                setRetryCount(0);
                setFeedActive(true);
              }}
              onError={() => {
                console.log('❌ Feed error');
                setHasError(true);
                setIsLoading(false);
                setFeedActive(false);
              }}
            />

            {/* Status Overlay */}
            {!isMinimized && !isLoading && (
              <div className="absolute bottom-8 left-0 right-0 bg-gradient-to-t from-black to-transparent p-2">
                <div className="flex justify-between text-[10px] text-white">
                  <span>Mobile: {mobileFrames > 0 ? `⚠️ ${mobileFrames}/3` : '✓'}</span>
                </div>
              </div>
            )}
          </>
        ) : (
          /* Error State */
          <div className={`w-full ${heightClass} bg-gray-800 flex flex-col items-center justify-center p-4`}>
            {isTestCompleted ? (
              <>
                <CheckCircle className="h-8 w-8 text-green-500 mb-2" />
                <span className="text-green-500 text-xs text-center">Test Completed</span>
              </>
            ) : networkStatus === 'disconnected' ? (
              <>
                <WifiOff className="h-8 w-8 text-yellow-500 mb-2" />
                <span className="text-yellow-500 text-xs text-center">Network disconnected</span>
              </>
            ) : (
              <>
                <Camera className="h-8 w-8 text-gray-600 mb-2" />
                <span className="text-gray-400 text-xs text-center">Camera feed unavailable</span>
                <span className="text-gray-500 text-[10px] mt-1">Retry {retryCount}/3</span>
                <button
                  onClick={refreshImage}
                  className="mt-2 bg-blue-600 text-white px-3 py-1 rounded text-xs hover:bg-blue-700 transition-colors"
                >
                  Retry
                </button>
              </>
            )}
          </div>
        )}
        
        {/* Mobile Detection Progress Bar */}
        {!isMinimized && !isTestCompleted && mobileFrames > 0 && (
          <div className="absolute bottom-0 left-0 h-1 bg-gray-700 w-full">
            <div 
              className={`h-1 transition-all duration-300 ${getWarningColor()}`}
              style={{ width: `${(mobileFrames / 3) * 100}%` }}
            />
          </div>
        )}
        
        {/* Warning Message */}
        {violations.length > 0 && !isMinimized && !isTestCompleted && (
          <div className="absolute bottom-0 left-0 right-0 bg-red-600/90 text-white text-xs p-2 animate-pulse">
            <div className="flex items-center">
              <AlertTriangle className="h-3 w-3 mr-1 flex-shrink-0" />
              <span className="truncate font-medium">
                ⚠️ {violations[violations.length - 1]?.message || 'Violation detected'}
              </span>
            </div>
          </div>
        )}
      </div>
      
      {/* Minimized Indicators */}
      {isMinimized && !isTestCompleted && (
        <>
          {/* Active Recording Dot */}
          <div className="absolute -top-1 -right-1 w-3 h-3 bg-green-500 rounded-full animate-pulse" />
          
          {/* Warning Count Badge */}
          {warningCount > 0 && (
            <div className="absolute bottom-0 left-0 right-0 bg-red-600 text-white text-[10px] p-1 text-center font-bold">
              W: {warningCount}/3
            </div>
          )}
          
          {/* Mobile Detection Indicator */}
          {mobileFrames > 0 && (
            <div className="absolute top-8 right-1 bg-red-600 text-white text-[8px] px-1 py-0.5 rounded">
              📱 {mobileFrames}/3
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default CameraPreview;