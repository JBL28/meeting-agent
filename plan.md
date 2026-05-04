# 회의 STT & Agentic 협업 관리 시스템 구현 계획

> 기준 문서: `GettingStart.md`  
> 스택: Next.js (Frontend) · Spring Boot (Backend) · MySQL 8.0 (DB)  
> 범위: MVP (실시간 STT/스트리밍 제외, 텍스트 매칭 검색까지)

---

## Ground Rules (전 Phase 공통)

모든 Phase 완료 시 아래 세 가지를 반드시 수행한다.

### 1. Git Push
- Phase 완료 시점에 작업 브랜치를 원격 저장소에 push한다.
- 커밋 단위는 기능 단위로 나누되, Phase 완료 시 최소 1회 push한다.
- 커밋 메시지 형식: `[PhaseX] 기능 설명` (예: `[Phase1] JWT 인증 및 팀 관리 API 구현`)

### 2. 단위 테스트 (FIRST 원칙)
FIRST 원칙(Fast, Isolated, Repeatable, Self-validating, Timely)에 따라 작성한다.

| 레이어 | 도구 | 대상 |
|--------|------|------|
| Frontend | Vitest + Testing Library | 컴포넌트, 커스텀 훅, API 클라이언트 유틸 |
| Backend | JUnit 5 + Mockito | Service 레이어, 도메인 로직, 유효성 검증 |

- **각 Phase의 핵심 비즈니스 로직에 단위 테스트를 작성한다.**
- 외부 의존성(DB, OpenAI API, MCP 서버)은 Mockito로 모킹한다.
- 테스트 커버리지 목표: 핵심 Service 클래스 **80% 이상**
- 예시 (Phase 1): `MemberServiceTest`, `TeamPermissionEvaluatorTest`
- 예시 (Phase 3): `TranscriptionAsyncServiceTest` (OpenAI 클라이언트 모킹)
- 예시 (Phase 5): `AgentProposalServiceTest` (상태 전이 검증)

### 3. 로그 코드
모든 핵심 흐름에 수준별 로그를 작성한다.

| 수준 | 사용 기준 | 예시 |
|------|-----------|------|
| `ERROR` | 예외 발생, 외부 API 실패, DB 오류 | STT API 호출 실패, LLM 파싱 오류 |
| `WARN` | 비정상이지만 처리 가능한 상황 | known speaker reference 없이 STT 수행, 재시도 발생 |
| `INFO` | 주요 상태 전이, 비즈니스 이벤트 | Job 시작/완료, 승인 처리, 파일 업로드 완료 |
| `DEBUG` | 개발 디버깅용 (운영 환경 비활성화) | API 요청 파라미터, 파싱 중간 결과 |

- Backend: SLF4J + Logback (`log.info(...)`, `log.error(...)` 등)
- Frontend: 개발 환경에서만 `console.debug`, 운영 환경에서는 ERROR 수준만 외부 수집

### 4. 구현 내용 문서화
Phase 완료 시 해당 Phase에서 구현한 내용을 별도 파일로 작성한다.

- 파일 위치: 프로젝트 루트 (예: `Phase01.md`, `Phase02A.md`, `Phase02B.md`, ...)
- 작성 항목:
  - 구현한 기능 목록
  - 계획 대비 달라진 점 및 이유
  - 알려진 제한사항 또는 TODO
  - 다음 Phase 진입 전 확인이 필요한 사항
- 목적: 다른 팀원이 해당 파일만 읽어도 Phase에서 무엇이 어떻게 구현됐는지 파악할 수 있어야 한다.

### 5. 구현 중 불확실한 사항은 반드시 재확인
구현 도중 아래 상황에 해당하면 임의로 진행하지 않고 반드시 한 번 더 확인한다.

- 계획서의 내용이 **애매하거나 해석이 두 가지 이상** 가능한 경우
- 외부 API(OpenAI, MCP 등) 동작이 **계획과 다른 것으로 확인된 경우**
- 도메인 설계 결정이 **후속 Phase에 영향을 줄 수 있는 경우**
- 완료 기준을 충족하기 위해 **계획에 없는 추가 구현이 필요한 경우**

확인 없이 임의로 구현하면 의도와 다른 방향으로 누적될 수 있으므로, 잠깐 멈추고 질문하는 것을 원칙으로 한다.

---

## Phase 0. 프로젝트 초기 설정 및 인프라

**목표**: 모든 팀원이 동일한 환경에서 개발할 수 있도록 프로젝트 골격을 세운다.

### 0-1. 레포지토리 구조

```
/
├── frontend/          # Next.js 앱
├── backend/           # Spring Boot 앱
└── docker-compose.yml # MySQL + 로컬 개발 환경
```

### 0-2. Backend 초기 설정 (Spring Boot)

- **Spring Initializr** 기준 의존성
  - `spring-boot-starter-web`
  - `spring-boot-starter-data-jpa`
  - `spring-boot-starter-security`
  - `spring-boot-starter-validation`
  - `spring-boot-starter-actuator`
  - `mysql-connector-j`
  - `jjwt` (JWT 토큰)
  - `lombok`
- `application.yml` 환경변수 바인딩
  ```yaml
  openai:
    api-key: ${OPENAI_API_KEY}
    transcribe-model: ${OPENAI_TRANSCRIBE_MODEL:gpt-4o-transcribe-diarize}
  storage:
    base-path: ${STORAGE_BASE_PATH:./data}
  jira:
    mcp-server-url: ${JIRA_MCP_SERVER_URL}
  notion:
    mcp-server-url: ${NOTION_MCP_SERVER_URL}
  integration:
    token-encryption-key: ${INTEGRATION_TOKEN_ENCRYPTION_KEY}
  spring:
    servlet:
      multipart:
        max-file-size: 100MB
        max-request-size: 110MB
  ```
- **공통 응답 포맷** `ApiResponse<T>` 정의
  ```json
  { "success": true, "data": {}, "error": null }
  ```
- **전역 예외 핸들러** `GlobalExceptionHandler` (@ControllerAdvice)
  - `ResourceNotFoundException` → 404
  - `UnauthorizedException` → 401
  - `ForbiddenException` → 403
  - `ValidationException` → 400
- **CORS 설정**: `http://localhost:3000` 허용 (개발), 운영 도메인 별도 관리

### 0-3. Frontend 초기 설정 (Next.js)

- `app/` router 방식 사용 (Next.js 14+)
- **환경변수**: `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`
- **공통 HTTP 클라이언트** (`/lib/api.ts`): axios 또는 fetch 래퍼, JWT 자동 첨부, 401 시 로그인 리다이렉트
- **전역 상태**: Zustand 또는 React Context (인증 정보)
- **UI 라이브러리**: shadcn/ui + Tailwind CSS

### 0-4. DB 스키마 설계 및 마이그레이션

