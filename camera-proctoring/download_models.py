import os
import urllib.request
import ssl

def download_file(url, filename):
    """Download file with progress indicator"""
    print(f"Downloading {filename}...")
    
    # Create SSL context that doesn't verify (for some HTTPS sites)
    ssl_context = ssl.create_default_context()
    ssl_context.check_hostname = False
    ssl_context.verify_mode = ssl.CERT_NONE
    
    def report_progress(block_num, block_size, total_size):
        downloaded = block_num * block_size
        if total_size > 0:
            percent = min(100, downloaded * 100 // total_size)
            print(f"\rProgress: {percent}%", end='', flush=True)
    
    urllib.request.urlretrieve(url, filename, reporthook=report_progress)
    print("\nDownload complete!")

def main():
    """Download pre-trained models for face and mobile detection"""
    print("=" * 60)
    print("Downloading Models for Camera Proctoring System")
    print("=" * 60)
    
    # Create directories
    os.makedirs('models/face_detection_model', exist_ok=True)
    os.makedirs('models/mobile_detection_model', exist_ok=True)
    
    # Face detection models
    print("\n1. Downloading Face Detection Models...")
    
    face_urls = [
        ('https://github.com/opencv/opencv_3rdparty/raw/dnn_samples_face_detector_20170830/opencv_face_detector_uint8.pb',
         'models/face_detection_model/opencv_face_detector_uint8.pb'),
        ('https://raw.githubusercontent.com/opencv/opencv/master/samples/dnn/face_detector/opencv_face_detector.pbtxt',
         'models/face_detection_model/opencv_face_detector.pbtxt')
    ]
    
    for url, filename in face_urls:
        if not os.path.exists(filename):
            download_file(url, filename)
        else:
            print(f"✓ {filename} already exists")
    
    # Mobile detection models (MobileNet SSD)
    print("\n2. Downloading Mobile Detection Models...")
    
    mobile_urls = [
        ('https://github.com/chuanqi305/MobileNet-SSD/raw/master/MobileNetSSD_deploy.caffemodel',
         'models/mobile_detection_model/MobileNetSSD_deploy.caffemodel'),
        ('https://github.com/chuanqi305/MobileNet-SSD/raw/master/MobileNetSSD_deploy.prototxt',
         'models/mobile_detection_model/MobileNetSSD_deploy.prototxt'),
        ('https://github.com/pjreddie/darknet/raw/master/data/coco.names',
         'models/mobile_detection_model/coco.names')
    ]
    
    for url, filename in mobile_urls:
        if not os.path.exists(filename):
            download_file(url, filename)
        else:
            print(f"✓ {filename} already exists")
    
    print("\n" + "=" * 60)
    print("✅ All models downloaded successfully!")
    print("=" * 60)

if __name__ == '__main__':
    main()