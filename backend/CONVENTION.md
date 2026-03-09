# E108 Backend Convention

## 목차
1. [패키지 구조](#패키지-구조)
2. [네이밍 규칙](#네이밍-규칙)
3. [코딩 컨벤션](#코딩-컨벤션)
4. [API 설계](#api-설계)
5. [예외 처리](#예외-처리)
6. [Git 컨벤션](#git-컨벤션)

---

## 패키지 구조

```
com.e108.be/
├── BeApplication.java
│
├── domain/                          ← 도메인별 비즈니스 로직
│   └── {도메인명}/
│       ├── controller/              ← API 진입점
│       ├── service/                 ← 비즈니스 로직
│       ├── repository/              ← DB 접근
│       ├── entity/                  ← JPA 엔티티
│       ├── dto/
│       │   ├── request/             ← 요청 DTO
│       │   └── response/            ← 응답 DTO
│       └── exception/               ← 도메인 예외
│
└── global/                          ← 공통 모듈
    ├── common/
    │   ├── template/                ← ResTemplate
    │   ├── entity/                  ← BaseEntity
    │   ├── enums/                   ← 공통 Enum
    │   └── util/                    ← 유틸리티
    ├── config/                      ← 설정 클래스
    ├── error/                       ← 전역 예외 처리
    ├── jwt/                         ← JWT 관련
    └── logging/                     ← 로깅 필터
```

### 새 도메인 추가 시
```
domain/{도메인명}/
├── controller/{도메인명}Controller.java
├── service/{도메인명}Service.java
├── repository/{도메인명}Repository.java
├── entity/{엔티티명}.java
├── dto/
│   ├── request/{동작}{도메인명}Request.java
│   └── response/{도메인명}Response.java
└── exception/{예외명}Exception.java
```

---

## 네이밍 규칙

### 클래스명

| 구분 | 패턴 | 예시 |
|---|---|---|
| Controller | `{도메인}Controller` | `AuthController`, `BoardController` |
| Service | `{도메인}Service` | `AuthService`, `BoardService` |
| Repository | `{엔티티}Repository` | `MemberRepository`, `BoardRepository` |
| Entity | 명사 단수형 | `Member`, `Board`, `Comment` |
| Request DTO | `{동작}{도메인}Request` | `LoginRequest`, `CreateBoardRequest` |
| Response DTO | `{도메인}Response` | `LoginResponse`, `BoardResponse` |
| Exception | `{상황}Exception` | `AuthException`, `MemberNotFoundException` |

### 메서드명

| 구분 | 패턴 | 예시 |
|---|---|---|
| 조회 (단건) | `get{Entity}`, `find{Entity}` | `getMember()`, `findById()` |
| 조회 (목록) | `get{Entity}List`, `findAll{Entity}` | `getBoardList()`, `findAllByUserId()` |
| 생성 | `create{Entity}`, `save{Entity}` | `createBoard()`, `saveMember()` |
| 수정 | `update{Entity}`, `modify{Entity}` | `updateBoard()`, `modifyPassword()` |
| 삭제 | `delete{Entity}`, `remove{Entity}` | `deleteBoard()`, `removeMember()` |
| 검증 | `validate{대상}`, `check{대상}` | `validateToken()`, `checkDuplicate()` |

### 변수명

| 구분 | 규칙 | 예시 |
|---|---|---|
| 일반 변수 | camelCase | `memberName`, `accessToken` |
| 상수 | UPPER_SNAKE_CASE | `MAX_SIZE`, `DEFAULT_PAGE_SIZE` |
| 컬렉션 | 복수형 | `members`, `boardList` |
| Boolean | `is-`, `has-`, `can-` | `isDeleted`, `hasPermission` |

### URL 패턴

```
/{도메인}/{리소스}

# CRUD 기본
GET    /boards           → 목록 조회
GET    /boards/{id}      → 단건 조회
POST   /boards           → 생성
PUT    /boards/{id}      → 전체 수정
PATCH  /boards/{id}      → 부분 수정
DELETE /boards/{id}      → 삭제

# 관계 리소스
GET    /boards/{id}/comments      → 게시글의 댓글 목록
POST   /boards/{id}/comments      → 게시글에 댓글 추가

# 특수 동작
POST   /auth/login
POST   /auth/logout
```

---

## 코딩 컨벤션

### Entity

```java
@Entity
@Table(name = "members")        // 테이블명은 복수형 snake_case
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    // 생성은 Builder 패턴 사용
    @Builder
    public Member(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // 수정은 명시적 메서드 사용 (setter 금지)
    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }
}
```

**규칙:**
- `@Setter` 사용 금지 → 명시적 메서드로 변경
- `BaseEntity` 상속 필수 (createdAt, updatedAt, isDeleted)
- `@NoArgsConstructor(access = PROTECTED)` 필수 (JPA용)
- 생성은 `@Builder` 사용

### Controller

```java
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping
    public ResTemplate<List<BoardResponse>> getBoards() {
        List<BoardResponse> boards = boardService.getBoards();
        return ResTemplate.success(HttpStatus.OK, "게시글 목록 조회 성공", boards);
    }

    @PostMapping
    public ResTemplate<BoardResponse> createBoard(@RequestBody CreateBoardRequest request) {
        BoardResponse response = boardService.createBoard(request);
        return ResTemplate.success(HttpStatus.CREATED, "게시글 생성 성공", response);
    }
}
```

**규칙:**
- 반환은 `ResTemplate<T>` 사용
- `@RequiredArgsConstructor`로 의존성 주입
- Controller에 비즈니스 로직 금지 → Service에 위임

### Service

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)      // 기본은 읽기 전용
public class BoardService {

    private final BoardRepository boardRepository;

    public List<BoardResponse> getBoards() {
        // 조회 로직
    }

    @Transactional                    // 쓰기 작업만 별도 선언
    public BoardResponse createBoard(CreateBoardRequest request) {
        // 생성 로직
    }
}
```

**규칙:**
- 클래스에 `@Transactional(readOnly = true)` 기본 적용
- 쓰기 작업(Create/Update/Delete)에만 `@Transactional` 별도 선언

### DTO

```java
// Request: @Getter, @NoArgsConstructor
@Getter
@NoArgsConstructor
public class CreateBoardRequest {
    private String title;
    private String content;
}

// Response: @Getter, @Builder
@Getter
@Builder
public class BoardResponse {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;

    // Entity → Response 변환 정적 메서드
    public static BoardResponse from(Board board) {
        return BoardResponse.builder()
                .id(board.getId())
                .title(board.getTitle())
                .content(board.getContent())
                .createdAt(board.getCreatedAt())
                .build();
    }
}
```

**규칙:**
- Request DTO: `@Getter`, `@NoArgsConstructor`
- Response DTO: `@Getter`, `@Builder`
- Entity → DTO 변환은 DTO에 `from()` 정적 메서드로

### Repository

```java
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 메서드 이름으로 쿼리 자동 생성
    Optional<Board> findByTitle(String title);
    List<Board> findAllByMemberId(Long memberId);
    boolean existsByTitle(String title);

    // 복잡한 쿼리는 @Query 사용
    @Query("SELECT b FROM Board b WHERE b.member.id = :memberId AND b.isDeleted = false")
    List<Board> findActiveByMemberId(@Param("memberId") Long memberId);
}
```

---

## API 설계

### 응답 형식 (ResTemplate)

모든 API는 동일한 형식으로 응답:

```json
// 성공 (data 있음)
{
  "code": 200,
  "message": "조회 성공",
  "data": { ... }
}

// 성공 (data 없음)
{
  "code": 204,
  "message": "삭제 성공"
}

// 에러
{
  "code": 404,
  "message": "회원을 찾을 수 없습니다."
}
```

### HTTP 상태 코드

| 코드 | 의미 | 사용 시점 |
|---|---|---|
| 200 | OK | 조회, 수정 성공 |
| 201 | Created | 생성 성공 |
| 204 | No Content | 삭제 성공 |
| 400 | Bad Request | 잘못된 요청 (파라미터 오류) |
| 401 | Unauthorized | 인증 실패 (로그인 필요) |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 충돌 (중복 데이터) |
| 500 | Internal Server Error | 서버 에러 |

---

## 예외 처리

### 예외 계층 구조

```
RuntimeException
└── {상태코드}GroupException (추상)
    └── 도메인별 구체 예외

예시:
AuthGroupException (401)
└── AuthException
└── TokenExpiredException

NotFoundGroupException (404)
└── MemberNotFoundException
└── BoardNotFoundException
```

### 새 예외 추가 방법

```java
// 1. 어떤 HTTP 상태코드인지 결정 → 해당 그룹 예외 상속
public class MemberNotFoundException extends NotFoundGroupException {
    public MemberNotFoundException() {
        super("회원을 찾을 수 없습니다.");
    }
}

// 2. Service에서 throw
throw new MemberNotFoundException();  // → 404 자동 반환
```

### 그룹 예외 목록

| 예외 클래스 | HTTP 코드 | 사용 시점 |
|---|---|---|
| `InvalidGroupException` | 400 | 잘못된 요청, 유효성 실패 |
| `AuthGroupException` | 401 | 인증 실패, 토큰 만료 |
| `AccessDeniedGroupException` | 403 | 권한 없음 |
| `NotFoundGroupException` | 404 | 리소스 없음 |
| `ConflictGroupException` | 409 | 중복, 충돌 |
| `InternalServerErrorGroupException` | 500 | 서버 에러 |

---

## Git 컨벤션

### 브랜치 전략

| 브랜치 | 용도 |
|---|---|
| `master` | 배포 브랜치 |
| `develop` | 기본(default) 브랜치, 개발 통합 |
| `be/feature` | 백엔드 기능 개발 |
| `be/refactor` | 백엔드 리팩토링 |
| `be/fix` | 백엔드 버그 수정 |
| `be/init` | 백엔드 초기 세팅 |

### 브랜치 네이밍

```
카테고리/브랜치유형/이슈번호-기능명
```

**예시:**
```
be/feature/12-login          # 백엔드 로그인 기능
be/feature/15-board-crud     # 백엔드 게시판 CRUD
be/refactor/20-auth-service  # 백엔드 인증 서비스 리팩토링
be/fix/25-token-expire       # 백엔드 토큰 만료 버그 수정
be/init/1-project-setup      # 백엔드 프로젝트 초기 세팅
```

### 커밋 메시지 형식

```
커밋유형[#이슈번호] 커밋 내용
```

> **규칙**
> - 커밋 유형은 **영어 대문자**로 작성
> - 한 커밋에는 **한 가지 문제만** 담기 (추적 용이)
> - 이슈 번호로 GitHub/Jira Issue 연동

### 커밋 타입

| 타입 | 설명 |
|---|---|
| `FEAT` | 새로운 기능 추가 |
| `FIX` | 버그 수정 |
| `DOCS` | 문서 수정 |
| `STYLE` | 코드 formatting, 세미콜론 누락, 코드 자체의 변경이 없는 경우 |
| `REFACTOR` | 코드 리팩토링 |
| `TEST` | 테스트 코드, 리팩토링 테스트 코드 추가 |
| `CHORE` | 패키지 매니저 수정, 그 외 기타 수정 (ex. .gitignore) |
| `DESIGN` | CSS 등 사용자 UI 디자인 변경 |
| `COMMENT` | 필요한 주석 추가 및 변경 |
| `RENAME` | 파일 또는 폴더 명을 수정하거나 옮기는 작업만인 경우 |
| `REMOVE` | 파일을 삭제하는 작업만 수행한 경우 |
| `!BREAKING CHANGE` | 커다란 API 변경의 경우 |
| `!HOTFIX` | 급하게 치명적인 버그를 고쳐야 하는 경우 |

### 커밋 예시

```bash
# 좋은 예
FEAT[#12] 로그인 API 구현
FIX[#12] 비밀번호 암호화 누락 수정
REFACTOR[#12] JWT 토큰 생성 로직 분리
DOCS[#12] README 실행 방법 추가
CHORE[#12] .gitignore에 .env 추가
!HOTFIX[#12] 보안 취약점 긴급 패치

# 나쁜 예
update
수정
ㅋㅋ
FEAT 로그인 (이슈번호 누락)
```

### PR (Pull Request) 규칙

```markdown
## 작업 내용
- 로그인 API 구현
- JWT 토큰 발급 로직 추가

## 변경 파일
- AuthController.java
- AuthService.java
- JwtTokenProvider.java

## 테스트
- [x] 로그인 성공 테스트
- [x] 비밀번호 불일치 테스트
```

---

## 체크리스트

### 코드 작성 전
- [ ] 어느 도메인에 속하는지 확인
- [ ] 기존 코드 스타일 참고

### 코드 작성 후
- [ ] Entity에 `@Setter` 없는지 확인
- [ ] Service에 `@Transactional` 적절히 적용했는지 확인
- [ ] 예외는 적절한 GroupException을 상속했는지 확인
- [ ] Controller 반환이 `ResTemplate`인지 확인

### 커밋 전
- [ ] 불필요한 import 제거
- [ ] console.log, System.out.println 제거
- [ ] 커밋 메시지 컨벤션 준수