- **Flyway**로 마이그레이션 관리 (`V1__init.sql`, ...)
- 초기 테이블: `teams`, `members`, `team_members`
- 이후 Phase마다 마이그레이션 파일 추가

### 0-5. Docker Compose (개발용)

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: meetingdb
    ports: ["3306:3306"]
```

### 완료 기준

- [ ] `docker-compose up`으로 MySQL 실행 가능
- [ ] Backend `./gradlew bootRun`으로 8080 포트 기동 및 `/actuator/health` 200 응답
- [ ] Frontend `npm run dev`로 3000 포트 기동
- [ ] Backend → MySQL 연결 확인 (Flyway 마이그레이션 성공)
- [ ] **[Ground Rule]** 작업 브랜치 push 완료
- [ ] **[Ground Rule]** Phase 0 설정 검증 테스트 작성 (예: DB 연결 확인 통합 테스트)
- [ ] **[Ground Rule]** 애플리케이션 기동/종료 시 INFO 로그 출력 확인

---

## Phase 1. 인증/인가 및 팀·멤버 관리

**목표**: 사용자가 가입·로그인하고, 팀을 생성·관리하며, 권한 체계가 동작하는 상태를 만든다.

### 1-1. 도메인 모델

#### Member
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| email | VARCHAR(255) UNIQUE | 로그인 ID |
| password | VARCHAR(255) | BCrypt 해시 |
| name | VARCHAR(100) | 표시 이름 |
| created_at | DATETIME | |

#### Team
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| name | VARCHAR(200) | |
| owner_id | BIGINT FK → members | 팀 생성자 |
| created_at | DATETIME | |

#### TeamMember
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| team_id | BIGINT FK | |
| member_id | BIGINT FK | |
| role | ENUM(OWNER, ADMIN, MEMBER, VIEWER) | |
| joined_at | DATETIME | |

### 1-2. API 엔드포인트 (Backend)

#### 인증
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/api/auth/register` | 회원가입 | 누구나 |
| POST | `/api/auth/login` | 로그인 → JWT 발급 | 누구나 |
| GET | `/api/auth/me` | 내 정보 조회 | 인증 필요 |

- JWT: Access Token (1시간) + Refresh Token (7일)
- Access Token은 `Authorization: Bearer {token}` 헤더로 전달
- Refresh Token은 DB 저장 또는 HttpOnly Cookie

#### 팀 관리
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/api/teams` | 팀 생성 | 인증 |
| GET | `/api/teams/{teamId}` | 팀 조회 | VIEWER 이상 |
| PUT | `/api/teams/{teamId}` | 팀 수정 | OWNER |
| DELETE | `/api/teams/{teamId}` | 팀 삭제 | OWNER |
| GET | `/api/teams/{teamId}/members` | 팀 멤버 목록 | VIEWER 이상 |
| POST | `/api/teams/{teamId}/members` | 멤버 초대 (이메일) | ADMIN 이상 |
| PUT | `/api/teams/{teamId}/members/{memberId}/role` | 역할 변경 | OWNER |
| DELETE | `/api/teams/{teamId}/members/{memberId}` | 멤버 제거 | ADMIN 이상 |

### 1-3. 권한 체계 구현

- Spring Security `@PreAuthorize` + 커스텀 `TeamPermissionEvaluator`
- `TeamMemberService.requireRole(teamId, memberId, minRole)` 유틸 메서드
  - OWNER > ADMIN > MEMBER > VIEWER 순서
  - 역할 부족 시 `ForbiddenException` throw

### 1-4. Frontend 화면

- `/login` — 로그인 폼
- `/register` — 회원가입 폼
- `/teams` — 내가 속한 팀 목록
- `/teams/new` — 팀 생성
- `/teams/{teamId}/settings` — 팀 설정, 멤버 관리 (역할별 UI 분기)

### 완료 기준

- [ ] 회원가입 → 로그인 → JWT 발급 → 인증 API 호출 가능
- [ ] 팀 생성 시 생성자가 OWNER로 자동 등록
- [ ] VIEWER 계정이 팀 삭제 API 호출 시 403 응답
- [ ] 팀 멤버 목록 화면에서 역할 표시 및 ADMIN 이상만 초대 버튼 노출
- [ ] **[Ground Rule]** 브랜치 push 완료
- [ ] **[Ground Rule]** `MemberServiceTest`, `TeamPermissionEvaluatorTest` 작성 (JUnit 5 + Mockito)
- [ ] **[Ground Rule]** 로그인 성공/실패, 팀 생성, 권한 거부 시 INFO/WARN 로그 출력 확인

---

## Phase 2A. Voice Sample 등록 및 회의·음성 파일 업로드

**목표**: Voice sample 등록 API와 회의 생성·음성 파일 업로드 API를 완성한다. 웹 녹음 UI는 Phase 2B에서 별도로 구현하며, 이 Phase에서는 파일 선택(드래그&드랍) 방식 업로드만 제공한다.

> **분리 이유**: MediaRecorder, Blob, webm 처리 등 브라우저 녹음 관련 구현을 격리해 STT 파이프라인(Phase 3)과의 의존을 최소화한다.

### 2A-1. 도메인 모델

#### VoiceSample
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| member_id | BIGINT FK → members | |
| team_id | BIGINT FK → teams | |
| file_path | VARCHAR(500) | 서버 저장 경로 |
| file_name | VARCHAR(255) | 원본 파일명 |
| duration_seconds | INT | 측정된 길이 (초) |
| created_at | DATETIME | |
| consent_agreed_at | DATETIME | 동의 시각 |

#### Meeting
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| team_id | BIGINT FK | |
| title | VARCHAR(300) | |
| scheduled_at | DATETIME | 예정 일시 |
| status | ENUM | DRAFT/RECORDING/RECORDED/TRANSCRIBING/TRANSCRIBED/MINUTES_GENERATED/ARCHIVED |
| created_by | BIGINT FK → members | |
| created_at | DATETIME | |

#### MeetingParticipant
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| meeting_id | BIGINT FK | |
| member_id | BIGINT FK | |

#### AudioFile
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| meeting_id | BIGINT FK | |
| file_path | VARCHAR(500) | |
| file_name | VARCHAR(255) | |
| file_size | BIGINT | bytes |
| duration_seconds | INT | 측정된 길이 (초, nullable) |
| uploaded_at | DATETIME | |

### 2A-2. 파일 크기·형식·길이 제한 정책

| 파일 종류 | 허용 형식 | 최대 크기 | 길이 제한 |
|-----------|-----------|-----------|-----------|
| VoiceSample | wav, mp3, webm | **10MB** | **2~10초** |
| MeetingAudio | wav, mp3, webm, m4a | **100MB** | **최대 60분** |

#### VoiceSample duration 검증 방법

MultipartFile만으로는 오디오 길이를 알 수 없다. MVP에서는 다음 정책을 따른다:

- **서버 검증**: `ffprobe`가 서버에 설치된 경우 업로드 직후 duration을 측정해 2~10초 범위를 검증한다.
  ```
  ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 {파일경로}
  ```
- **ffprobe 미설치 환경**: duration 검증을 WARN 처리하고 서버에서는 파일 크기와 확장자만 검증한다. Frontend 녹음 UI에서 2~10초를 강제한다.
- **실제 운영 환경에서는 서버 ffprobe 검증을 필수로 한다.**

#### MeetingAudio duration 검증 방법

- ffprobe 사용 가능 시 업로드 직후 측정, 60분 초과 시 400 에러.
- ffprobe 미사용 시 100MB 파일 크기 제한으로 간접 제어한다.
- Frontend에서 녹음 시작 시 60분 타이머를 두어 초과 시 자동 중지 및 안내.

#### known speaker reference 인원 제한

- known speaker reference는 최대 4명 제한(공식 문서 재확인 필수)이므로:
  - 참가자 ≤ 4명: 모두 known speaker reference로 전달한다.
  - 참가자 ≥ 5명: known speaker reference 없이 diarization만 수행한다. (SpeakerMapping UI에서 전원 수동 매핑)

### 2A-3. 파일 저장 인터페이스

```java
// backend/src/main/java/com/.../storage/StorageService.java
public interface StorageService {
    String save(MultipartFile file, String category); // "voice-samples" | "meetings"
    Resource load(String filePath);
    void delete(String filePath);
}

