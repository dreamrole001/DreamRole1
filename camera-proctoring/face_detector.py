import cv2
import numpy as np
import os

class FaceDetector:
    def __init__(self):
        """Initialize face detector"""
        self.use_dnn = False
        
        # Try to use DNN-based detector (more accurate)
        try:
            model_path = 'models/face_detection_model/opencv_face_detector_uint8.pb'
            config_path = 'models/face_detection_model/opencv_face_detector.pbtxt'
            
            if os.path.exists(model_path) and os.path.exists(config_path):
                self.net = cv2.dnn.readNetFromTensorflow(model_path, config_path)
                self.use_dnn = True
                print("FaceDetector: Using DNN model")
            else:
                # Use Haar cascade as fallback
                cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
                self.face_cascade = cv2.CascadeClassifier(cascade_path)
                print("FaceDetector: Using Haar cascade")
        except Exception as e:
            print(f"FaceDetector initialization error: {e}")
            cascade_path = cv2.data.haarcascades + 'haarcascade_frontalface_default.xml'
            self.face_cascade = cv2.CascadeClassifier(cascade_path)
        
        # Tracking variables
        self.face_history = []
        self.face_positions = []
        
    def detect_faces(self, frame):
        """Detect faces in frame"""
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        
        if self.use_dnn:
            faces = self.detect_faces_dnn(frame, gray)
        else:
            faces = self.detect_faces_haar(gray)
        
        # Filter invalid faces
        valid_faces = []
        for (x, y, w, h) in faces:
            if w > 30 and h > 30:  # Minimum face size
                valid_faces.append((x, y, w, h))
        
        # Track face positions
        self.update_face_tracking(valid_faces)
        
        return valid_faces, frame
    
    def detect_faces_haar(self, gray):
        """Detect faces using Haar cascade"""
        faces = self.face_cascade.detectMultiScale(
            gray,
            scaleFactor=1.1,
            minNeighbors=5,
            minSize=(60, 60),
            flags=cv2.CASCADE_SCALE_IMAGE
        )
        return faces
    
    def detect_faces_dnn(self, frame, gray):
        """Detect faces using DNN model"""
        try:
            h, w = frame.shape[:2]
            blob = cv2.dnn.blobFromImage(frame, 1.0, (300, 300), [104, 117, 123])
            self.net.setInput(blob)
            detections = self.net.forward()
            
            faces = []
            for i in range(detections.shape[2]):
                confidence = detections[0, 0, i, 2]
                if confidence > 0.5:  # Confidence threshold
                    box = detections[0, 0, i, 3:7] * np.array([w, h, w, h])
                    (x, y, x2, y2) = box.astype("int")
                    faces.append((x, y, x2-x, y2-y))
            
            return faces
        except Exception as e:
            # Fallback to Haar on error
            return self.detect_faces_haar(gray)
    
    def update_face_tracking(self, faces):
        """Update face tracking history"""
        self.face_history.append(len(faces))
        if len(self.face_history) > 30:
            self.face_history.pop(0)
        
        # Store face positions for movement detection
        if len(faces) == 1:
            self.face_positions.append(faces[0])
            if len(self.face_positions) > 60:  # Last 2 seconds
                self.face_positions.pop(0)
    
    def detect_looking_away(self):
        """Detect if user is looking away based on face position"""
        if len(self.face_positions) < 30:
            return False
        
        # Calculate movement variance
        x_positions = [pos[0] + pos[2]//2 for pos in self.face_positions]
        y_positions = [pos[1] + pos[3]//2 for pos in self.face_positions]
        
        x_variance = np.var(x_positions) if len(x_positions) > 1 else 0
        y_variance = np.var(y_positions) if len(y_positions) > 1 else 0
        
        # High variance means moving a lot
        return x_variance > 5000 or y_variance > 5000
    
    def get_face_stability(self):
        """Get face stability score (0-1)"""
        if len(self.face_history) < 30:
            return 1.0
        
        # Calculate ratio of frames with face detected
        face_ratio = sum(1 for f in self.face_history if f > 0) / len(self.face_history)
        
        # Calculate position stability if face present
        if face_ratio > 0.5 and len(self.face_positions) > 10:
            x_centers = [pos[0] + pos[2]//2 for pos in self.face_positions[-10:]]
            y_centers = [pos[1] + pos[3]//2 for pos in self.face_positions[-10:]]
            
            x_std = np.std(x_centers) if len(x_centers) > 1 else 0
            y_std = np.std(y_centers) if len(y_centers) > 1 else 0
            
            # Normalize by frame width/height
            position_stability = 1.0 - min(1.0, (x_std + y_std) / 500)
            return face_ratio * position_stability
        
        return face_ratio