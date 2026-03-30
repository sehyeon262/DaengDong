# 🐾 멍멍로드 (MeongMeong Road)

> 반려견과 함께하는 스마트 산책 앱 — AI 기반 경로 추천, 실시간 위치 추적, 소셜 산책 기능을 제공합니다.

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시스템 아키텍처](#시스템-아키텍처)
- [프로젝트 구조](#프로젝트-구조)
- [시작하기](#시작하기)
  - [사전 요구사항](#사전-요구사항)
  - [백엔드 실행](#백엔드-실행)
  - [프론트엔드 실행](#프론트엔드-실행)
  - [Docker 환경 실행](#docker-환경-실행)
- [API 명세](#api-명세)
- [데이터 모델](#데이터-모델)
- [외부 서비스 연동](#외부-서비스-연동)
- [CI/CD 파이프라인](#cicd-파이프라인)
- [개발 컨벤션](#개발-컨벤션)

---

## 프로젝트 소개

**멍멍로드**는 반려견 보호자를 위한 올인원 산책 앱입니다. GPS 기반 실시간 산책 추적, AI 경로 추천, 주변 반려견 매칭, 위험 구역 알림 등 안전하고 즐거운 산책 경험을 제공합니다.

Samsung Software Academy for Youth(SSAFY) 자율 프로젝트로 개발되었습니다.

---

## 주요 기능

### 산책 & 경로

| 기능 | 설명 |
|------|------|
| **실시간 GPS 추적** | 산책 중 GPS 좌표를 배치로 수집하여 경로를 기록합니다 (PostGIS LINESTRING) |
| **AI 경로 추천** | TMap API + 사용자 선호도 + ML 스코어링으로 맞춤 산책 코스 3개를 추천합니다 |
| **산책 기록** | 거리, 시간, 소모 칼로리, 사진을 포함한 상세 산책 기록을 제공합니다 |
| **캘린더 뷰** | 월별 산책 기록을 캘린더 형태로 확인할 수 있습니다 |

### 안전

| 기능 | 설명 |
|------|------|
| **위험 구역 알림** | 사용자 제보 기반 위험 구역 접근 시 실시간 경고 알림을 보냅니다 |
| **비선호 반려견 알림** | 산책 중 비선호 반려견 접근 시 알림을 제공합니다 |
| **날씨 기반 가이드** | 기온, 대기질 정보를 기반으로 산책 적합도를 판단합니다 |

### 소셜

| 기능 | 설명 |
|------|------|
| **주변 반려견 탐색** | 반경 내 산책 중인 반려견을 실시간으로 확인합니다 |
| **산책 제안** | 주변 반려견에게 함께 산책하기를 제안할 수 있습니다 |
| **실시간 채팅** | WebSocket(STOMP) 기반 실시간 메시지를 주고받을 수 있습니다 |
| **만난 반려견 기록** | 산책 중 만난 반려견을 기록하고 피드백을 남길 수 있습니다 |

### 반려견 프로필 & 관리

| 기능 | 설명 |
|------|------|
| **반려견 프로필** | 이름, 견종, 나이, 체중, 성격 태그를 관리합니다 |
| **체중 추적** | 반려견 체중 변화를 기록합니다 |
| **성격 태그** | 반려견 성격 특성을 태그로 관리합니다 |

### 장소 & 발자국

| 기능 | 설명 |
|------|------|
| **반려견 동반 장소** | PostGIS 기반 주변 반려견 동반 가능 장소를 검색합니다 |
| **발자국(스탬프)** | 방문한 장소에 스탬프를 찍어 수집합니다 |
| **장소 등록** | 새로운 반려견 동반 장소를 사진과 함께 등록합니다 |
| **히트맵** | 산책 발자국을 히트맵 형태로 시각화합니다 |

### 게임화 & AI

| 기능 | 설명 |
|------|------|
| **뱃지 시스템** | 9종의 성취 뱃지와 진행도 추적을 제공합니다 |
| **AI 산책 일기** | Google Vision + GMS AI로 산책 사진 기반 일기를 자동 생성합니다 |
| **반려견 감정 분석** | ONNX 모델을 통한 반려견 감정 분석을 수행합니다 |
| **캐릭터 시스템** | 산책 활동에 따라 성장하는 캐릭터를 제공합니다 |

### 워치 연동

| 기능 | 설명 |
|------|------|
| **웨어러블 제어** | 스마트워치에서 산책 시작/종료/일시정지를 제어합니다 |
| **코스 선택** | 워치에서 추천 코스를 선택할 수 있습니다 |
| **실시간 통계** | 산책 통계를 1초 간격으로 워치에 동기화합니다 |

---

## 기술 스택

### Backend

| 기술 | 버전 | 용도 |
|------|------|------|
| **Spring Boot** | 3.5.11 | REST API 서버 |
| **Java** | 17 | 서버 언어 |
| **PostgreSQL** | 15 | 관계형 데이터베이스 |
| **PostGIS** | 3.5 | 공간 데이터 처리 (좌표, 경로) |
| **Redis** | - | 캐싱 (산책 패턴, 사용자 선호도) + GPS 좌표 배치 저장 |
| **Spring WebSocket** | - | STOMP + SockJS 실시간 채팅 |
| **Spring Security + JWT** | - | 인증/인가 (Access + Refresh Token) |
| **AWS S3** | - | 이미지 파일 저장 |
| **ONNX Runtime** | 1.18.0 | 반려견 감정 분석 ML 추론 |

### Frontend (Android)

| 기술 | 버전 | 용도 |
|------|------|------|
| **Kotlin** | 2.0.21 | 앱 개발 언어 |
| **Jetpack Compose** | BOM 2024.09 | 선언적 UI |
| **Hilt** | 2.52 | 의존성 주입 |
| **Retrofit** | 2.9.0 | REST API 통신 |
| **OkHttp** | 4.12.0 | HTTP 클라이언트 + STOMP WebSocket |
| **Kakao Maps SDK** | 2.13.1 | 지도 (경로, 장소, 위험구역 표시) |
| **Google Play Location** | 21.2.0 | GPS 위치 서비스 |
| **DataStore** | 1.1.1 | 토큰/설정 로컬 저장 |
| **Coil** | 2.6.0 | 이미지 로딩 |
| **Firebase** | BOM 34.10 | 앱 배포 (App Distribution) |
| **Wearable API** | 18.2.0 | 스마트워치 연동 |

### Infrastructure

| 기술 | 용도 |
|------|------|
| **Docker** | 컨테이너화 (멀티스테이지 빌드) |
| **Docker Compose** | Local / Dev / Prod 환경 구성 |
| **Jenkins** | CI/CD 파이프라인 (GitLab 연동) |
| **Firebase App Distribution** | Android APK 배포 |

---

## 시스템 아키텍처

```
┌─────────────────────────────────────────────────────────┐
│                    Android Client                        │
│  Jetpack Compose + Kakao Maps + Wearable                │
└────────────┬──────────────┬──────────────┬──────────────┘
             │ REST API     │ WebSocket    │ Wearable
             │ (Retrofit)   │ (STOMP)      │ (Data/Msg)
             ▼              ▼              ▼
┌─────────────────────────────────────────────────────────┐
│                  Spring Boot 3.5                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │
│  │   Auth   │ │   Walk   │ │   Chat   │ │   Route    │ │
│  │  (JWT)   │ │  (GPS)   │ │ (STOMP)  │ │ (ML+TMap)  │ │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘ │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │
│  │  Place   │ │  Safety  │ │  Badge   │ │   Diary    │ │
│  │(PostGIS) │ │(RiskZone)│ │(Gamify)  │ │  (AI Gen)  │ │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘ │
└───────┬───────────┬───────────┬───────────┬─────────────┘
        │           │           │           │
   ┌────▼────┐ ┌────▼────┐ ┌───▼───┐ ┌────▼─────┐
   │PostgreSQL│ │  Redis  │ │AWS S3 │ │ External │
   │+PostGIS │ │ (Cache) │ │(Files)│ │   APIs   │
   └─────────┘ └─────────┘ └───────┘ └──────────┘
                                       TMap, Weather,
                                       Vision, GMS AI
```

---

## 프로젝트 구조

```
S14P21E108/
├── backend/                          # Spring Boot API 서버
│   ├── src/main/java/com/e108/be/
│   │   ├── domain/                   # 도메인별 기능 모듈
│   │   │   ├── auth/                 # 인증 (회원가입, 로그인, JWT)
│   │   │   ├── walk/                 # 산책 (GPS 추적, 제안, 피드백)
│   │   │   ├── route/                # 경로 추천 (ML, TMap)
│   │   │   ├── chat/                 # 실시간 채팅 (WebSocket)
│   │   │   ├── dog/                  # 반려견 프로필
│   │   │   ├── place/                # 반려견 동반 장소 (PostGIS)
│   │   │   ├── maps/                 # 발자국 & 스탬프
│   │   │   ├── safety/               # 위험 구역 신고
│   │   │   ├── badge/                # 뱃지 (게임화)
│   │   │   ├── diary/                # AI 산책 일기
│   │   │   ├── home/                 # 홈 대시보드
│   │   │   └── record/               # 산책 기록 & 통계
│   │   └── global/                   # 공통 모듈
│   │       ├── common/               # ResTemplate, BaseEntity
│   │       ├── config/               # Security, WebSocket, Redis, S3
│   │       ├── error/                # 예외 처리 (ControllerAdvice)
│   │       ├── jwt/                  # JWT 토큰 관리
│   │       └── logging/              # 요청/응답 로깅
│   ├── models/                       # ML 모델 (dog.onnx)
│   ├── Dockerfile                    # 멀티스테이지 빌드
│   ├── Jenkinsfile                   # CI/CD 파이프라인
│   └── build.gradle                  # Gradle 빌드 설정
│
├── frontend/                         # Android 앱
│   └── app/src/main/java/com/frontend/
│       ├── ui/screen/                # 화면 (Jetpack Compose)
│       │   ├── splash/               # 스플래시 (인증 체크)
│       │   ├── login/                # 로그인
│       │   ├── home/                 # 홈 대시보드
│       │   ├── walk/                 # 산책 (Kakao Maps)
│       │   ├── record/               # 산책 기록
│       │   ├── dog/                  # 반려견 프로필
│       │   ├── badge/                # 뱃지
│       │   ├── chat/                 # 채팅
│       │   └── place/                # 장소 등록
│       ├── data/                     # 데이터 계층
│       │   ├── remote/               # API 인터페이스 (Retrofit)
│       │   ├── local/                # 로컬 저장 (DataStore)
│       │   └── repository/           # 리포지토리 패턴
│       ├── domain/                   # 도메인 계층
│       │   ├── model/                # 데이터 모델 (DTO)
│       │   └── usecase/              # 유스케이스
│       ├── di/                       # Hilt 의존성 주입
│       ├── navigation/               # Compose Navigation
│       ├── notification/             # 알림 관리
│       ├── wearable/                 # 스마트워치 연동
│       └── util/                     # 유틸리티
│
├── data/                             # 장소 데이터 수집 스크립트
│   └── scripts/                      # Python 크롤링 & DB 적재
│
├── infra/                            # 인프라 설정
│   └── docker-compose-local.yaml     # 로컬 개발 환경
│
├── docker-compose-dev.yaml           # 개발 서버 환경
└── docker-compose-prod.yaml          # 운영 서버 환경
```

---

## 시작하기

### 사전 요구사항

- **Java** 17+
- **Android Studio** (Kotlin 2.0+, Compose 지원)
- **Docker** & Docker Compose
- **PostgreSQL** 15+ with PostGIS 확장
- **Redis**

### 백엔드 실행

1. 환경 변수 파일 생성:

```bash
cp backend/.env.example backend/.env
# .env 파일 편집하여 실제 값 입력
```

`.env` 필수 항목:
```env
DB_URL=jdbc:postgresql://localhost:5432/e108_db
DB_PASSWORD=<비밀번호>
JWT_SECRET=<32자 이상 비밀키>
SPRING_PROFILES_ACTIVE=dev
TMAP_APP_KEY=<TMap API 키>
WEATHER_SERVICE_KEY=<기상청 API 키>
AIR_QUALITY_SERVICE_KEY=<대기질 API 키>
AWS_ACCESS_KEY=<AWS 키>
AWS_SECRET_KEY=<AWS 시크릿>
GOOGLE_VISION_API_KEY=<Vision API 키>
GMS_KEY=<GMS AI 키>
```

2. 빌드 및 실행:

```bash
cd backend
./gradlew bootRun
```

3. Health check: `GET http://localhost:8080/api/v1/actuator/health`

### 프론트엔드 실행

1. `frontend/.env.example`을 참고하여 `local.properties`에 API 키 추가:

```properties
KAKAO_MAP_API_KEY=<카카오 맵 API 키>
```

2. Android Studio에서 빌드:

```bash
cd frontend
./gradlew assembleDevDebug
```

> Kakao Maps SDK는 ARM 기기/에뮬레이터에서만 동작합니다 (x86 미지원).

### Docker 환경 실행

```bash
# 로컬 개발 (DB + Redis만)
docker compose -f infra/docker-compose-local.yaml --env-file infra/.env up -d

# 개발 서버 (DB + Redis + App: 포트 8081)
docker compose -f docker-compose-dev.yaml --env-file .env up -d

# 운영 서버 (DB + Redis + App: 포트 8080)
docker compose -f docker-compose-prod.yaml --env-file .env up -d
```

---

## API 명세

**Base Path:** `/api/v1`

### 인증 (Auth)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/auth/register` | 회원가입 |
| POST | `/auth/login` | 로그인 |
| POST | `/auth/refresh-token` | 토큰 갱신 |
| GET | `/auth/validate-token` | 토큰 검증 |
| POST | `/auth/logout` | 로그아웃 |

### 산책 (Walk)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/walks` | 산책 시작 |
| POST | `/walks/{walkId}/locations` | GPS 좌표 배치 저장 |
| POST | `/walks/{walkId}/end` | 산책 종료 |
| GET | `/walks/{walkId}` | 산책 상세 조회 |
| GET | `/walks/{walkId}/distance` | 이동 거리 |
| GET | `/walks/{walkId}/duration` | 산책 시간 |
| GET | `/walks/{walkId}/calories` | 소모 칼로리 |
| POST | `/walks/{walkId}/photos` | 사진 업로드 |
| DELETE | `/walks/{walkId}/photos` | 사진 삭제 |
| GET | `/walks/nearby-dogs` | 주변 산책 중인 반려견 |
| POST | `/walks/proposals` | 산책 제안 보내기 |
| PATCH | `/walks/proposals/{id}` | 산책 제안 응답 |
| POST | `/walks/{walkRecordId}/encounters` | 만난 반려견 기록 |
| GET | `/walks/met-dogs` | 만난 반려견 목록 |
| PATCH | `/walks/met-dogs/feedback` | 피드백 수정 |

### 경로 추천 (Route)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/routes/recommend` | 맞춤 경로 3개 추천 (위치 + 날씨 기반) |
| POST | `/routes/selection` | 경로 선택 기록 |

### 채팅 (Chat)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/chat/rooms/{chatRoomId}` | 채팅방 조회 + 메시지 이력 |
| WS | `/app/chat/{chatRoomId}` | 메시지 전송 (STOMP) |
| WS | `/topic/chat/{chatRoomId}` | 메시지 구독 (STOMP) |

### 반려견 (Dog)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/dogs` | 반려견 등록 |
| GET | `/dogs/{dogId}` | 반려견 상세 (비공개) |
| GET | `/dogs/{dogId}/public` | 반려견 프로필 (공개) |
| PATCH | `/dogs/{dogId}` | 정보 수정 |
| PATCH | `/dogs/{dogId}/weight` | 체중 수정 |
| PATCH | `/dogs/{dogId}/traits` | 성격 태그 수정 |

### 장소 (Place)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/places` | 장소 등록 (이미지 포함) |
| GET | `/places/nearby` | 주변 장소 검색 (PostGIS) |
| GET | `/places/{placeId}` | 장소 상세 |
| GET | `/places/categories` | 카테고리 목록 |

### 지도 & 발자국 (Maps)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/maps/footprints` | 발자국 히트맵 |
| GET | `/maps/footprints/history` | 과거 산책 목록 |
| GET | `/maps/stamps` | 스탬프 목록 |
| POST | `/maps/stamps` | 스탬프 찍기 |
| DELETE | `/maps/stamps` | 스탬프 취소 |

### 안전 (Safety)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/safety/risk-zones` | 위험 구역 신고 |
| GET | `/safety/risk-zones` | 주변 위험 구역 조회 |
| GET | `/safety/risk-zones/mine` | 내 신고 목록 |
| DELETE | `/safety/risk-zones/{id}` | 신고 삭제 |

### 기록 (Record)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/records/calendar` | 캘린더 뷰 (월별) |
| GET | `/records/daily` | 일별 산책 목록 |
| GET | `/records/{recordId}` | 산책 상세 기록 |

### 뱃지 (Badge)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/badges` | 전체 뱃지 목록 |
| GET | `/badges/my` | 획득한 뱃지 |
| GET | `/badges/progress` | 뱃지 진행도 |

### 일기 (Diary)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/diaries` | 산책 일기 조회 |
| POST | `/diaries/generate` | 일기 생성 (AI) |

### 홈 (Home)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/home` | 대시보드 (날씨 + 통계 + 추천) |

---

## 데이터 모델

### 핵심 엔티티

```
User ──1:N──▶ Dog ──1:N──▶ WalkRecord ──1:N──▶ WalkLocation
                              │
                              ├──1:N──▶ Footprint
                              ├──1:N──▶ MetDog ──1:1──▶ Feedback
                              └──1:1──▶ Diary

Dog ──N:M──▶ PersonalityTag
User ──1:N──▶ UserBadge ──N:1──▶ Badge
User ──1:N──▶ ChatRoom ──1:N──▶ ChatMessageEntry
User ──1:N──▶ RiskReport
User ──1:N──▶ RouteSelectionLog ──1:N──▶ RouteSelectionPlace
User ──1:N──▶ UserCategoryPreference

Place ──N:1──▶ PlaceCategory
```

### 공간 데이터 타입

- **Place** — `GEOGRAPHY(Point, 4326)` (WGS84 좌표)
- **RiskReport** — 위도/경도 (DOUBLE)
- **WalkRecord** — `GEOGRAPHY(LINESTRING, 4326)` (산책 경로)

### Soft Delete

모든 엔티티는 `BaseEntity`를 상속하며, `isDeleted` 플래그를 통한 논리 삭제를 사용합니다. `createdAt`, `updatedAt`은 JPA Auditing으로 자동 관리됩니다.

---

## 외부 서비스 연동

| 서비스 | 용도 | 비고 |
|--------|------|------|
| **TMap API** | 보행자 경로 탐색 | 경로 추천 시 실제 도보 경로 계산 |
| **기상청 API** | 날씨 정보 | 홈 화면 + 경로 추천 가중치 |
| **에어코리아 API** | 대기질 정보 | 산책 적합도 판단 |
| **Google Cloud Vision** | 이미지 분석 | 산책 사진에서 반려견 인식 |
| **GMS AI** | 텍스트 생성 | 산책 일기 자동 작성 |
| **AWS S3** | 파일 저장소 | 반려견 사진, 산책 사진 업로드 |
| **Kakao Maps SDK** | 지도 렌더링 | 산책 경로, 장소, 위험구역 표시 |
| **Kakao Local API** | 장소 검색 | 주소/키워드 기반 장소 탐색 |
| **Firebase** | 앱 배포 | App Distribution (테스터 배포) |

---

## CI/CD 파이프라인

### Jenkins 기반 자동 배포

```
GitLab Push
    │
    ├── develop 브랜치 ──▶ Dev 환경 배포
    │   ├── Backend: Docker 이미지 빌드 → docker-compose-dev.yaml
    │   │   └── Health check: /api/v1/actuator/health (포트 8081)
    │   └── Frontend: assembleDevDebug → Firebase App Distribution
    │
    └── main 브랜치 ──▶ Prod 환경 배포
        └── Backend: Docker 이미지 빌드 → docker-compose-prod.yaml
            └── Health check: /api/v1/actuator/health (포트 8080)
```

### 빌드 환경별 URL

| 환경 | Backend URL | 비고 |
|------|-------------|------|
| **Local** | `http://10.0.2.2:8080/api/v1/` | Android 에뮬레이터 |
| **Dev** | `https://j14e108.p.ssafy.io/dev/api/v1/` | 개발 서버 |
| **Prod** | `https://j14e108.p.ssafy.io/api/v1/` | 운영 서버 |

---

## 개발 컨벤션

### 브랜치 전략

| 브랜치 | 용도 |
|--------|------|
| `main` | 운영 배포 |
| `develop` | 통합 (기본 브랜치) |
| `be/feature/{issue}-{desc}` | 백엔드 기능 개발 |
| `be/fix/{issue}-{desc}` | 백엔드 버그 수정 |
| `feat/{issue}-{desc}` | 프론트엔드 기능 개발 |

### 커밋 메시지

```
{TYPE}[#{ISSUE}] {설명}
```

| Type | 용도 |
|------|------|
| `FEAT` | 새 기능 |
| `FIX` | 버그 수정 |
| `REFACTOR` | 리팩토링 |
| `DOCS` | 문서 |
| `STYLE` | 코드 스타일 |
| `TEST` | 테스트 |
| `CHORE` | 빌드/설정 |
| `!HOTFIX` | 긴급 수정 |

### 네이밍 규칙

| 대상 | 패턴 | 예시 |
|------|------|------|
| Controller | `{Domain}Controller` | `WalkController` |
| Service | `{Domain}Service` | `WalkService` |
| Repository | `{Entity}Repository` | `WalkRecordRepository` |
| Request DTO | `{Action}{Domain}Request` | `StartWalkRequest` |
| Response DTO | `{Domain}Response` | `WalkDetailResponse` |
| Exception | `{Situation}Exception` | `WalkNotFoundException` |

### API 응답 형식

모든 API는 `ResTemplate<T>`로 응답합니다:

```json
{
  "code": 200,
  "message": "조회 성공",
  "data": { ... }
}
```

### 코드 규칙

- 엔티티에 `@Setter` 사용 금지 — 생성자 또는 빌더 패턴 사용
- `System.out.println` 금지
- Response DTO에 `static from(Entity)` 팩토리 메서드 필수
- 물리 삭제 금지 — `isDeleted = true`로 논리 삭제
- 서비스 클래스: 기본 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional`

---

## 라이선스

이 프로젝트는 Samsung Software Academy for Youth(SSAFY) 자율 프로젝트의 일환으로 개발되었습니다.