// MVP 구현체 (추후 S3StorageService 교체)
@Service
public class LocalStorageService implements StorageService { ... }
```

- 저장 경로: `${STORAGE_BASE_PATH}/{category}/{yyyy}/{MM}/{uuid}.{ext}`

### 2A-4. API 엔드포인트 (Backend)

#### Voice Sample
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/api/teams/{teamId}/members/{memberId}/voice-samples` | 샘플 등록 (`consent=true` 필수, multipart) | 본인 또는 ADMIN |
| GET | `/api/teams/{teamId}/members/{memberId}/voice-samples` | 샘플 목록 | MEMBER 이상 |
| DELETE | `/api/voice-samples/{sampleId}` | 샘플 삭제 (본인만) | 본인 |

- 동의(`consent=true`) 없이 등록 시 400 에러
- 파일 크기·길이 초과 시 400 에러 (ffprobe 사용 시)

#### 회의
| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/api/teams/{teamId}/meetings` | 회의 생성 (status: DRAFT) | MEMBER 이상 |
| GET | `/api/teams/{teamId}/meetings` | 회의 목록 | VIEWER 이상 |
| GET | `/api/meetings/{meetingId}` | 회의 상세 | VIEWER 이상 |
| POST | `/api/meetings/{meetingId}/participants` | 참가자 추가 | 회의 생성자 또는 ADMIN |
| POST | `/api/meetings/{meetingId}/audio` | 음성 파일 업로드 → status: RECORDED | MEMBER 이상 |

### 2A-5. 파일 다운로드 보안

- 음성 파일은 인증 없이 직접 URL 접근 불가
- `GET /api/audio/{audioFileId}/stream` → Authorization 헤더 검증 후 `StreamingResponseBody`로 스트리밍

### 2A-6. Frontend 화면

- `/teams/{teamId}/profile` — Voice Sample 파일 업로드 (동의 체크박스 필수, 녹음 UI는 Phase 2B)
- `/meetings/new` — 회의 생성 폼
- `/meetings/{meetingId}` — 회의 상세 (참가자 목록, 파일 선택/드래그&드랍으로 음성 파일 업로드)

### 완료 기준

- [ ] 동의 없이 voice sample 업로드 시 400 응답
- [ ] voice sample 10MB 초과 시 400 응답
- [ ] voice sample 10초 초과 시 400 응답 (ffprobe 사용 시) 또는 WARN 로그 (미사용 시)
- [ ] 본인이 아닌 멤버의 voice sample 삭제 시 403 응답
- [ ] 음성 파일 업로드 후 AudioFile DB 저장 및 Meeting status RECORDED 변경
- [ ] MeetingAudio 100MB 초과 시 400 응답
- [ ] 인증 없이 음성 파일 스트리밍 URL 접근 시 401 응답
- [ ] **[Ground Rule]** 브랜치 push 완료
- [ ] **[Ground Rule]** `VoiceSampleServiceTest` (동의 검증, 크기 제한), `AudioFileServiceTest` 작성
- [ ] **[Ground Rule]** 파일 업로드 성공/실패, duration 검증 결과 INFO/WARN 로그 출력 확인

---

## Phase 2B. 웹 녹음 UI

**목표**: Phase 2A에서 구축한 음성 업로드 API를 그대로 활용해, 브라우저에서 직접 녹음하는 UI를 추가한다. 백엔드 변경 없음.

> **전제**: Phase 2A의 `POST /api/meetings/{meetingId}/audio`가 완성된 상태에서 시작한다.

### 2B-1. 구현 범위

- `MediaRecorder` API로 webm Blob 생성
- 녹음 완료 후 기존 업로드 API(`/audio`)를 재사용
- **구현 제외**: 실시간 STT, chunk upload, streaming

### 2B-2. Frontend 구현

- `/meetings/{meetingId}/record` 페이지 또는 모달
  - "녹음 시작" → `navigator.mediaDevices.getUserMedia({ audio: true })`
  - 녹음 중: 타이머 표시 (분:초), **60분 도달 시 자동 중지 및 안내**
  - Meeting status → `RECORDING` (API 호출)
  - "녹음 중지" → `MediaRecorder.stop()` → Blob 생성
  - Blob을 `FormData`에 담아 `POST /api/meetings/{meetingId}/audio` 호출
  - 파일 포맷: `audio/webm;codecs=opus` (Chrome 기준)
  - Voice Sample 녹음 UI: 2초 미만 시 "더 길게 녹음해주세요", 10초 초과 시 자동 중지

### 완료 기준

- [ ] 브라우저에서 녹음 시작 → 중지 → 업로드 → AudioFile DB 저장 확인
- [ ] 녹음 중 Meeting status가 RECORDING → RECORDED로 순차 변경
- [ ] 마이크 권한 거부 시 사용자에게 안내 메시지 표시
- [ ] Voice Sample 녹음 시 10초 초과 시 자동 중지
- [ ] MeetingAudio 녹음 60분 도달 시 자동 중지 및 저장 안내
- [ ] **[Ground Rule]** 브랜치 push 완료
- [ ] **[Ground Rule]** 녹음 훅 Vitest 테스트 작성 (MediaRecorder 모킹)
- [ ] **[Ground Rule]** 녹음 시작/중지/업로드 완료 시 콘솔 INFO 로그 확인

---

## Phase 3. STT 처리 파이프라인 (비동기)

**목표**: 업로드된 음성 파일을 gpt-4o-transcribe-diarize로 처리하고, 발화자 식별 결과를 DB에 저장하며, 상태를 polling으로 확인할 수 있다.

### 3-1. 도메인 모델

#### TranscriptionJob
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| meeting_id | BIGINT FK | |
| audio_file_id | BIGINT FK | |
| status | ENUM | CREATED/PROCESSING/COMPLETED/FAILED/CANCELED |
| error_message | TEXT | 실패 사유 |
| raw_response_path | VARCHAR(500) | STT 원본 JSON 저장 경로 (파일시스템) |
| started_at | DATETIME | |
| completed_at | DATETIME | |
| created_at | DATETIME | |

> **Meeting-TranscriptionJob 상태 정책**
> - `TranscriptionJob.status`가 원천 상태다. `Meeting.status`는 Job 상태 변경 시 함께 갱신하는 집계 상태다.
> - MVP에서 하나의 Meeting에는 **활성 TranscriptionJob(CREATED/PROCESSING) 1개만** 허용한다. 재시도는 기존 job이 FAILED/CANCELED일 때만 가능하다.
> - Job 상태 변경 책임: `TranscriptionAsyncService`가 job을 갱신하고, 완료/실패 시 Meeting status도 함께 갱신한다.

#### TranscriptSegment
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| transcription_job_id | BIGINT FK | |
| speaker | VARCHAR(100) | API 응답의 speaker 값 그대로 저장 (형식 가정 금지) |
| member_id | BIGINT FK → members (nullable) | SpeakerMapping 후 채워짐 |
| start_time | DOUBLE | 초 단위 |
| end_time | DOUBLE | 초 단위 |
| text | TEXT | 발화 내용 |
| sequence | INT | 발화 순서 |

> **speaker 필드 주의**: API 응답의 `speaker` 값은 특정 형식("Speaker 1" 등)을 가정하지 않고 그대로 저장한다. 화면 표시용 label은 SpeakerMapping 또는 별도 변환 로직에서 처리한다.

#### SpeakerMapping
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| transcription_job_id | BIGINT FK | |
| speaker | VARCHAR(100) | TranscriptSegment.speaker와 동일한 값 |
| member_id | BIGINT FK → members | 매핑된 실제 멤버 |
| is_auto_mapped | BOOLEAN | known speaker reference로 자동 매핑 여부 |
| confirmed_by | BIGINT FK → members (nullable) | 사용자가 수동 확인/수정한 경우 |

### 3-2. STT 처리 흐름

```
[음성 파일 업로드 완료 (Meeting: RECORDED)]
        ↓
