# E108 Backend

## 기술 스택
- Java 17 / Spring Boot 3.5
- Spring Security + JWT
- Spring Data JPA + PostgreSQL
- Gradle

---

## 실행 전 준비

### 1. `.env` 파일 생성
프로젝트 루트(`build.gradle`과 같은 위치)에 `.env` 파일을 만들고 본인 환경에 맞게 값을 채워주세요.
```
DB_PASSWORD=본인_PostgreSQL_비밀번호
JWT_SECRET=32자_이상의_시크릿키_아무거나
```
> `.env`는 `.gitignore`에 포함되어 있어 Git에 올라가지 않습니다.

### 2. PostgreSQL 데이터베이스 생성
```sql
CREATE DATABASE e108_db;
```

### 3. 실행
```bash
./gradlew bootRun
```

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
