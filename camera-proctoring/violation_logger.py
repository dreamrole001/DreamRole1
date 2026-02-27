import json
import os
import cv2
from datetime import datetime
import threading

class ViolationLogger:
    def __init__(self, base_path='violations'):
        self.base_path = base_path
        self.log_lock = threading.Lock()
        
        # Create directories
        os.makedirs(base_path, exist_ok=True)
        os.makedirs(os.path.join(base_path, 'images'), exist_ok=True)
        os.makedirs(os.path.join(base_path, 'logs'), exist_ok=True)
    
    def log_violation(self, assignment_id, violation_type, message, critical=False, frame=None):
        """Log a violation with timestamp"""
        with self.log_lock:
            timestamp = datetime.now().isoformat()
            
            # Create log entry
            log_entry = {
                'assignment_id': assignment_id,
                'type': violation_type,
                'message': message,
                'critical': critical,
                'timestamp': timestamp
            }
            
            # Save to JSON log file
            log_file = os.path.join(self.base_path, 'logs', f'{assignment_id}_violations.json')
            
            # Read existing logs
            existing_logs = []
            if os.path.exists(log_file):
                try:
                    with open(log_file, 'r') as f:
                        existing_logs = json.load(f)
                except:
                    existing_logs = []
            
            # Append new log
            existing_logs.append(log_entry)
            
            # Write back
            with open(log_file, 'w') as f:
                json.dump(existing_logs, f, indent=2)
            
            # Save frame if provided
            if frame is not None:
                self.save_violation_image(assignment_id, violation_type, timestamp, frame)
    
    def log_event(self, assignment_id, event_type, message):
        """Log a general event"""
        with self.log_lock:
            timestamp = datetime.now().isoformat()
            
            log_entry = {
                'assignment_id': assignment_id,
                'type': event_type,
                'message': message,
                'timestamp': timestamp
            }
            
            # Save to event log
            event_file = os.path.join(self.base_path, 'logs', f'{assignment_id}_events.json')
            
            existing_events = []
            if os.path.exists(event_file):
                try:
                    with open(event_file, 'r') as f:
                        existing_events = json.load(f)
                except:
                    existing_events = []
            
            existing_events.append(log_entry)
            
            with open(event_file, 'w') as f:
                json.dump(existing_events, f, indent=2)
    
    def save_violation_image(self, assignment_id, violation_type, timestamp, frame):
        """Save frame with violation"""
        try:
            # Create filename
            safe_timestamp = timestamp.replace(':', '-').replace('.', '-')
            filename = f"{assignment_id}_{violation_type}_{safe_timestamp}.jpg"
            filepath = os.path.join(self.base_path, 'images', filename)
            
            # Save image
            cv2.imwrite(filepath, frame)
            print(f"📸 Saved violation image: {filename}")
            
            return filepath
        except Exception as e:
            print(f"❌ Error saving violation image: {e}")
            return None
    
    def get_violations(self, assignment_id):
        """Get all violations for an assignment"""
        log_file = os.path.join(self.base_path, 'logs', f'{assignment_id}_violations.json')
        
        if os.path.exists(log_file):
            try:
                with open(log_file, 'r') as f:
                    return json.load(f)
            except:
                return []
        return []
    
    def get_violation_summary(self, assignment_id):
        """Get summary of violations"""
        violations = self.get_violations(assignment_id)
        
        summary = {
            'total_violations': len(violations),
            'by_type': {},
            'critical_violations': 0,
            'timeline': []
        }
        
        for v in violations:
            v_type = v['type']
            summary['by_type'][v_type] = summary['by_type'].get(v_type, 0) + 1
            
            if v.get('critical', False):
                summary['critical_violations'] += 1
            
            summary['timeline'].append({
                'time': v['timestamp'],
                'type': v['type']
            })
        
        return summary
    
    def export_violation_report(self, assignment_id, user_id, test_name):
        """Generate comprehensive violation report"""
        violations = self.get_violations(assignment_id)
        
        report = {
            'assignment_id': assignment_id,
            'user_id': user_id,
            'test_name': test_name,
            'generated_at': datetime.now().isoformat(),
            'total_violations': len(violations),
            'violation_summary': {},
            'detailed_violations': violations
        }
        
        # Summarize by type
        for v in violations:
            v_type = v['type']
            report['violation_summary'][v_type] = \
                report['violation_summary'].get(v_type, 0) + 1
        
        return report
    
    def cleanup_old_logs(self, days=7):
        """Delete logs older than specified days"""
        import time
        
        now = time.time()
        cutoff = now - (days * 24 * 3600)
        
        # Clean up log files
        log_dir = os.path.join(self.base_path, 'logs')
        for filename in os.listdir(log_dir):
            filepath = os.path.join(log_dir, filename)
            if os.path.getmtime(filepath) < cutoff:
                os.remove(filepath)
        
        # Clean up images
        image_dir = os.path.join(self.base_path, 'images')
        for filename in os.listdir(image_dir):
            filepath = os.path.join(image_dir, filename)
            if os.path.getmtime(filepath) < cutoff:
                os.remove(filepath)