POST /api/meetings/{meetingId}/transcription
        ↓
TranscriptionJob 생성 (status: CREATED)
        ↓
즉시 jobId 반환 (HTTP 202 Accepted)
        ↓
@Async STT Worker 실행
        ↓
  job status → PROCESSING
  Meeting status → TRANSCRIBING
        ↓
OpenAI API 호출 (아래 주의사항 참고)
        ↓
  성공: TranscriptSegment 저장
        raw response JSON → 파일시스템 저장
        job status → COMPLETED
        Meeting status → TRANSCRIBED
  실패: errorMessage 저장
        error JSON → 파일시스템 저장
        job status → FAILED
        Meeting status → RECORDED (재시도 가능 상태)
```

### 3-3. OpenAI API 호출 주의사항

> **구현 전 반드시 OpenAI 공식 Audio Transcriptions API 문서를 확인한다.**

- `model=gpt-4o-transcribe-diarize`를 사용한다.
- speaker annotation을 받기 위해 **`response_format=diarized_json`** 을 요청한다.
- **회의 음성은 대부분 30초를 초과하므로 `chunking_strategy=auto`를 기본값으로 사용한다.**
- known speaker reference 관련 필드명과 형식(data URL? 파일 업로드?)은 공식 SDK/문서 기준으로 확인 후 구현한다.
  - 예상 필드: `known_speaker_names`, `known_speaker_references`
  - **예시 코드에 직접 박지 않는다. 구현 시점에 재확인한다.**
- diarized_json 응답의 speaker 필드 형식을 가정하지 않는다. 응답 구조를 문서로 확인 후 파싱한다.

### 3-4. STT raw response 보관 정책

- raw response JSON은 디버깅용이며 사용자 화면에 미노출
- 저장 경로: `${STORAGE_BASE_PATH}/stt-raw/{jobId}.json` (성공), `{jobId}.error.json` (실패)
- **MVP에서는 보관 기간 30일 정책만 명시하고, 자동 삭제 스케줄러는 고도화 범위로 둔다.** 운영 시 수동 삭제 또는 OS cron으로 관리한다.

### 3-5. Speaker Identification Fallback 정책

| 상황 | 처리 방식 |
|------|-----------|
| 참가자 5명 이상 | known speaker reference 없이 diarization만 수행, 전원 수동 매핑 |
| API가 매핑 신뢰도 미제공 | `is_auto_mapped=true`여도 사용자 확인 요청 |
| 자동 매핑 결과 존재 | SpeakerMapping UI에서 사용자가 수정 가능 |
| SpeakerMapping 저장 후 | TranscriptSegment.member_id 업데이트 → 회의록 생성에 반영 |

### 3-6. API 엔드포인트 (Backend)

| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/api/meetings/{meetingId}/transcription` | STT 작업 시작 → 202 + jobId | MEMBER 이상 |
| GET | `/api/transcription-jobs/{jobId}` | 작업 상태 조회 | VIEWER 이상 |
| GET | `/api/transcription-jobs/{jobId}/segments` | 발화 세그먼트 목록 | VIEWER 이상 |
| POST | `/api/transcription-jobs/{jobId}/cancel` | 작업 취소 (PROCESSING만) | 회의 생성자 |
| POST | `/api/transcription-jobs/{jobId}/retry` | 재시도 (FAILED만) | MEMBER 이상 |
| POST | `/api/transcription-jobs/{jobId}/speaker-mapping` | speaker → 멤버 매핑 저장 | MEMBER 이상 |

speaker-mapping 요청 body:
```json
{
  "mappings": [
    { "speaker": "API 응답에서 받은 speaker 값 그대로", "memberId": 42 }
  ]
}
```

### 3-7. 비동기 처리 구현

