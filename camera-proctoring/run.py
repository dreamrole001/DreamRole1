# run.py
import subprocess
import sys
import os

def main():
    print("=" * 60)
    print("Starting Camera Proctoring Service")
    print("=" * 60)
    
    # Check if we're in virtual environment
    if not hasattr(sys, 'real_prefix') and not sys.base_prefix != sys.prefix:
        print("⚠️  Not running in virtual environment. Please activate venv first.")
        print("Run: .\\venv\\Scripts\\Activate")
        return
    
    # Check if OpenCV is installed
    try:
        import cv2
        print(f"✅ OpenCV version: {cv2.__version__}")
    except ImportError:
        print("❌ OpenCV not installed. Installing...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", "opencv-python"])
        subprocess.check_call([sys.executable, "-m", "pip", "install", "opencv-contrib-python"])
    
    # Check if Flask is installed
    try:
        import flask
        print(f"✅ Flask version: {flask.__version__}")
    except ImportError:
        print("❌ Flask not installed. Installing...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", "flask", "flask-cors"])
    
    # Check if requests is installed
    try:
        import requests
        print(f"✅ Requests version: {requests.__version__}")
    except ImportError:
        print("❌ Requests not installed. Installing...")
        subprocess.check_call([sys.executable, "-m", "pip", "install", "requests"])
    
    print("\n" + "=" * 60)
    print("Starting proctoring service on port 5001...")
    print("=" * 60)
    print("\nPress Ctrl+C to stop\n")
    
    # Run the app
    import app
    app.app.run(host='0.0.0.0', port=5001, debug=True, threaded=True)

if __name__ == '__main__':
    main()