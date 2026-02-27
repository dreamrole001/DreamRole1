# camera-proctoring/app.py - COMPLETE WITH FULLSCREEN & KEY DETECTION
from flask import Flask, request, jsonify, Response
from flask_cors import CORS
import cv2
import numpy as np
import threading
import time
import logging
import platform
import requests
import re
from datetime import datetime

# Set up logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app, origins=["http://localhost:3000", "http://127.0.0.1:3000"])

# Backend URL for sending violations
BACKEND_URL = 'http://localhost:8080/api'

# Active test sessions
active_sessions = {}
session_lock = threading.Lock()

class Proctor:
    def __init__(self, assignment_id, user_id, test_name):
        self.assignment_id = assignment_id
        self.user_id = user_id
        self.test_name = test_name
        self.is_monitoring = False
        self.current_frame = None
        self.frame_lock = threading.Lock()
        self.terminate_test = False
        self.termination_reason = None
        self.test_completed = False
        self.cap = None
        self.frame_count = 0
        self.violations = []
        
        # Mobile detection - 3 frame threshold
        self.mobile_detected_frames = 0
        self.mobile_detection_threshold = 3
        
        # Multiple face detection - 3 frame threshold
        self.multiple_face_frames = 0
        self.multiple_face_threshold = 3
        
        # Tab switch detection (handled by frontend)
        self.tab_switches = 0
        self.tab_switch_threshold = 1  # Terminate on first tab switch
        
        # Key combination detection (handled by frontend)
        self.key_violations = 0
        self.key_violation_threshold = 1  # Terminate on first key violation
        
        # Load face detection
        self.face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_default.xml')
        
        logger.info(f"Proctor initialized for test {assignment_id}")
    
    def detect_mobile_phone(self, frame, faces):
        """Detect mobile phones in frame"""
        height, width = frame.shape[:2]
        gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        
        # Create mask to exclude face regions
        face_mask = np.zeros((height, width), dtype=np.uint8)
        for (fx, fy, fw, fh) in faces:
            x1 = max(0, fx - 30)
            y1 = max(0, fy - 30)
            x2 = min(width, fx + fw + 30)
            y2 = min(height, fy + fh + 30)
            face_mask[y1:y2, x1:x2] = 255
        
        # Look for bright regions
        _, bright_regions = cv2.threshold(gray, 180, 255, cv2.THRESH_BINARY)
        bright_regions = cv2.bitwise_and(bright_regions, cv2.bitwise_not(face_mask))
        
        # Find contours
        contours, _ = cv2.findContours(bright_regions, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        for contour in contours:
            area = cv2.contourArea(contour)
            if area < 500 or area > 40000:
                continue
            
            x, y, w, h = cv2.boundingRect(contour)
            if w < 40 or h < 40:
                continue
            
            aspect_ratio = w / h if h > 0 else 0
            if aspect_ratio < 0.3 or aspect_ratio > 3.0:
                continue
            
            roi = gray[y:y+h, x:x+w]
            if roi.size == 0:
                continue
            
            avg_brightness = np.mean(roi)
            edges = cv2.Canny(roi, 50, 150)
            edge_density = np.sum(edges > 0) / edges.size if edges.size > 0 else 0
            
            if avg_brightness > 150 and edge_density > 0.05:
                return (x, y, w, h)
        
        return None
    
    def add_violation(self, violation_type, message, critical=True):
        """Add a violation and check for termination"""
        violation = {
            'type': violation_type,
            'message': message,
            'timestamp': datetime.now().isoformat(),
            'critical': critical
        }
        self.violations.append(violation)
        logger.warning(f"VIOLATION: {message}")
        
        # Auto-terminate on critical violations
        if critical:
            self.terminate_test = True
            self.termination_reason = message
    
    def start_monitoring(self):
        """Start camera monitoring"""
        logger.info(f"Starting proctor for test {self.assignment_id}")
        
        # Open camera with multiple attempts
        for attempt in range(3):
            try:
                if platform.system() == 'Windows':
                    self.cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)
                else:
                    self.cap = cv2.VideoCapture(0)
                
                if self.cap and self.cap.isOpened():
                    # Set camera properties
                    self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
                    self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)
                    self.cap.set(cv2.CAP_PROP_FPS, 30)
                    self.cap.set(cv2.CAP_PROP_BUFFERSIZE, 1)
                    
                    # Test frame read
                    ret, frame = self.cap.read()
                    if ret and frame is not None:
                        logger.info(f"✅ Camera opened successfully on attempt {attempt + 1}")
                        break
                    else:
                        self.cap.release()
                        self.cap = None
                else:
                    logger.warning(f"Attempt {attempt + 1}: Could not open camera")
                    time.sleep(1)
            except Exception as e:
                logger.error(f"Camera error: {e}")
                if self.cap:
                    self.cap.release()
                self.cap = None
                time.sleep(1)
        
        if not self.cap or not self.cap.isOpened():
            logger.error("❌ Could not open camera after multiple attempts")
            return
        
        self.is_monitoring = True
        logger.info(f"✅ Proctor started for test {self.assignment_id}")
        
        # Monitoring loop
        while self.is_monitoring and not self.test_completed and not self.terminate_test:
            try:
                # Read frame
                ret, frame = self.cap.read()
                
                if not ret or frame is None:
                    time.sleep(0.03)
                    continue
                
                self.frame_count += 1
                
                # Face detection
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                faces = self.face_cascade.detectMultiScale(gray, 1.1, 5, minSize=(80, 80))
                num_faces = len(faces)
                
                # Draw faces
                for (x, y, w, h) in faces:
                    cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 255, 0), 2)
                    cv2.putText(frame, 'Face', (x, y-10),
                               cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
                
                # Mobile phone detection
                phone_bbox = self.detect_mobile_phone(frame, faces)
                
                if phone_bbox:
                    x, y, w, h = phone_bbox
                    cv2.rectangle(frame, (x, y), (x+w, y+h), (0, 0, 255), 3)
                    cv2.putText(frame, 'MOBILE!', (x, y-10),
                               cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 255), 2)
                    
                    self.mobile_detected_frames += 1
                    logger.info(f"⚠️ Mobile: {self.mobile_detected_frames}/{self.mobile_detection_threshold}")
                    
                    cv2.putText(frame, f'Mobile: {self.mobile_detected_frames}/3', 
                               (10, 120), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 255), 2)
                    
                    if self.mobile_detected_frames >= self.mobile_detection_threshold:
                        self.add_violation('mobile_phone', 'Mobile phone detected during test')
                        
                        cv2.putText(frame, '!!! TEST TERMINATED - MOBILE PHONE !!!', 
                                   (50, frame.shape[0]//2), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 3)
                else:
                    self.mobile_detected_frames = max(0, self.mobile_detected_frames - 1)
                
                # Multiple faces detection
                if num_faces > 1:
                    self.multiple_face_frames += 1
                    cv2.putText(frame, f'{num_faces} PEOPLE! {self.multiple_face_frames}/3', 
                               (10, 90), cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 0, 255), 2)
                    
                    logger.info(f"⚠️ Multiple faces: {self.multiple_face_frames}/3")
                    
                    if self.multiple_face_frames >= self.multiple_face_threshold:
                        self.add_violation('multiple_faces', f'{num_faces} people detected during test')
                        
                        cv2.putText(frame, '!!! TEST TERMINATED - MULTIPLE PEOPLE !!!', 
                                   (50, frame.shape[0]//2 + 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 3)
                else:
                    self.multiple_face_frames = max(0, self.multiple_face_frames - 1)
                
                # Status overlay
                cv2.putText(frame, f'Faces: {num_faces}', (10, 30), 
                           cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
                cv2.putText(frame, datetime.now().strftime("%H:%M:%S"), 
                           (frame.shape[1] - 150, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)
                
                # Update current frame
                with self.frame_lock:
                    self.current_frame = frame.copy()
                
                time.sleep(0.03)
                
            except Exception as e:
                logger.error(f"Error: {e}")
                time.sleep(0.1)
        
        # Cleanup
        if self.cap:
            self.cap.release()
            self.cap = None
        self.is_monitoring = False
        
        # Send termination to backend if test was terminated
        if self.terminate_test and self.violations:
            self.send_termination_to_backend()
        
        logger.info(f"Stopped monitoring for test {self.assignment_id}")
    
    def send_termination_to_backend(self):
        """Send termination notice to backend"""
        try:
            numeric_id = re.sub(r'[^0-9]', '', self.assignment_id)
            
            violation_data = {
                'assignment_id': self.assignment_id,
                'numeric_id': numeric_id,
                'user_id': self.user_id,
                'test_name': self.test_name,
                'terminated': True,
                'termination_reason': self.termination_reason or 'Proctoring violation',
                'violations': self.violations,
                'completed_at': datetime.now().isoformat()
            }
            
            response = requests.post(
                f"{BACKEND_URL}/aptitude-tests/{numeric_id}/violations",
                json=violation_data,
                timeout=3
            )
            
            if response.status_code == 200:
                logger.info(f"✅ Termination sent to backend for test {self.assignment_id}")
            else:
                logger.error(f"❌ Failed to send termination: {response.status_code}")
                
        except Exception as e:
            logger.error(f"❌ Error sending termination: {e}")
    
    def get_current_frame(self):
        """Get current frame"""
        with self.frame_lock:
            if self.current_frame is not None:
                return self.current_frame.copy()
        return None
    
    def stop_monitoring(self):
        """Stop monitoring"""
        self.test_completed = True
        self.is_monitoring = False
        if self.cap:
            self.cap.release()
            self.cap = None
        logger.info(f"✅ Stopped monitoring")

# Flask Routes
@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'running'})

@app.route('/check-camera', methods=['POST'])
def check_camera():
    try:
        if platform.system() == 'Windows':
            cap = cv2.VideoCapture(0, cv2.CAP_DSHOW)
        else:
            cap = cv2.VideoCapture(0)
        
        if cap.isOpened():
            ret, frame = cap.read()
            cap.release()
            if ret and frame is not None:
                return jsonify({'available': True})
        return jsonify({'available': False})
    except:
        return jsonify({'available': False})

@app.route('/start-test', methods=['POST'])
def start_test():
    data = request.json
    assignment_id = data.get('assignmentId')
    user_id = data.get('userId')
    test_name = data.get('testName', 'Test')
    
    with session_lock:
        if assignment_id in active_sessions:
            active_sessions[assignment_id].stop_monitoring()
            del active_sessions[assignment_id]
        
        proctor = Proctor(assignment_id, user_id, test_name)
        active_sessions[assignment_id] = proctor
        
        thread = threading.Thread(target=proctor.start_monitoring)
        thread.daemon = True
        thread.start()
        time.sleep(2)
    
    return jsonify({'message': 'Test started'})

@app.route('/stop-test/<assignment_id>', methods=['POST'])
def stop_test(assignment_id):
    with session_lock:
        if assignment_id in active_sessions:
            active_sessions[assignment_id].stop_monitoring()
            del active_sessions[assignment_id]
            return jsonify({'message': 'Test stopped'})
    return jsonify({'error': 'No active test'}), 404

@app.route('/test-status/<assignment_id>', methods=['GET'])
def test_status(assignment_id):
    with session_lock:
        if assignment_id in active_sessions:
            proctor = active_sessions[assignment_id]
            return jsonify({
                'active': True,
                'shouldTerminate': proctor.terminate_test,
                'termination_reason': proctor.termination_reason,
                'face_detected': proctor.frame_count > 0,
                'mobile_detected': proctor.mobile_detected_frames > 0,
                'mobile_frames': proctor.mobile_detected_frames,
                'multiple_faces_frames': proctor.multiple_face_frames,
                'violations': proctor.violations
            })
    return jsonify({'active': False})

@app.route('/report-violation/<assignment_id>', methods=['POST'])
def report_violation(assignment_id):
    """Endpoint for frontend to report violations (tab switch, key combinations)"""
    data = request.json
    violation_type = data.get('type')
    message = data.get('message')
    
    with session_lock:
        if assignment_id in active_sessions:
            proctor = active_sessions[assignment_id]
            proctor.add_violation(violation_type, message, critical=True)
            return jsonify({'terminated': True, 'reason': message})
    
    return jsonify({'error': 'No active session'}), 404

@app.route('/video-feed/<assignment_id>')
def video_feed(assignment_id):
    def generate():
        proctor = None
        with session_lock:
            if assignment_id in active_sessions:
                proctor = active_sessions[assignment_id]
        
        if not proctor:
            return
        
        while proctor.is_monitoring and not proctor.test_completed and not proctor.terminate_test:
            frame = proctor.get_current_frame()
            if frame is not None:
                ret, jpeg = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 70])
                if ret:
                    yield (b'--frame\r\n'
                           b'Content-Type: image/jpeg\r\n\r\n' + 
                           jpeg.tobytes() + b'\r\n')
            time.sleep(0.05)
    
    return Response(generate(),
                   mimetype='multipart/x-mixed-replace; boundary=frame')

if __name__ == '__main__':
    logger.info("=" * 50)
    logger.info("Starting Camera Proctoring Service")
    logger.info("=" * 50)
    logger.info("Port: 5001")
    logger.info("Mobile detection: 3 frames")
    logger.info("Multiple faces: 3 frames")
    logger.info("Tab switch: Instant termination")
    logger.info("Key combinations: Instant termination")
    logger.info("=" * 50)
    app.run(host='0.0.0.0', port=5001, debug=True, threaded=True)