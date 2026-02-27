import cv2
import platform

print("=" * 50)
print("Camera Detection Test")
print("=" * 50)
print(f"System: {platform.system()}")
print(f"OpenCV Version: {cv2.__version__}")
print("=" * 50)

# Try different camera indices
for camera_index in range(5):
    print(f"\nTesting camera index {camera_index}...")
    
    # Try different backends on Windows
    if platform.system() == 'Windows':
        backends = [
            (cv2.CAP_DSHOW, "DirectShow"),
            (cv2.CAP_MSMF, "Media Foundation"),
            (cv2.CAP_VFW, "VFW"),
            (cv2.CAP_ANY, "Default")
        ]
    else:
        backends = [(cv2.CAP_ANY, "Default")]
    
    for backend, backend_name in backends:
        try:
            cap = cv2.VideoCapture(camera_index, backend)
            
            if cap.isOpened():
                print(f"  ✅ {backend_name}: Camera opened")
                
                # Try to read a frame
                ret, frame = cap.read()
                if ret and frame is not None:
                    print(f"     Frame captured: {frame.shape}")
                    
                    # Save test image
                    filename = f"test_camera_{camera_index}_{backend_name}.jpg"
                    cv2.imwrite(filename, frame)
                    print(f"     Image saved: {filename}")
                else:
                    print(f"     ❌ Could not read frame")
                
                cap.release()
            else:
                print(f"  ❌ {backend_name}: Could not open")
                
        except Exception as e:
            print(f"  ❌ {backend_name}: Error - {e}")

print("\n" + "=" * 50)
print("Camera detection complete")
print("=" * 50)