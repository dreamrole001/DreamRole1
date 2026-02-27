import cv2
import numpy as np
import os

class MobileDetector:
    def __init__(self):
        """Initialize mobile phone detector"""
        self.use_dnn = False
        
        # Paths for pre-trained models
        self.model_paths = {
            'ssd': 'models/mobile_detection_model/MobileNetSSD_deploy.caffemodel',
            'prototxt': 'models/mobile_detection_model/MobileNetSSD_deploy.prototxt',
        }
        
        # Check if we have SSD model
        if os.path.exists(self.model_paths['ssd']) and os.path.exists(self.model_paths['prototxt']):
            try:
                self.net = cv2.dnn.readNetFromCaffe(
                    self.model_paths['prototxt'],
                    self.model_paths['ssd']
                )
                self.use_dnn = True
                self.model_type = 'ssd'
                print("MobileDetector: Using SSD model")
            except Exception as e:
                print(f"SSD model load error: {e}")
        
        # Fallback to contour detection
        if not self.use_dnn:
            print("MobileDetector: Using contour-based detection")
        
        # Mobile phone color ranges (common colors)
        self.phone_colors = {
            'black': ([0, 0, 0], [180, 255, 30]),
            'silver': ([0, 0, 150], [180, 30, 255]),
            'white': ([0, 0, 200], [180, 30, 255]),
            'gold': ([15, 50, 150], [35, 255, 255]),
            'blue': ([90, 50, 50], [130, 255, 255])
        }
        
        # Phone aspect ratios (width/height)
        self.phone_aspect_ratios = [
            (0.4, 0.6),   # Portrait phone
            (1.8, 2.2)    # Landscape phone
        ]
        
        # Minimum and maximum area for phone (percentage of frame)
        self.min_area_percent = 0.03  # 3% of frame
        self.max_area_percent = 0.20  # 20% of frame
        
        # Detection history for stability
        self.detection_history = []
        self.confidence_threshold = 0.6
        
    def detect_mobile(self, frame):
        """Detect if a mobile phone is present in the frame"""
        detected = False
        confidence = 0.0
        
        if self.use_dnn:
            detected, confidence = self.detect_mobile_dnn(frame)
        else:
            detected, confidence = self.detect_mobile_contour(frame)
        
        # Apply temporal smoothing
        self.update_detection_history(detected, confidence)
        
        # Get stable detection
        stable_detected = self.get_stable_detection()
        
        return stable_detected, frame
    
    def detect_mobile_dnn(self, frame):
        """Detect mobile phone using DNN"""
        try:
            h, w = frame.shape[:2]
            
            # SSD preprocessing
            blob = cv2.dnn.blobFromImage(frame, 0.007843, (300, 300), 127.5)
            self.net.setInput(blob)
            detections = self.net.forward()
            
            for i in range(detections.shape[2]):
                confidence = detections[0, 0, i, 2]
                if confidence > 0.5:
                    class_id = int(detections[0, 0, i, 1])
                    # Class 15 in COCO/SSD is 'cell phone'
                    if class_id == 15:
                        return True, confidence
            
            return False, 0.0
            
        except Exception as e:
            print(f"DNN detection error: {e}")
            return self.detect_mobile_contour(frame)
    
    def detect_mobile_contour(self, frame):
        """Detect mobile phone using contour analysis"""
        hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
        
        # Create mask for phone-like colors
        combined_mask = np.zeros(frame.shape[:2], dtype=np.uint8)
        
        for color_name, (lower, upper) in self.phone_colors.items():
            lower_np = np.array(lower, dtype=np.uint8)
            upper_np = np.array(upper, dtype=np.uint8)
            mask = cv2.inRange(hsv, lower_np, upper_np)
            combined_mask = cv2.bitwise_or(combined_mask, mask)
        
        # Clean up mask
        kernel = np.ones((5, 5), np.uint8)
        combined_mask = cv2.morphologyEx(combined_mask, cv2.MORPH_OPEN, kernel)
        combined_mask = cv2.morphologyEx(combined_mask, cv2.MORPH_CLOSE, kernel)
        
        # Find contours
        contours, _ = cv2.findContours(combined_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        frame_area = frame.shape[0] * frame.shape[1]
        
        for contour in contours:
            area = cv2.contourArea(contour)
            area_percent = area / frame_area
            
            # Check if area is in phone range
            if area_percent < self.min_area_percent or area_percent > self.max_area_percent:
                continue
            
            # Get bounding rectangle
            x, y, w, h = cv2.boundingRect(contour)
            aspect_ratio = w / h if h > 0 else 0
            
            # Check aspect ratio
            is_phone_aspect = False
            for min_ar, max_ar in self.phone_aspect_ratios:
                if min_ar <= aspect_ratio <= max_ar:
                    is_phone_aspect = True
                    break
            
            if is_phone_aspect:
                # Additional check: solidity (convex hull area / contour area)
                hull = cv2.convexHull(contour)
                hull_area = cv2.contourArea(hull)
                solidity = area / hull_area if hull_area > 0 else 0
                
                if solidity > 0.7:  # Phone shapes are generally solid
                    confidence = min(1.0, area_percent / 0.1)  # Normalize by expected size
                    return True, confidence
        
        return False, 0.0
    
    def update_detection_history(self, detected, confidence):
        """Update detection history for temporal smoothing"""
        self.detection_history.append((detected, confidence))
        if len(self.detection_history) > 30:  # Keep last 30 frames
            self.detection_history.pop(0)
    
    def get_stable_detection(self):
        """Get stable detection using temporal smoothing"""
        if len(self.detection_history) < 10:
            return False
        
        # Count detections in last 10 frames
        recent = self.detection_history[-10:]
        detection_count = sum(1 for d, _ in recent if d)
        avg_confidence = np.mean([c for _, c in recent if d]) if detection_count > 0 else 0
        
        # Stable detection if detected in 8+ frames with good confidence
        stable = detection_count >= 8 and avg_confidence >= self.confidence_threshold
        
        return stable