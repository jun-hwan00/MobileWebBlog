# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

이 프로젝트는 **YOLO 객체 검출 시스템**과 **Django REST API**, **Android 클라이언트**를 연동한 침입자 감시 시스템입니다.

- **PhotoBlogServer**: Django 5.2.7 기반 REST API 백엔드 (VSCode에서 개발)
- **PhotoViewer**: Android 클라이언트 (Android Studio에서 개발)
- **YOLOv5**: 객체 검출 Edge 클라이언트 (VSCode에서 개발)

**시스템 동작 흐름:**
1. YOLOv5가 웹캠에서 객체(사람, 물체 등)를 실시간 감지
2. 새로운 객체가 출현하면 이미지를 Django 서버로 전송
3. Android 앱에서 감지된 이미지 확인

## Django 서버 구조

```
PhotoBlogServer/
├── manage.py              # Django 관리 스크립트
├── db.sqlite3             # SQLite 데이터베이스
├── media/                 # 업로드된 이미지 파일 (blog_image/YYYY/MM/DD/)
├── mysite/                # Django 프로젝트 설정
│   ├── settings.py        # 전역 설정 (ALLOWED_HOSTS, INSTALLED_APPS 등)
│   └── urls.py            # 루트 URL 라우팅
└── blog/                  # 메인 Django 앱
    ├── models.py          # Post 모델 (author, title, text, image, dates)
    ├── views.py           # BlogImages ViewSet + post_list 웹뷰
    ├── serializers.py     # PostSerializer (REST API 직렬화)
    ├── urls.py            # 앱 URL 라우팅 (/api_root/Post/)
    ├── admin.py           # Django admin 등록
    └── migrations/        # DB 마이그레이션 파일
```

## 주요 기술 스택

- **Django 5.2.7**: 웹 프레임워크
- **Django REST Framework 3.16.1**: REST API 구현
- **SQLite**: 데이터베이스
- **Pillow 12.0.0**: 이미지 처리
- **Token Authentication**: Android 클라이언트 인증

## 데이터 모델

### Post Model (blog/models.py)

```python
class Post(models.Model):
    author = models.ForeignKey(User, on_delete=models.CASCADE)
    title = models.CharField(max_length=200, blank=True, default='')
    text = models.TextField(blank=True, default='')
    created_date = models.DateTimeField(auto_now_add=True)
    published_date = models.DateTimeField(auto_now=True)
    image = models.ImageField(upload_to='blog_image/%Y/%m/%d/')
```

- **author**: Django User 모델 외래키
- **title, text**: 선택사항 (빈 값 허용)
- **image**: 필수 필드, `media/blog_image/YYYY/MM/DD/` 경로에 자동 저장
- **created_date**: 생성 시 자동 설정
- **published_date**: 수정 시마다 자동 업데이트

## REST API 엔드포인트

### 기본 URL 구조

- `/admin/` - Django 관리자 페이지
- `/` - 웹 블로그 리스트 뷰 (post_list.html)
- `/api_root/Post/` - REST API 엔드포인트

### BlogImages ViewSet (blog/views.py)

```python
class BlogImages(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer
```

**자동 생성되는 CRUD 엔드포인트:**
- `GET /api_root/Post/` - 모든 게시물 목록 조회
- `POST /api_root/Post/` - 새 게시물 생성 (multipart/form-data)
- `GET /api_root/Post/{id}/` - 특정 게시물 조회
- `PUT /api_root/Post/{id}/` - 게시물 수정
- `DELETE /api_root/Post/{id}/` - 게시물 삭제

## 개발 서버 실행

### 가상환경 활성화 및 서버 시작

```bash
# Windows
cd PhotoBlogServer
venv\Scripts\activate
python manage.py runserver

# 외부 접속 허용 (Android 에뮬레이터용)
python manage.py runserver 0.0.0.0:8000
```

서버 주소:
- 로컬: `http://127.0.0.1:8000`
- Android 에뮬레이터: `http://10.0.2.2:8000` (에뮬레이터 게이트웨이)

## 주요 Django 명령어