```java
@Service
public class TranscriptionAsyncService {

    @Async("transcriptionExecutor")
    public CompletableFuture<Void> processTranscription(Long jobId) {
        // 1. job status → PROCESSING, Meeting status → TRANSCRIBING
        log.info("STT 처리 시작: jobId={}", jobId);
        // 2. AudioFile 로드
        // 3. 참가자 VoiceSample 로드 (4명 이하인 경우만, 이상이면 WARN 로그)
        // 4. OpenAI API 호출 (response_format=diarized_json, chunking_strategy=auto)
        // 5. 응답 파싱 → TranscriptSegment 저장
        // 6. raw response JSON → 파일시스템 저장
        // 7. job status → COMPLETED, Meeting status → TRANSCRIBED
        //    (실패 시) job status → FAILED, Meeting status → RECORDED
        //    log.error("STT 처리 실패: jobId={}, error={}", jobId, e.getMessage());
    }
}

@Bean("transcriptionExecutor")
public Executor transcriptionExecutor() {
    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    exec.setCorePoolSize(2);
    exec.setMaxPoolSize(5);
    exec.setQueueCapacity(50);
    return exec;
}
```

### 3-8. Frontend Polling

- STT 작업 시작 후 `jobId`를 받아 2초 간격 polling 시작
- status가 `COMPLETED` 또는 `FAILED`가 될 때까지 반복 후 자동 종료
- 진행 상태 표시 UI: 스피너 + 현재 상태 텍스트
- COMPLETED → SpeakerMapping UI 전환
- FAILED → 에러 메시지 + 재시도 버튼

### 3-9. SpeakerMapping UI

- API 응답의 고유 speaker 값 목록 표시
- 각 speaker별 샘플 발화 2~3개 표시 + 드롭다운으로 멤버 선택
- `is_auto_mapped=true` 항목은 "자동 감지됨" 표시 (수정 가능)
- "매핑 저장" 버튼 → `POST /api/transcription-jobs/{jobId}/speaker-mapping`

### 완료 기준

- [ ] RECORDED 상태가 아닌 meeting에서 STT 시작 시 400 응답
- [ ] 활성 TranscriptionJob이 있는 meeting에서 재시작 시 409 응답
- [ ] STT 완료 후 TranscriptSegment가 `speaker`, `start_time`, `end_time`, `text` 포함해 저장
- [ ] raw STT 응답 JSON이 파일시스템에 저장, 에러 시 error.json 저장
- [ ] FAILED 상태에서만 retry 가능 (PROCESSING 중 retry 시 409)
- [ ] 참가자 5명 이상인 회의에서 STT 시 known speaker reference 없이 처리 + WARN 로그
- [ ] Frontend polling이 COMPLETED/FAILED 도달 시 자동 종료
- [ ] **[Ground Rule]** 브랜치 push 완료
- [ ] **[Ground Rule]** `TranscriptionAsyncServiceTest` 작성 (OpenAI 클라이언트 모킹, 상태 전이 검증)
- [ ] **[Ground Rule]** STT 시작/완료/실패 시 INFO/ERROR 로그 출력 확인

---

## Phase 4. 회의록 생성·관리 및 검색

**목표**: STT 결과를 LLM으로 구조화해 회의록을 생성하고, 조회·수정·삭제 및 텍스트 검색이 가능하다.

### 4-1. 도메인 모델

#### MinutesGenerationJob
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| meeting_id | BIGINT FK | |
| status | ENUM(CREATED, PROCESSING, COMPLETED, FAILED) | |
| error_message | TEXT | |
| started_at | DATETIME | |
| completed_at | DATETIME | |
| created_at | DATETIME | |

> STT와 MCP 실행이 비동기 Job으로 관리되므로, 회의록 생성도 일관성을 위해 MinutesGenerationJob으로 추적한다. Frontend는 jobId로 polling한다.

#### MeetingMinutes
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| meeting_id | BIGINT FK UNIQUE | |
| generation_job_id | BIGINT FK → minutes_generation_jobs | |
| title | VARCHAR(300) | 회의 제목 |
| meeting_date | DATE | 회의 일시 |
| full_summary | TEXT | 전체 요약 |
| raw_content | LONGTEXT | LLM 응답 원본 JSON (Jackson으로 파싱) |
| created_at | DATETIME | |
| updated_at | DATETIME | |

> `raw_content`는 MySQL JSON 컬럼이 아닌 **LONGTEXT**로 선언한다. JSON 파싱은 애플리케이션(Jackson)에서 처리한다.

#### ActionItem
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| minutes_id | BIGINT FK | |
| assignee_id | BIGINT FK → members | 담당자 |
| content | TEXT | 내용 |
| due_date | DATE | 마감일 |
| status | ENUM(TODO, IN_PROGRESS, DONE) | |

#### MemberMinutesSummary
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| minutes_id | BIGINT FK | |
| member_id | BIGINT FK | |
| progress | TEXT | 진행 상황 |
| issues | TEXT | 주요 이슈 |
| next_tasks | TEXT | 다음 작업 |

> **결정사항(decisions)**: MVP에서는 `MeetingMinutes.raw_content`에만 저장하고 별도 Decision 테이블을 두지 않는다. 화면에서는 raw_content를 파싱해 표시한다. 결정사항 별도 관리가 필요하면 고도화 범위로 분리한다.

### 4-2. 회의록 생성 (LLM 기반 구조화, 비동기)

TRANSCRIBED 상태에서 수동 트리거:

**LLM 호출 방식**:
- **OpenAI Structured Outputs 또는 JSON Schema 기반 응답으로 고정한다.** 일반 텍스트 응답 + 수동 파싱은 사용하지 않는다.
- **최신 OpenAI API 공식 문서를 확인**해 JSON Schema response_format을 적용한다.
- 입력: 전체 TranscriptSegment (speaker → member 이름 치환, SpeakerMapping 기준)
- 출력 JSON Schema:
  ```json
  {
    "title": "string",
    "full_summary": "string",
    "member_summaries": [
      { "member_name": "string", "progress": "string", "issues": "string", "next_tasks": "string" }
    ],
    "decisions": ["string"],
    "action_items": [
      { "assignee": "string", "content": "string", "due_date": "string (YYYY-MM-DD)" }
    ]
  }
  ```
- **JSON Schema 파싱 실패 시 MinutesGenerationJob → FAILED로 저장하고 종료한다. 빈 회의록을 저장하지 않는다.**
- 사용자는 FAILED 상태에서 재시도할 수 있다.

**처리 흐름**:
```
[TRANSCRIBED] → MinutesGenerationJob 생성 (CREATED)
              → 202 + jobId 반환
              → @Async 실행 → PROCESSING
              → LLM Structured Outputs 호출
              → 성공: MeetingMinutes/MemberMinutesSummary/ActionItem 저장
                       Job → COMPLETED, Meeting → MINUTES_GENERATED
              → 실패: Job → FAILED, Meeting status 유지 (TRANSCRIBED)
```

### 4-3. API 엔드포인트 (Backend)

| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/api/meetings/{meetingId}/minutes/generate` | 회의록 생성 시작 → 202 + jobId | MEMBER 이상 |
| GET | `/api/minutes-generation-jobs/{jobId}` | 생성 작업 상태 조회 (polling용) | VIEWER 이상 |
| GET | `/api/meetings/{meetingId}/minutes` | 회의록 조회 | VIEWER 이상 |
| PUT | `/api/meetings/{meetingId}/minutes` | 회의록 수정 | MEMBER 이상 |
| DELETE | `/api/meetings/{meetingId}/minutes` | 회의록 삭제 | ADMIN 이상 |
| GET | `/api/meetings/{meetingId}/minutes/segments` | 원본 발화 로그 조회 | VIEWER 이상 |
| PUT | `/api/action-items/{actionItemId}` | 액션 아이템 수정 | MEMBER 이상 |
| PUT | `/api/action-items/{actionItemId}/status` | 액션 아이템 상태 변경 | 본인 또는 ADMIN |

### 4-4. 검색 기능

- **SearchService 인터페이스** 정의 (추후 ES/Vector 교체 대비)
  ```java
  public interface SearchService {
      Page<MeetingSearchResult> search(Long teamId, String keyword, Pageable pageable);
  }
  ```
- **MVP 구현체**: `JpaSearchService`
  - 검색 대상: 회의 제목, MeetingMinutes full_summary, TranscriptSegment text, ActionItem content
  - JPA JPQL `LIKE :keyword` (바인딩 파라미터 사용, SQL Injection 방지)
  - 결과: 회의 단위 반환, 매칭된 snippet (키워드 앞뒤 50자) 포함

| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| GET | `/api/teams/{teamId}/meetings/search?keyword=xxx&page=0&size=20` | 회의록 검색 | VIEWER 이상 |

### 4-5. Frontend 화면

- **회의록 조회 페이지** (`/meetings/{meetingId}/minutes`)
  - 전체 요약, 멤버별 진행 상황/이슈/다음 작업, 결정사항, 액션 아이템 표
  - 원본 발화 로그 (토글), 편집 모드 (MEMBER 이상)
  - generation_status=FAILED 시 "재생성" 버튼
  - 생성 중 polling (MinutesGenerationJob)
- **검색 UI** (`/teams/{teamId}/search`): 키워드, 결과 카드, 페이지네이션

### 완료 기준

- [ ] TRANSCRIBED 상태가 아닌 meeting에서 generate 시 400 응답
- [ ] LLM Structured Outputs 파싱 성공 시 MeetingMinutes/MemberMinutesSummary/ActionItem 생성
- [ ] 파싱 실패 시 MinutesGenerationJob → FAILED, 빈 회의록 저장 없음
- [ ] MinutesGenerationJob polling으로 Frontend 상태 감지 가능
- [ ] VIEWER가 회의록 수정 API 호출 시 403
- [ ] 키워드 검색 시 snippet 포함 응답, 팀 내 회의만 반환
- [ ] **[Ground Rule]** 브랜치 push 완료
- [ ] **[Ground Rule]** `MinutesGenerationServiceTest` (LLM 클라이언트 모킹, 상태 전이), `JpaSearchServiceTest` 작성
- [ ] **[Ground Rule]** 회의록 생성 시작/완료/실패 시 INFO/ERROR 로그 출력 확인

---

## Phase 5. Agentic 작업 제안 및 승인 플로우

**목표**: 회의록을 기반으로 LLM이 Jira/Notion 작업을 제안하고, 사용자가 명시적으로 승인한 것만 MCP Gateway를 통해 실행된다.

### 5-1. 도메인 모델

#### AgentRun
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| meeting_minutes_id | BIGINT FK | |
| proposal_id | BIGINT FK → agent_proposals (nullable) | MCP_EXECUTION run에만 사용 |
| run_type | ENUM(PROPOSAL_GENERATION, MCP_EXECUTION) | |
| status | ENUM(PENDING, RUNNING, COMPLETED, FAILED) | |
| started_at | DATETIME | |
| completed_at | DATETIME | |
| error_message | TEXT | |

> - `PROPOSAL_GENERATION` run: `meeting_minutes_id`만 설정, `proposal_id`는 null
> - `MCP_EXECUTION` run: `meeting_minutes_id` + `proposal_id` 모두 설정

#### AgentProposal

**상태 정의**:

| 상태 | 설명 |
|------|------|
| PENDING_APPROVAL | LLM 생성 완료, 사용자 검토 대기 |
| APPROVED | 사용자 승인 |
| REJECTED | 사용자 거절 |
| REVISION_REQUESTED | 수정 요청 (재제안 필요) |
| EXECUTING | MCP 실행 중 |
| EXECUTED | MCP 실행 완료 |
| FAILED | MCP 실행 실패 |

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| minutes_id | BIGINT FK | |
| generation_run_id | BIGINT FK → agent_runs | 생성한 PROPOSAL_GENERATION run |
| proposal_type | ENUM(JIRA_TICKET, NOTION_DOC, ASSIGNEE_TASK) | |
| title | VARCHAR(300) | 제안 제목 |
| description | LONGTEXT | 상세 내용 |
| status | ENUM (위 상태표) | |
| revision_comment | TEXT | 수정 요청 내용 |
| proposed_at | DATETIME | |
| reviewed_at | DATETIME | |
| reviewed_by | BIGINT FK → members (nullable) | |

> **ApprovalRequest 제거**: `AgentProposal`에 `reviewed_by`, `reviewed_at`, `revision_comment`를 직접 두어 MVP에서의 승인 이력을 관리한다. 감사 로그가 필요하면 고도화 단계에서 별도 테이블로 분리한다.

#### McpExecutionLog
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| proposal_id | BIGINT FK | |
| agent_run_id | BIGINT FK | |
| mcp_target | ENUM(JIRA, NOTION) | |
| tool_name | VARCHAR(200) | MCP tool 이름 (예: create_issue) |
| request_payload | LONGTEXT | tool call 요청 내용 |
| response_payload | LONGTEXT | tool call 응답 내용 |
| success | BOOLEAN | |
| error_message | TEXT | |
| executed_at | DATETIME | |

#### IntegrationAccount
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| team_id | BIGINT FK | |
| service | ENUM(JIRA, NOTION) | |
| encrypted_access_token | VARCHAR(500) | AES-GCM 암호화 저장 |
| workspace_info | LONGTEXT | JSON (project key 등) |

> **Integration token 보안 정책**
> - access_token은 **AES-GCM**으로 암호화해 저장한다.
> - 암호화 키는 환경변수 `INTEGRATION_TOKEN_ENCRYPTION_KEY`로 관리한다.
> - **MVP에서 MockMcpGateway를 기본으로 사용하는 경우, 실제 access_token 저장은 생략할 수 있다.**

### 5-2. MCP 인증 및 Gateway 설계

MCP 서버의 인증 관리 방식에 따라 두 가지 경우가 있다:

| 방식 | 설명 |
|------|------|
| MCP 서버가 자체 인증 관리 | 서비스는 MCP 서버 URL + 워크스페이스 설정만 저장한다. IntegrationAccount의 token은 불필요 |
| 서비스가 사용자 토큰 직접 관리 | IntegrationAccount에 AES-GCM 암호화 저장 |

**MVP에서는 MockMcpGateway를 기본으로 하고, 실제 Jira/Notion 토큰 입력은 고도화 범위로 둔다.**

```java
// backend/src/main/java/com/.../mcp/McpGateway.java
public interface McpGateway {
    McpToolResult executeTool(String toolName, Map<String, Object> params);
}

