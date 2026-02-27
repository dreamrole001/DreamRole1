# install_proctoring.ps1
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Camera Proctoring Service - Complete Installation" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# Check current directory
Write-Host "`nCurrent Directory: $(Get-Location)" -ForegroundColor Yellow

# Check Python version
$pythonVersion = python --version 2>&1
Write-Host "Python: $pythonVersion" -ForegroundColor Yellow

# Step 1: Create virtual environment
Write-Host "`n[1/8] Creating virtual environment..." -ForegroundColor Green
python -m venv venv
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to create virtual environment" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Virtual environment created" -ForegroundColor Green

# Step 2: Activate virtual environment
Write-Host "`n[2/8] Activating virtual environment..." -ForegroundColor Green
& .\venv\Scripts\Activate
if ($?) {
    Write-Host "✅ Virtual environment activated" -ForegroundColor Green
} else {
    Write-Host "❌ Failed to activate virtual environment" -ForegroundColor Red
    exit 1
}

# Step 3: Upgrade pip
Write-Host "`n[3/8] Upgrading pip..." -ForegroundColor Green
python -m pip install --upgrade pip
Write-Host "✅ Pip upgraded" -ForegroundColor Green

# Step 4: Install setuptools and wheel
Write-Host "`n[4/8] Installing setuptools and wheel..." -ForegroundColor Green
python -m pip install setuptools wheel
Write-Host "✅ Setuptools and wheel installed" -ForegroundColor Green

# Step 5: Install numpy (critical step)
Write-Host "`n[5/8] Installing numpy 1.26.3..." -ForegroundColor Green
python -m pip install numpy==1.26.3
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Failed to install numpy" -ForegroundColor Red
    exit 1
}
Write-Host "✅ NumPy installed successfully" -ForegroundColor Green

# Step 6: Install opencv
Write-Host "`n[6/8] Installing OpenCV..." -ForegroundColor Green
python -m pip install opencv-python==4.9.0.80
python -m pip install opencv-contrib-python==4.9.0.80
Write-Host "✅ OpenCV installed" -ForegroundColor Green

# Step 7: Install remaining packages
Write-Host "`n[7/8] Installing remaining packages..." -ForegroundColor Green
python -m pip install Flask==2.3.3
python -m pip install Flask-CORS==4.0.0
python -m pip install requests==2.31.0
python -m pip install Pillow==10.2.0
Write-Host "✅ All packages installed" -ForegroundColor Green

# Step 8: Download models
Write-Host "`n[8/8] Downloading AI models..." -ForegroundColor Green
python download_models.py
Write-Host "✅ Models downloaded" -ForegroundColor Green

# Verify installation
Write-Host "`n==================================================" -ForegroundColor Cyan
Write-Host "Verifying Installation..." -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

python -c "import numpy; print('NumPy:', numpy.__version__)"
python -c "import cv2; print('OpenCV:', cv2.__version__)"
python -c "import flask; print('Flask:', flask.__version__)"

Write-Host "`n==================================================" -ForegroundColor Green
Write-Host "✅ INSTALLATION COMPLETE!" -ForegroundColor Green
Write-Host "==================================================" -ForegroundColor Green
Write-Host "`nTo start the proctoring service:" -ForegroundColor Yellow
Write-Host "1. .\venv\Scripts\Activate" -ForegroundColor White
Write-Host "2. python app.py" -ForegroundColor White
Write-Host "`nThe service will run on: http://localhost:5001" -ForegroundColor Cyan

Read-Host "`nPress Enter to exit"