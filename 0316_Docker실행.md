# 0316 작업 정리 — GPS 산책 API 구현 및 Docker 설정

---

## 목표
반려견 산책 앱에 GPS 관련 API 3개 구현 및 테스트
- `POST /api/v1/walks/{walkId}/locations` — GPS 좌표 저장
- `GET /api/v1/walks/{walkId}/distance` — 산책 거리 계산
- `GET /api/v1/walks/{walkId}/calories` — 칼로리 계산

---

## 한 작업 목록

### 1. 의존성 추가 (`build.gradle`)
- Redis 의존성 추가
- hibernate-spatial 추가 (PostGIS LINESTRING 타입 지원)

### 2. 파일 구현
- `WalkRecord.java` — `GEOGRAPHY(LINESTRING, 4326)` 타입 route_line 컬럼
- `WalkService.java` — GPS 저장(Redis), 거리(Haversine), 칼로리 계산
- `WalkController.java` — 3개 API 엔드포인트
- `WalkLocationRequest.java`, `WalkStartRequest.java` 등 DTO 파일들
- `Dog.java`, `DogRepository.java` — 칼로리 계산용 체중 조회

### 3. Docker 설정 변경
- `docker-compose-local.yaml`: `postgres:15` → `postgis/postgis:15-3.5`

### 4. DB 설정
- PostGIS 확장 활성화
- 테스트용 더미 데이터 삽입

### 5. Postman 테스트 완료 ✅

---

## 직면한 문제 & 해결 방법

### 문제 1. `geography type does not exist`
**원인**: `postgres:15` 이미지에 PostGIS가 없음
**해결**:
```yaml
# docker-compose-local.yaml
image: postgis/postgis:15-3.5  # postgres:15 → 변경
```
```bash
docker-compose down -v && docker-compose up -d
```

---

### 문제 2. 계속되는 403 Forbidden
**원인**: 두 가지가 복합적으로 작용
1. `walk_records` 테이블이 없어서 500 에러 발생
2. Spring이 500 에러를 `/error`로 포워딩하는데, `/error`가 SecurityConfig `permitAll()`에 없어서 403 반환

**해결 과정**:
- 임시 DEBUG 로그 추가해서 원인 파악
```yaml
logging:
  level:
    org.springframework.security: DEBUG
```
- 로그에서 발견:
```
Securing POST /walks/1/locations  ← 통과 ✅
→ DB 에러 발생 (walk_records 없음)
→ Spring이 /error로 포워딩
Securing POST /error  ← 여기서 막힘!
Http403ForbiddenEntryPoint → 403 반환
```
- PostGIS 활성화로 `walk_records` 테이블 생성
```bash
docker exec -it e108-postgres-local psql -U postgres -d e108_db -c "CREATE EXTENSION IF NOT EXISTS postgis;"
```
- 디버그 로그는 테스트 후 원복

---

### 문제 3. `walk_records` 테이블이 없음
**원인**: PostGIS 미활성화 상태에서 `geography` 타입 컬럼 생성 실패
**해결**: PostGIS 활성화 후 서버 재시작 → Hibernate가 테이블 자동 생성

---

### 문제 4. 테스트 데이터 없어서 404
**원인**: `walk_records`에 데이터 없음
**해결**:
```bash
# is_deleted NOT NULL 오류 → 컬럼 추가해서 재시도
docker exec -it e108-postgres-local psql -U postgres -d e108_db -c \
"INSERT INTO walk_records (dog_id2, walk_status, start_time, is_deleted, created_at, updated_at)
VALUES (1, 'IN_PROGRESS', NOW(), false, NOW(), NOW());"
```
> 첫 INSERT 실패로 sequence가 올라가 실제 id=2로 생성됨 → Postman URL을 `/walks/2/`로 수정

---

### 문제 5. calories 404
**원인**: dogs 테이블에 데이터 없음 (칼로리 계산에 체중 필요)
**해결**:
```bash
docker exec -it e108-postgres-local psql -U postgres -d e108_db -c \
"INSERT INTO dogs (id, weight) VALUES (1, 5.00);"
```

---

### 문제 6. 포트 8080 충돌
**원인**: Spring Boot가 완전히 종료되지 않은 채 프로세스가 남아있음
**해결**:
```bash
netstat -ano | findstr :8080   # PID 확인
taskkill /PID [번호] /F         # 관리자 권한 터미널에서 실행
```
> **포인트**: 일반 터미널에서는 권한 부족으로 실패 → VS Code 터미널(관리자 권한)에서만 성공

**예방법**: 서버 종료 시 IntelliJ 빨간 Stop 버튼 클릭 후 닫기

---

## 질문 & 답변 정리

| 질문 | 답변 |
|------|------|
| LINESTRING은 왜 좌표가 3개 필요해? | 선(Line)을 그리려면 최소 2개 필요하지만 PostGIS 기본값이 3개. 실제론 2개도 가능 |
| `/walks/**` permitAll이면 하위 API 다 허용 아냐? | 맞음. `/walks/2/locations`, `/walks/2/distance` 등 모두 인증 없이 허용 |
| `/error`는 왜 추가해야 해? | Spring이 내부 에러를 `/error`로 포워딩하는데 거기서 Security가 다시 막아서 403이 됨 |
| 테스트 더미 데이터가 팀원 코드와 충돌하나? | 충돌 없음. 로컬 DB에만 존재, git에 올라가지 않음 |
| Redis 관련 파일이 따로 있어? | 별도 파일 없이 WalkService에서 RedisTemplate으로 직접 사용 |
| Redis는 왜 쓰는 거야? | 산책 중 빠른 읽기/쓰기 위해 DB 대신 Redis에 임시 저장. 산책 종료 시 DB에 최종 저장 |
| 포트 충돌이 왜 자꾸 일어나? | Spring Boot가 완전히 종료되지 않고 프로세스가 남아있기 때문 |
| postgres:15 → postgis:15-3.5로 바꾸는 이유? | `geography(POINT/LINESTRING, 4326)` 타입은 PostGIS 없이는 사용 불가 |
| places 테이블 location도 PostGIS 필요해? | 맞음. `GEOGRAPHY(POINT, 4326)`이라서 동일하게 PostGIS 필요 |

---

## 팀원에게 공유해야 할 것

### 1. docker-compose-local.yaml 이미지 변경
```yaml
# 수정 전
image: postgres:15

# 수정 후
image: postgis/postgis:15-3.5
```

git pull 후 아래 명령어 실행 필요:
```bash
cd S14P21E108/infra
docker-compose -f docker-compose-local.yaml down -v
docker-compose -f docker-compose-local.yaml up -d
```

### 2. PostGIS 확장 활성화 (최초 1회)
```bash
docker exec -it e108-postgres-local psql -U postgres -d e108_db -c "CREATE EXTENSION IF NOT EXISTS postgis;"
```

### 3. 이유
`walk_records.route_line`과 `places.location` 모두 geography 타입 사용:
```sql
walk_records.route_line  → GEOGRAPHY(LINESTRING, 4326)
places.location          → GEOGRAPHY(POINT, 4326)
```
PostGIS 없이는 서버 실행 시 테이블 생성 실패함

---

## 남은 작업
- [ ] `WalkController.java` import 수정: `LocationBatchRequest` → `WalkLocationRequest`
- [ ] 불필요 파일 삭제: `LocationBatchRequest.java`, `CaloriesResponse.java`, `DistanceResponse.java`
- [ ] `SecurityConfig`에 `/error` 추가 (권장)
- [ ] Git 커밋 & MR 생성
