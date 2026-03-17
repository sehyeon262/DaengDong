---

## 프로젝트 구조

```
com.e108.be/
├── BeApplication.java              ← 앱 실행 진입점
│
├── domain/                         ← 비즈니스 기능별로 나눠놓은 곳
│   └── auth/                       ← 인증 관련 기능 (로그인, 회원가입 등)
│       ├── controller/             ← API 요청을 받는 곳
│       ├── service/                ← 핵심 비즈니스 로직
│       ├── repository/             ← DB 접근 (조회, 저장, 삭제)
│       ├── entity/                 ← DB 테이블과 매핑되는 클래스
│       ├── dto/
│       │   ├── request/            ← 프론트 → 서버 데이터 형식
│       │   └── response/           ← 서버 → 프론트 데이터 형식
│       └── exception/              ← 이 도메인에서 쓰는 커스텀 예외
│
└── global/                         ← 프로젝트 전체에서 공통으로 쓰는 것들
    ├── common/
    │   ├── template/               ← ResTemplate (API 응답 통일 형식)
    │   ├── entity/                 ← BaseEntity (생성일, 수정일, 삭제여부)
    │   ├── enums/                  ← 여러 도메인에서 공유하는 Enum
    │   └── util/                   ← 유틸리티 클래스
    ├── config/                     ← 설정 (Security, CORS 등)
    ├── error/
    │   ├── ControllerAdvice.java   ← 전역 예외 처리
    │   └── exception/              ← HTTP 상태코드별 그룹 예외
    ├── jwt/                        ← JWT 토큰 생성/검증
    └── logging/                    ← API 요청/응답 로그
```

---

## 핵심 개념 설명

### API 요청 흐름 (로그인 예시)

```
프론트에서 POST /auth/login 요청
       ↓
  Controller    요청을 받아서 Service에 넘긴다
       ↓
  Service       비밀번호 확인하고, JWT 토큰 만든다
       ↓
  Repository    DB에서 회원 정보를 조회한다
       ↓
  Controller    결과를 ResTemplate으로 감싸서 응답한다
       ↓
프론트에서 JSON 응답 받음
```

### 각 폴더가 하는 일

| 폴더 | 한 줄 요약 | 비유 |
|---|---|---|
| `controller/` | 프론트의 요청을 받는 창구 | 식당 카운터 (주문 받는 곳) |
| `service/` | 실제 로직을 처리 | 주방 (요리하는 곳) |
| `repository/` | DB에서 데이터 가져오기 | 냉장고 (재료 꺼내는 곳) |
| `entity/` | DB 테이블 = Java 클래스 | 재료 그 자체 |
| `dto/request/` | 프론트가 보내는 데이터 형식 | 주문서 |
| `dto/response/` | 서버가 돌려주는 데이터 형식 | 완성된 요리 |
| `exception/` | "이런 에러야" 정의 | "재료 없음", "조리 실패" 같은 문제 상황 |

### 응답 형식 (ResTemplate)

모든 API는 이 형태로 응답합니다:
```json
// 성공
{ "code": 200, "message": "로그인 성공", "data": { "accessToken": "...", "nickname": "홍길동" } }

// 에러 (data 필드 자체가 없음)
{ "code": 404, "message": "회원을 찾을 수 없습니다." }
```

### 에러 처리 구조

```
도메인 예외 → 그룹 예외 상속 → ControllerAdvice가 자동 처리

예시: throw new AuthException("비밀번호가 틀립니다.")
             ↓ (AuthGroupException 상속)
      ControllerAdvice가 잡아서 401 응답 자동 반환
```

| 그룹 예외 | 상태코드 | 언제 쓰나 |
|---|---|---|
| `InvalidGroupException` | 400 | 잘못된 요청 (파라미터 오류) |
| `AuthGroupException` | 401 | 인증 실패 (로그인 실패, 토큰 만료) |
| `AccessDeniedGroupException` | 403 | 권한 없음 (로그인은 했지만 접근 불가) |
| `NotFoundGroupException` | 404 | 리소스 없음 (회원 없음, 게시글 없음) |
| `ConflictGroupException` | 409 | 충돌 (이메일 중복, 닉네임 중복) |
| `InternalServerErrorGroupException` | 500 | 서버 에러 |

**새 예외 추가 방법:**
```java
// 1. 어떤 HTTP 상태인지 결정 → 해당 그룹 예외 상속
public class MemberNotFoundException extends NotFoundGroupException {
    public MemberNotFoundException() {
        super("회원을 찾을 수 없습니다.");
    }
}

// 2. Service에서 throw만 하면 끝 (ControllerAdvice가 알아서 처리)
throw new MemberNotFoundException();  // → 404 자동 반환
```

---

## 새 도메인 추가 방법 (예: board 게시판)

```
1. domain/board/ 폴더 생성
2. 아래 구조대로 파일 생성:

domain/board/
├── controller/BoardController.java
├── service/BoardService.java
├── repository/BoardRepository.java
├── entity/Board.java              ← extends BaseEntity
├── dto/
│   ├── request/CreateBoardRequest.java
│   └── response/BoardResponse.java
└── exception/BoardNotFoundException.java  ← extends NotFoundGroupException
```
> `global/` 쪽은 건드릴 필요 없이, `domain/` 안에서만 작업하면 됩니다.