@Service("jiraMcpGateway")
public class JiraMcpGateway implements McpGateway { ... }

@Service("notionMcpGateway")
public class NotionMcpGateway implements McpGateway { ... }

@Service("mockMcpGateway")  // 개발/MVP 기본값
public class MockMcpGateway implements McpGateway { ... }
```

- Agent는 직접 tool을 실행하지 않고 AgentProposal만 생성한다.
- 사용자 승인 후 Backend가 승인된 proposal을 MCP tool call로 변환한다.
- 모든 tool call request/response를 McpExecutionLog에 저장한다.
- **MCP 공식 프로토콜 문서 확인** 후 구현한다.

### 5-3. Agentic 제안 생성 흐름 (비동기)

```
[MINUTES_GENERATED]
        ↓
POST /api/meetings/{meetingId}/minutes/agent-proposals/generate
        ↓
AgentRun 생성 (run_type: PROPOSAL_GENERATION, proposal_id: null)
        ↓
즉시 agentRunId 반환 (HTTP 202)
        ↓
@Async → AgentRun status: RUNNING
        ↓
LLM Structured Outputs 호출 (회의록 기반 제안 생성)
        ↓
  성공: AgentProposal 생성 (status: PENDING_APPROVAL)
        AgentRun → COMPLETED
  실패: AgentRun → FAILED
```

### 5-4. 승인 및 MCP 실행 흐름

```
PENDING_APPROVAL → APPROVED
  → AgentRun 생성 (run_type: MCP_EXECUTION, proposal_id: 설정)
  → EXECUTING
  → McpGateway.executeTool() 호출
  → McpExecutionLog 저장 (요청/응답)
  → EXECUTED (성공) / FAILED (실패)

PENDING_APPROVAL → REJECTED → 종료
PENDING_APPROVAL → REVISION_REQUESTED
  → revision_comment 기반 LLM 재제안
  → 새 AgentProposal (PENDING_APPROVAL)
