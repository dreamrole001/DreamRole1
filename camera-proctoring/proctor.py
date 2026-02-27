import cv2
import time
import threading
from datetime import datetime
from collections import deque
from face_detector import FaceDetector
from mobile_detector import MobileDetector

class Proctor:
    def __init__(self, assignment_id, user_id, test_name, violation_logger):
        self.assignment_id = assignment_id
        self.user_id = user_id
        self.test_name = test_name
        self.violation_logger = violation_logger
        
        # Initialize detectors
        self.face_detector = FaceDetector()
        self.mobile_detector = MobileDetector()
        
        # Camera
        self.cap = None
        self.is_monitoring = False
        self.current_frame = None
        self.frame_lock = threading.Lock()
        
        # Violation tracking
        self.violations = {
            'no_face': 0,
            'multiple_faces': 0,
            'mobile_detected': 0,
            'looking_away': 0
        }
        
        self.violation_thresholds = {
            'no_face': 5,        # Seconds before warning
            'multiple_faces': 1,  # Immediate termination
            'mobile_detected': 1, # Immediate termination
            'looking_away': 10    # Seconds before warning
        }
        
        self.violation_timers = {
            'no_face': 0,
            'looking_away': 0
        }
        
        # Warning tracking
        self.warnings = []
        self.warning_count = 0
        self.max_warnings = 3
        
        # FPS and performance
        self.fps = 0
        self.frame_count = 0
        self.start_time = time.time()
        
        # Face tracking
        self.face_history = deque(maxlen=30)
        self.face_present = False
        
        # Termination flag
        self.terminate_test = False
        
    def start_monitoring(self):
        """Start camera monitoring"""
        print(f"▶️ Starting proctor for test {self.assignment_id}")
        
        # Open camera
        self.cap = cv2.VideoCapture(0)
        if not self.cap.isOpened():
            print("❌ Error: Could not open camera")
            return
        
        # Set camera properties for better performance
        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
        self.cap.set(cv2.CAP_PROP_FPS, 30)
        
        self.is_monitoring = True
        
        # Log test start
        self.violation_logger.log_event(
            self.assignment_id,
            'TEST_STARTED',
            'Test started with proctoring'
        )
        
        while self.is_monitoring:
            ret, frame = self.cap.read()
            if not ret:
                print("❌ Error: Could not read frame")
                time.sleep(0.1)
                continue
            
            # Update FPS calculation
            self.frame_count += 1
            if self.frame_count % 30 == 0:
                elapsed = time.time() - self.start_time
                self.fps = self.frame_count / elapsed
            
            # Analyze frame
            analyzed_frame = self.analyze_frame(frame)
            
            # Update current frame for streaming
            with self.frame_lock:
                self.current_frame = analyzed_frame
            
            # Check if test should be terminated
            if self.terminate_test:
                self.stop_monitoring()
                break
            
            time.sleep(0.01)  # Small delay to prevent CPU overload
    
    def analyze_frame(self, frame):
        """Analyze frame for violations"""
        # Detect faces
        faces, face_frame = self.face_detector.detect_faces(frame)
        num_faces = len(faces)
        
        # Update face history
        self.face_history.append(num_faces > 0)
        self.face_present = num_faces > 0
        
        # Detect mobile phones
        mobile_detected, mobile_frame = self.mobile_detector.detect_mobile(face_frame)
        
        # Check for violations
        
        # 1. No face detected
        if num_faces == 0:
            self.violation_timers['no_face'] += 1
            if self.violation_timers['no_face'] > self.violation_thresholds['no_face'] * 30:  # 30 FPS
                self.violations['no_face'] += 1
                self.add_violation('no_face', 'No face detected in frame')
                self.violation_timers['no_face'] = 0
        else:
            self.violation_timers['no_face'] = max(0, self.violation_timers['no_face'] - 5)
        
        # 2. Multiple faces detected - IMMEDIATE TERMINATION
        if num_faces > 1:
            self.violations['multiple_faces'] += 1
            self.add_violation('multiple_faces', f'{num_faces} people detected in frame!', critical=True)
            self.terminate_test = True
        
        # 3. Mobile phone detected - IMMEDIATE TERMINATION
        if mobile_detected:
            self.violations['mobile_detected'] += 1
            self.add_violation('mobile_detected', 'Mobile phone detected in frame!', critical=True)
            self.terminate_test = True
        
        # 4. Check if looking away (based on face position stability)
        if num_faces == 1 and self.face_detector.detect_looking_away():
            self.violation_timers['looking_away'] += 1
            if self.violation_timers['looking_away'] > self.violation_thresholds['looking_away'] * 30:
                self.violations['looking_away'] += 1
                self.add_violation('looking_away', 'Candidate looking away from screen')
                self.violation_timers['looking_away'] = 0
        else:
            self.violation_timers['looking_away'] = max(0, self.violation_timers['looking_away'] - 5)
        
        # Draw annotations on frame
        annotated_frame = self.draw_annotations(frame, faces, mobile_detected)
        
        return annotated_frame
    
    def add_violation(self, violation_type, message, critical=False):
        """Add a violation and log it"""
        timestamp = datetime.now().isoformat()
        
        violation = {
            'type': violation_type,
            'message': message,
            'timestamp': timestamp,
            'critical': critical
        }
        
        self.warnings.append(violation)
        
        # Log to file
        self.violation_logger.log_violation(
            self.assignment_id,
            violation_type,
            message,
            critical,
            self.current_frame if critical else None
        )
        
        # Increment warning count for non-critical violations
        if not critical:
            self.warning_count += 1
        
        print(f"⚠️ VIOLATION [{violation_type}]: {message}")
        
        # Check if max warnings exceeded
        if self.warning_count >= self.max_warnings:
            self.terminate_test = True
            self.violation_logger.log_event(
                self.assignment_id,
                'TEST_TERMINATED',
                f'Test terminated after {self.warning_count} warnings'
            )
            print(f"🛑 TEST TERMINATED: Maximum warnings ({self.max_warnings}) exceeded")
    
    def draw_annotations(self, frame, faces, mobile_detected):
        """Draw annotations on frame"""
        # Draw face rectangles
        for (x, y, w, h) in faces:
            # Draw rectangle around face
            cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
            
            # Add "Face" label
            cv2.putText(frame, 'Face', (x, y-10),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
        
        # Add warning if multiple faces
        if len(faces) > 1:
            cv2.putText(frame, f'⚠️ WARNING: {len(faces)} faces detected!', 
                       (10, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
        
        # Add warning if mobile detected
        if mobile_detected:
            cv2.putText(frame, '⚠️ WARNING: Mobile phone detected!', 
                       (10, 90), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
        
        # Add FPS counter
        cv2.putText(frame, f'FPS: {self.fps:.1f}', (10, 30),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
        
        # Add face count
        cv2.putText(frame, f'Faces: {len(faces)}', (10, 120),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
        
        # Add warning count
        cv2.putText(frame, f'Warnings: {self.warning_count}/{self.max_warnings}', 
                   (10, 150), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
        
        # Add test terminated warning if applicable
        if self.terminate_test:
            cv2.putText(frame, '!!! TEST TERMINATED !!!', 
                       (frame.shape[1]//2 - 150, frame.shape[0]//2),
                       cv2.FONT_HERSHEY_SIMPLEX, 1.0, (0, 0, 255), 3)
        
        return frame
    
    def get_current_frame(self):
        """Get current frame for streaming"""
        with self.frame_lock:
            if self.current_frame is not None:
                return self.current_frame.copy()
        return None
    
    def get_current_violations(self):
        """Get current violation counts"""
        return self.violations.copy()
    
    def get_warnings(self):
        """Get recent warnings"""
        return self.warnings[-5:]  # Last 5 warnings
    
    def should_terminate(self):
        """Check if test should be terminated"""
        return self.terminate_test
    
    def get_violation_summary(self):
        """Get summary of all violations"""
        return {
            'assignment_id': self.assignment_id,
            'user_id': self.user_id,
            'test_name': self.test_name,
            'violations': self.violations,
            'warning_count': self.warning_count,
            'terminated': self.terminate_test,
            'total_warnings': len(self.warnings),
            'warnings': self.warnings
        }
    
    def stop_monitoring(self):
        """Stop camera monitoring"""
        self.is_monitoring = False
        if self.cap:
            self.cap.release()
            print(f"📷 Camera released for test {self.assignment_id}")
        
        # Log test end
        status = "Terminated" if self.terminate_test else "Completed"
        self.violation_logger.log_event(
            self.assignment_id,
            'TEST_ENDED',
            f'Test ended. Status: {status}'
        )
        print(f"✅ Proctor stopped for test {self.assignment_id}. Status: {status}")