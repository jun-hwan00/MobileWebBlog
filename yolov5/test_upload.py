"""
Django 서버로 이미지 업로드 테스트 스크립트
"""
import requests
import cv2
import numpy as np
from datetime import datetime

# 설정
HOST = 'http://127.0.0.1:8000'
username = 'admin'
password = 'admin'

print("=" * 50)
print("Django server image upload test")
print("=" * 50)

# 1. 토큰 발급
print("\n1. Token authentication...")
try:
    res = requests.post(HOST + '/api-token-auth/', {
        'username': username,
        'password': password,
    })
    res.raise_for_status()
    token = res.json()['token']
    print(f"[OK] Token obtained: {token[:20]}...")
except Exception as e:
    print(f"[ERROR] Token failed: {e}")
    exit(1)

# 2. 테스트 이미지 생성
print("\n2. Creating test image...")
img = np.zeros((240, 320, 3), dtype=np.uint8)
img[:, :] = (100, 150, 200)  # BGR 색상
cv2.putText(img, 'Test Image', (50, 120), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)
cv2.imwrite('test_image.jpg', img)
print("[OK] Test image created: test_image.jpg")

# 3. Django 서버로 전송
print("\n3. Sending to Django server...")
headers = {
    'Authorization': 'Token ' + token,
}

now = datetime.now()
data = {
    'title': 'Test Upload',
    'text': 'This is a test upload',
    'author': 1
}

files = {'image': open('test_image.jpg', 'rb')}

try:
    res = requests.post(HOST + '/api_root/Post/', data=data, files=files, headers=headers)
    print(f"[OK] Server response code: {res.status_code}")

    if res.status_code == 201:
        print("[OK] Image upload success!")
        print(f"[OK] Response data: {res.json()}")
    else:
        print(f"[ERROR] Upload failed")
        print(f"[ERROR] Error message: {res.text}")
except Exception as e:
    print(f"[ERROR] Send failed: {e}")

print("\n" + "=" * 50)
print("Test completed!")
print(f"Check browser: {HOST}/api_root/Post/")
print("=" * 50)
