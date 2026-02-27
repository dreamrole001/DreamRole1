@echo off
echo ========================================
echo JOB RECOMMENDATION SYSTEM - START ALL SERVICES
echo ========================================
echo.

echo [1/3] Starting Proctoring Service (Port 5001)
echo ========================================
cd /d C:\Users\satej\job-recommendation-system007\camera-proctoring

if not exist venv (
    echo Creating virtual environment...
    python -m venv venv
)

call .\venv\Scripts\activate.bat

echo Installing required packages...
pip install opencv-python flask flask-cors requests > nul

echo Testing camera...
python -c "import cv2; cap=cv2.VideoCapture(0); print('✅ Camera OK' if cap.isOpened() else '❌ Camera Failed'); cap.release()"

echo.
echo Starting proctoring service...
echo This window will show the camera feed logs
echo Press Ctrl+C to stop
echo.

start "Proctoring Service" cmd /k "cd /d C:\Users\satej\job-recommendation-system007\camera-proctoring && .\venv\Scripts\activate.bat && python app.py"

timeout /t 3

echo.
echo [2/3] Starting Backend (Port 8080)
echo ========================================
start "Backend Service" cmd /k "cd /d C:\Users\satej\job-recommendation-system007\job-recommendation-backend && .\mvnw.cmd spring-boot:run"

timeout /t 10

echo.
echo [3/3] Starting Frontend (Port 3000)
echo ========================================
start "Frontend Service" cmd /k "cd /d C:\Users\satej\job-recommendation-system007\frontend && npm run dev"

echo.
echo ========================================
echo ALL SERVICES STARTED!
echo ========================================
echo.
echo Proctoring: http://localhost:5001
echo Backend:    http://localhost:8080
echo Frontend:   http://localhost:3000
echo.
echo Open http://localhost:3000 in your browser
echo.
pause