```bash
# 마이그레이션
python manage.py makemigrations    # 모델 변경사항을 마이그레이션 파일로 생성
python manage.py migrate           # 마이그레이션 적용

# 관리자 계정
python manage.py createsuperuser   # 관리자 계정 생성

# Django Shell
python manage.py shell             # 대화형 Python 쉘
python manage.py dbshell           # 데이터베이스 쉘

# 서버 실행
python manage.py runserver         # 개발 서버 시작 (포트 8000)
```

## 중요 설정 (mysite/settings.py)

### ALLOWED_HOSTS

```python
ALLOWED_HOSTS = ['127.0.0.1', '10.0.2.2', '.pythonanywhere.com']
```

- `127.0.0.1`: 로컬호스트
- `10.0.2.2`: Android 에뮬레이터 기본 게이트웨이
- `.pythonanywhere.com`: 프로덕션 배포용

### INSTALLED_APPS

```python
INSTALLED_APPS = [
    # Django 기본 앱
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.sessions',
    'django.contrib.messages',
    'django.contrib.staticfiles',

    # 외부 패키지
    'rest_framework',          # DRF
    'rest_framework.authtoken', # 토큰 인증

    # 커스텀 앱
    'blog',
]
```

### Media Files

```python
MEDIA_URL = '/media/'
MEDIA_ROOT = os.path.join(BASE_DIR, 'media')
```

업로드된 이미지는 `PhotoBlogServer/media/blog_image/YYYY/MM/DD/` 경로에 저장됩니다.

## Android 클라이언트 연동

Android PhotoViewer 앱은 다음과 같이 서버와 통신합니다:

1. **GET /api_root/Post/** - 서버에서 게시물 목록 받아오기
   - JSON 배열로 모든 Post 데이터 수신
   - 각 Post의 `image` URL에서 이미지 다운로드
   - RecyclerView에 표시

2. **POST /api_root/Post/** - 새 이미지 업로드
   - multipart/form-data 형식으로 전송
   - 필드: `author`, `title`, `text`, `image`
   - Token 인증 헤더 필요

3. **인증**
   - `Authorization: Token {token}` 헤더 사용
   - Token은 Django admin에서 생성 또는 코드로 하드코딩

## 모델 변경 시 주의사항

models.py를 수정한 후에는 반드시:

```bash
python manage.py makemigrations
python manage.py migrate
```

마이그레이션 파일은 `blog/migrations/`에 자동 생성되며, 데이터베이스 스키마를 버전 관리합니다.

현재 마이그레이션:
- `0001_initial.py` - Post 모델 초기 생성
- `0002_alter_post_text_alter_post_title.py` - title, text 필드를 선택사항으로 변경

## API 응답 예시

### GET /api_root/Post/

```json
[
  {
    "id": 1,
    "author": 1,
    "title": "제목",
    "text": "설명",
    "created_date": "2024-11-17T10:30:00Z",
    "published_date": "2024-11-17T15:45:00Z",
    "image": "http://127.0.0.1:8000/media/blog_image/2024/11/17/photo.jpg"
  }
]
```

- `image` 필드는 전체 URL로 반환됨
- DateTime은 ISO 8601 형식
- 모든 모델 필드가 포함됨

## 새 기능 추가 시 권장 사항

1. **모델 변경**: `blog/models.py` 수정 → makemigrations → migrate
2. **Serializer 추가**: `blog/serializers.py`에 새 Serializer 클래스 생성
3. **ViewSet/View 추가**: `blog/views.py`에 ViewSet 또는 함수 뷰 작성
4. **URL 라우팅**: `blog/urls.py`에 라우터 등록 또는 path 추가
5. **Admin 등록**: 필요시 `blog/admin.py`에 모델 등록
6. **테스트**: `blog/tests.py`에 테스트 케이스 작성

## 보안 주의사항 (개발 환경)

현재 설정은 **개발용**이며, 프로덕션 배포 전 다음 사항을 변경해야 합니다:

- `DEBUG = False` 설정
- `SECRET_KEY`를 환경 변수로 분리
- ALLOWED_HOSTS 제한적으로 설정
- HTTPS 사용
- 데이터베이스를 PostgreSQL로 변경
- Static/Media 파일을 CDN/클라우드 스토리지로 이동

## YOLOv5 객체 검출 클라이언트

### 구조

```
yolov5/
├── detect.py             # YOLO 객체 검출 메인 스크립트 (수정됨)
├── changedetection.py    # Django 서버 연동 모듈 (신규 추가)
├── requirements.txt      # Python 패키지 의존성
├── venv/                 # Python 가상환경
├── .vscode/
│   └── launch.json       # VSCode 디버깅 설정
├── data/
│   └── coco.yaml         # COCO 데이터셋 클래스 정의 (80개 객체)
└── runs/detect/
    └── detected/         # 감지된 이미지 저장 경로
        └── YYYY/MM/DD/   # 날짜별 디렉토리
```

### 주요 기능

**1. 객체 검출 (detect.py)**
- YOLOv5 모델을 사용한 실시간 객체 검출
- 80가지 객체 클래스 지원 (사람, 자동차, 동물 등)
- 웹캠 (`--source 0`) 또는 비디오 파일 입력

**2. 변화 감지 (changedetection.py)**
- 객체 출현 감지: 이전 프레임 '0' → 현재 프레임 '1'
- 서버 과부하 방지: 모든 프레임이 아닌 변화 시점만 전송
- Django Token 인증

**3. 서버 전송**
- 감지된 이미지를 320x240으로 리사이즈
- POST /api_root/Post/ 엔드포인트로 전송
- 이미지는 날짜별 디렉토리에 저장

### 설치 및 실행

```bash
# 1. YOLOv5 디렉토리로 이동
cd yolov5

# 2. 가상환경 활성화 (Windows)
venv\Scripts\activate

# 3. YOLO 실행 (웹캠 사용)
python detect.py --source 0

# 4. 특정 비디오 파일 사용
python detect.py --source video.mp4
```

### changedetection.py 주요 설정

```python
HOST = 'http://127.0.0.1:8000'
username = 'admin'
password = 'admin'  # Django admin 비밀번호
```

**동작 원리:**
1. `__init__()`: Django 서버에서 Token 발급
2. `add()`: 객체 출현 변화 감지
3. `send()`: 이미지를 Django 서버로 POST

### detect.py 수정 사항

```python
# 라인 48: changedetection import 추가
from changedetection import ChangeDetection

# 라인 172: ChangeDetection 초기화
cd = ChangeDetection(names)

# 라인 220: detected 배열 초기화
detected = [0 for i in range(len(names))]

# 라인 263: 검출된 객체 클래스 기록
detected[int(cls)] = 1

# 라인 291: 서버로 전송
cd.add(names, detected, save_dir, im0)
```

### COCO 데이터셋 클래스 (80개)

주요 객체:
- **person** (사람) - 침입자 감지에 주로 사용
- car, truck, bus - 차량
- cat, dog - 애완동물
- bottle, cup, chair - 일반 사물
- laptop, cell phone, keyboard - 전자기기

전체 목록은 `data/coco.yaml` 참조

### 실행 예시

```bash
# 웹캠에서 실시간 검출
python detect.py --source 0

# 신뢰도 임계값 조정
python detect.py --source 0 --conf-thres 0.5

# 특정 클래스만 검출 (사람만)
python detect.py --source 0 --classes 0
```

### 문제 해결

**1. Token 인증 실패**
- Django 서버가 실행 중인지 확인
- changedetection.py의 username/password 확인
- `/api-token-auth/` 엔드포인트 정상 동작 확인

**2. 웹캠 접근 실패**
- 다른 프로그램이 웹캠을 사용 중인지 확인
- `--source 1` 또는 다른 숫자로 변경

**3. 이미지 전송 실패**
- Django 서버 로그 확인
- author ID가 올바른지 확인 (기본값: 1)

## Git 저장소 구조

```
MobileWebProject/
├── PhotoBlogServer/    # Django 서버 (VSCode에서 작업)
├── PhotoViewer/        # Android 클라이언트 (Android Studio에서 작업)
└── yolov5/             # YOLO 객체 검출 클라이언트 (VSCode에서 작업)
```

Git 브랜치: `main`

최근 커밋:
- `636f835` - Reorganize: Django를 PhotoBlogServer로 이동, Android PhotoViewer 추가
- `42b8ee1` - Add Android PhotoViewer client
- `69f556b` - update
- `a28df9a` - initial commit