```

### 5-5. API 엔드포인트 (Backend)

| Method | Path | 설명 | 권한 |
|--------|------|------|------|
| POST | `/api/meetings/{meetingId}/minutes/agent-proposals/generate` | 제안 생성 시작 → 202 + agentRunId | MEMBER 이상 |
| GET | `/api/agent-runs/{agentRunId}` | AgentRun 상태 조회 (polling) | VIEWER 이상 |
| GET | `/api/meetings/{meetingId}/minutes/agent-proposals` | 제안 목록 | VIEWER 이상 |
| POST | `/api/agent-proposals/{proposalId}/approve` | 승인 → MCP 실행 시작 | ADMIN 이상 |
| POST | `/api/agent-proposals/{proposalId}/reject` | 거절 | ADMIN 이상 |
| POST | `/api/agent-proposals/{proposalId}/request-revision` | 수정 요청 (body: revision_comment) | ADMIN 이상 |
| GET | `/api/agent-proposals/{proposalId}/execution-log` | MCP 실행 로그 조회 | ADMIN 이상 |
| POST | `/api/teams/{teamId}/integrations/jira` | Jira 연동 설정 저장 (MVP: URL/워크스페이스만) | ADMIN 이상 |
| POST | `/api/teams/{teamId}/integrations/notion` | Notion 연동 설정 저장 (MVP: URL/워크스페이스만) | ADMIN 이상 |

### 5-6. Frontend 화면

- **제안 목록 패널** (회의록 페이지 하단)
  - 각 제안: type 아이콘, 제목, 설명 요약, 상태 배지
  - PENDING_APPROVAL → "승인" / "거절" / "수정 요청" (ADMIN 이상)
  - EXECUTED → "실행 결과 보기" (McpExecutionLog)
  - FAILED → 에러 메시지 표시
- **수정 요청 모달**: revision_comment 입력 → POST request-revision
- **연동 설정 페이지** (`/teams/{teamId}/integrations`): MCP 서버 URL/워크스페이스 입력 (MVP: token 입력 불필요)

### 완료 기준

- [ ] 회의록 기반으로 Jira 티켓, Notion 문서 AgentProposal이 PENDING_APPROVAL로 저장
- [ ] MEMBER가 승인 API 호출 시 403
- [ ] PENDING_APPROVAL 상태가 아닌 proposal 승인 시도 시 400
- [ ] 승인 후 McpGateway 호출 및 McpExecutionLog 저장 (성공/실패 모두)
- [ ] 수정 요청 → 재제안 루프 1회 이상 동작 확인
- [ ] MockMcpGateway로 E2E 승인 플로우 동작 확인
- [ ] AgentRun polling으로 제안 생성 완료 감지 가능
- [ ] **[Ground Rule]** 브랜치 push 완료
- [ ] **[Ground Rule]** `AgentProposalServiceTest` (상태 전이, McpGateway 모킹), `AgentRunServiceTest` 작성
- [ ] **[Ground Rule]** 제안 생성/승인/MCP 실행/실패 시 INFO/ERROR 로그 출력 확인

---

## Phase 6. 보안 강화 및 통합 테스트·마무리

**목표**: 보안 취약점을 점검하고, 전체 플로우 E2E 테스트를 통과하며, 운영 배포 가능한 상태로 마무리한다.

### 6-1. 보안 점검 항목

| 항목 | 기준 |
|------|------|
| API Key 노출 | OPENAI_API_KEY가 응답, 로그, Frontend에 절대 미노출 |
| 파일 접근 | 음성/voice sample 파일은 인증 후 스트리밍만 가능 |
| 권한 검증 | 모든 API에 팀 소속 여부 + 역할 검증 |
| SQL Injection | JPA Parameterized Query만 사용 (LIKE도 바인딩 파라미터) |
| XSS | Frontend 입력값 이스케이프, Content-Security-Policy 헤더 |
| CORS | 허용 도메인 화이트리스트만 |
| 파일 업로드 | MIME 타입 검증, voice sample 10MB / meeting audio 100MB 제한 |
| voice sample 삭제 | 본인만 삭제 가능 |
| STT raw response | 사용자 화면 미노출, 30일 정책 명시 (MVP: 수동 삭제) |
| Integration token | AES-GCM 암호화, `INTEGRATION_TOKEN_ENCRYPTION_KEY` 환경변수 관리 |
| MCP 실행 | APPROVED 상태 proposal만 실행 가능, status 검증 필수 |

### 6-2. 에러 처리 점검

| 상황 | 처리 방식 |
|------|-----------|
| STT API 타임아웃 | TranscriptionJob → FAILED, errorMessage 저장, 재시도 안내 |
| LLM 응답 파싱 실패 | MinutesGenerationJob → FAILED (빈 회의록 저장 없음) |
| MCP 서버 연결 실패 | AgentProposal → FAILED, McpExecutionLog 저장 |
| DB 트랜잭션 실패 | 롤백 후 500 응답 |
| 파일 크기 초과 | Spring multipart 예외 → 400 응답 |

### 6-3. 통합 테스트 시나리오

**Happy Path**:
1. 회원가입 → 팀 생성 → 팀원 초대 (4명 이하)
2. 팀원 voice sample 등록 (2~10초, 동의 체크)
3. 회의 생성 → 참가자 등록 → 음성 파일 업로드
4. STT 작업 시작 → polling → COMPLETED
5. SpeakerMapping
6. 회의록 생성 → polling → COMPLETED → 조회 → 수정
7. 키워드 검색
8. Agent 제안 생성 → ADMIN 승인 → MockMcpGateway 실행 → McpExecutionLog 확인

**Edge Cases**:
- voice sample 11초 업로드 → 400 응답
- MeetingAudio 101MB 업로드 → 400 응답
- 참가자 5명 이상 → known speaker 없이 diarization + WARN 로그
- STT FAILED → retry → 재처리
- PROCESSING 중 retry → 409 응답
- LLM 파싱 실패 → FAILED → 재생성 성공
- 제안 수정 요청 → 재제안 → 승인
- MEMBER가 승인 시도 → 403

### 6-4. 환경변수 최종 점검

```
OPENAI_API_KEY=...
OPENAI_TRANSCRIBE_MODEL=gpt-4o-transcribe-diarize
JIRA_MCP_SERVER_URL=...
NOTION_MCP_SERVER_URL=...
DB_URL=jdbc:mysql://localhost:3306/meetingdb
DB_USERNAME=...
DB_PASSWORD=...
STORAGE_BASE_PATH=/data/meeting-storage
INTEGRATION_TOKEN_ENCRYPTION_KEY=...   # AES-GCM 암호화 키
```

### 6-5. 인터페이스 교체 검증

- `StorageService` 구현체(Local → S3) 교체 시 기존 코드 무변경 확인
- `SearchService` 구현체(Jpa → ES) 교체 시 동일 응답 포맷 확인
- `McpGateway` 구현체(Mock → 실제) 교체 시 McpExecutionLog 형식 유지 확인

### 완료 기준

- [ ] 전체 Happy Path 시나리오가 오류 없이 완료
- [ ] OPENAI_API_KEY가 어떤 API 응답에도 미포함
- [ ] 타팀 멤버가 우리 팀 회의 API 호출 시 403/404
- [ ] VoiceSample 10MB 초과 시 400 응답
- [ ] MeetingAudio 100MB 초과 시 400 응답
- [ ] Flyway 마이그레이션이 클린 DB에서 순서대로 적용 성공
- [ ] **[Ground Rule]** 최종 브랜치 push 및 PR 생성
- [ ] **[Ground Rule]** 전체 테스트 스위트 (`./gradlew test`, `npm run test`) 통과
- [ ] **[Ground Rule]** 운영 로그 수준(INFO 이상)에서 민감 정보 미출력 확인

---

## 도메인 모델 의존 관계 요약

```
Team ──< TeamMember >── Member
  │                       │
  ├──< Meeting            └──< VoiceSample (2~10초, 최대 10MB)
  │     │
  │     ├──< MeetingParticipant
  │     ├──< AudioFile (최대 100MB, 최대 60분)
  │     └──< TranscriptionJob (활성 1개 제한)
  │               │
  │               ├──< TranscriptSegment (speaker 값 그대로 저장)
  │               └──< SpeakerMapping
  │
  ├──< IntegrationAccount (JIRA/NOTION, AES-GCM 암호화)
  └──< MinutesGenerationJob
              ↓
        MeetingMinutes (Meeting 1:1)
              │
              ├──< MemberMinutesSummary
              ├──< ActionItem
              │    (decisions: raw_content에만 저장, MVP 별도 테이블 없음)
              └──< AgentRun
                    │
                    ├── PROPOSAL_GENERATION: meeting_minutes_id만
                    └── MCP_EXECUTION: + proposal_id
                          └──< AgentProposal
                                (PENDING_APPROVAL→APPROVED/REJECTED/REVISION_REQUESTED→EXECUTING→EXECUTED/FAILED)
                                └──< McpExecutionLog
```

---

## Phase별 우선순위 및 병렬 진행 가이드

| Phase | 선행 조건 | 예상 작업량 | 병렬 가능 여부 |
|-------|-----------|-------------|----------------|
| 0 | 없음 | 소 | Frontend/Backend 동시 진행 |
| 1 | Phase 0 | 중 | Frontend/Backend 병렬 개발 |
| 2A | Phase 1 | 중 | Backend 파일 저장 / Frontend 업로드 UI 병렬 |
| 2B | Phase 2A | 소 | Frontend 전담 (Backend 변경 없음) |
| 3 | Phase 2A | 대 | Backend 중심 (Frontend는 polling + SpeakerMapping UI) |
| 4 | Phase 3 | 중 | Frontend/Backend 병렬 |
| 5 | Phase 4 | 대 | Backend McpGateway / Frontend 승인 UI 병렬 |
| 6 | Phase 5 | 소 | QA/보안 리뷰 |

---

## 외부 API 연동 시 필수 확인 사항

> **아래 두 가지는 코드 작성 전 반드시 공식 최신 문서를 확인한다. 예시 코드를 그대로 사용하지 않는다.**

### 1. gpt-4o-transcribe-diarize API
- `response_format=diarized_json` 지원 여부 및 응답 스키마
- `chunking_strategy` 파라미터 형식 (30초 초과 필수)
- known speaker reference 전달 방식: 필드명(`known_speaker_names`, `known_speaker_references` 예상), 형식(data URL? 파일?)
- known speaker 최대 인원 제한 (4명 예상 — 재확인 필수)
- voice sample 길이 제한 (2~10초 예상 — 재확인 필수)
- diarized_json 응답의 speaker 필드 형식

### 2. Jira / Notion MCP
- MCP 표준 프로토콜 (tool call 형식, transport 방식: stdio? HTTP?)
- Jira MCP: 이슈 생성 tool 이름, 파라미터 스키마
- Notion MCP: 페이지 생성 tool 이름, 파라미터 스키마
- MCP 서버 인증 방식 (서버 자체 관리 vs 서비스가 token 전달)
- 에러 코드 및 재시도 정책
