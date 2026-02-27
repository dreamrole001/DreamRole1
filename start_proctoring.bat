@echo off
echo ========================================
echo Camera Proctoring Service - Quick Start
echo ========================================
echo.

cd /d C:\Users\satej\job-recommendation-system007\camera-proctoring

echo [1/4] Checking virtual environment...
if not exist venv (
    echo Creating virtual environment...
    python -m venv venv
) else (
    echo Virtual environment exists
)

echo.
echo [2/4] Activating virtual environment...
call .\venv\Scripts\activate.bat

echo.
echo [3/4] Installing/updating packages...
pip install --upgrade pip
pip install opencv-python flask flask-cors requests

echo.
echo [4/4] Testing camera...
python -c "import cv2; cap=cv2.VideoCapture(0); print('✅ Camera OK' if cap.isOpened() else '❌ Camera Failed'); cap.release()"

echo.
echo ========================================
echo Starting proctoring service on port 5001...
echo ========================================
echo.
echo Press Ctrl+C to stop the service
echo.

python app.py

pause