---

## ⚠️ 팀원 필독 — 환경 변경 사항 (2026-03-16)

> GPS 산책 기능(#107, #109, #110) 구현으로 인해 아래 변경 사항이 생겼습니다.
> **Pull 후 반드시 아래 순서대로 환경을 다시 세팅해주세요.**

### 1. Docker PostgreSQL 이미지 변경

**변경 파일:** `infra/docker-compose-local.yaml`

```yaml
# 변경 전
image: postgres:15

# 변경 후
image: postgis/postgis:15-3.5
```

**이유:** GPS 경로(`route_line`)를 `GEOGRAPHY(LINESTRING, 4326)` 타입으로 저장하기 위해 PostGIS 확장이 필요합니다.
기존 `postgres:15` 이미지는 `geography` 타입을 지원하지 않아 서버 시작 시 오류가 발생합니다.

**적용 방법 (볼륨까지 초기화 필요):**
```bash
cd infra
docker-compose -f docker-compose-local.yaml down -v
docker-compose -f docker-compose-local.yaml up -d
```

> ⚠️ `-v` 옵션은 기존 DB 데이터를 모두 삭제합니다. 로컬 테스트 데이터가 있다면 미리 백업하세요.

---

### 2. PostGIS 확장 활성화

컨테이너 재시작 후, **한 번만** 아래 명령어를 실행해주세요:

```bash
docker exec -it e108-postgres-local psql -U postgres -d e108_db -c "CREATE EXTENSION IF NOT EXISTS postgis;"
```

이후 Spring Boot 서버를 시작하면 Hibernate가 `walk_records` 테이블을 자동 생성합니다.

---

### 3. Redis 추가

Redis가 `docker-compose-local.yaml`에 포함되어 있습니다. (포트: 6379)
`build.gradle`에 Redis 의존성이 추가되어 있으므로, Pull 후 빌드만 하면 됩니다.

---

## GPS 산책 기능 — 구현 내용 (#107, #109, #110)

### 추가된 API

| 메서드 | URL | 설명 |
|--------|-----|------|
| `POST` | `/api/v1/walks/{walkId}/locations` | GPS 좌표 배치 저장 |
| `GET` | `/api/v1/walks/{walkId}/distance` | 누적 산책 거리 조회 |
| `GET` | `/api/v1/walks/{walkId}/calories` | 소모 칼로리 조회 |

### 동작 방식

```
산책 중 GPS 좌표 수신
       ↓
POST /walks/{walkId}/locations
       ↓
Redis List에 임시 저장 (키: "walk:gps:{walkId}")
저장 형식: "위도,경도,타임스탬프"
       ↓
GET /walks/{walkId}/distance
       ↓
Redis에서 좌표 꺼내 Haversine 공식으로 거리 계산
       ↓
GET /walks/{walkId}/calories
       ↓
walk_records → dog → 체중 조회 → 체중 × 거리km × 0.8
```

### 칼로리 계산 공식

```
calories (kcal) = 체중(kg) × 거리(km) × 0.8
```

체중이 입력되지 않은 경우 `requiresWeight: true` 를 반환하여 프론트에서 입력 유도 UI를 표시합니다.

### 요청/응답 예시

**GPS 좌표 저장**
```json
// POST /api/v1/walks/1/locations
{
  "locations": [
    { "latitude": 37.5665, "longitude": 126.9780, "timestamp": 1234567890000 },
    { "latitude": 37.5670, "longitude": 126.9785, "timestamp": 1234567895000 }
  ]
}
// 응답
{ "code": 200, "message": "위치 저장 성공", "data": { "savedCount": 2 } }
```

**거리 조회**
```json
// GET /api/v1/walks/1/distance
{ "code": 200, "message": "거리 조회 성공", "data": { "distanceM": 67.3, "distanceKm": 0.07 } }
```

**칼로리 조회**
```json
// GET /api/v1/walks/1/calories (체중 있을 때)
{ "code": 200, "message": "칼로리 조회 성공", "data": { "calories": 0.3, "requiresWeight": false } }

// GET /api/v1/walks/1/calories (체중 미입력 시)
{ "code": 200, "message": "체중을 입력해 주세요", "data": { "requiresWeight": true } }
```

---

## SecurityConfig 변경 사항

`global/config/SecurityConfig.java`에 `/error` 경로 허용이 추가되었습니다.

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/auth/**").permitAll()
    .requestMatchers("/walks/**").permitAll()
    .requestMatchers("/error").permitAll()   // ← 추가됨
    .anyRequest().authenticated()
)
```

**추가 이유:**
Spring Boot는 서버 내부에서 오류(500 등)가 발생하면 자동으로 `/error` 경로로 포워딩합니다.
`/error`가 허용되지 않으면 Spring Security가 이 요청을 차단하여 실제 오류 메시지 대신 **403 Forbidden**이 반환됩니다.
추가 후에는 오류 발생 시 정확한 .,에러 메시지(500, 404 등)가 클라이언트에 전달됩니다.
