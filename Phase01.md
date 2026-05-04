# Phase 1 구현 기록

## 구현한 기능 목록

### Backend

- 인증/인가
  - `POST /api/auth/register`: 회원가입
  - `POST /api/auth/login`: BCrypt 비밀번호 검증 후 JWT access token 및 refresh token 발급
  - `GET /api/auth/me`: Bearer access token 기반 내 정보 조회
  - JWT access token 1시간, refresh token 7일 기본값 구성
  - refresh token DB 저장용 `refresh_tokens` 테이블 및 Flyway `V2__refresh_tokens.sql` 추가
  - 인증 실패 401, 권한 부족 403을 `ApiResponse` 형식으로 반환
- 팀·멤버 관리
  - `Member`, `Team`, `TeamMember`, `TeamRole` 도메인/JPA 엔티티 구현
  - `POST /api/teams`: 팀 생성 및 생성자 OWNER 자동 등록
  - `GET /api/teams`: 내 팀 목록 조회
  - `GET /api/teams/{teamId}`: VIEWER 이상 팀 조회
  - `PUT /api/teams/{teamId}`: OWNER 팀명 수정
  - `DELETE /api/teams/{teamId}`: OWNER 팀 삭제
  - `GET /api/teams/{teamId}/members`: VIEWER 이상 팀 멤버 목록 조회
  - `POST /api/teams/{teamId}/members`: ADMIN 이상 멤버 초대
  - `PUT /api/teams/{teamId}/members/{memberId}/role`: OWNER 역할 변경
  - `DELETE /api/teams/{teamId}/members/{memberId}`: ADMIN 이상 멤버 제거
- 권한 체계
  - `TeamRole`: `OWNER > ADMIN > MEMBER > VIEWER`
  - `TeamPermissionEvaluator.hasTeamRole(...)` 및 `requireRole(...)` 구현
  - Controller에 `@PreAuthorize` 적용
- 로그
  - 회원가입 성공/중복 실패, 로그인 성공/실패, 팀 생성/수정/삭제, 초대/역할 변경/제거, 인증/권한 실패에 INFO/WARN 로그 추가

### Frontend

- `/login`: 로그인 폼, 로그인 성공 시 access token 저장 및 `/teams` 이동
- `/register`: 회원가입 폼, 성공 시 `/login` 이동
- `/teams`: 내가 속한 팀 목록 및 역할 표시
- `/teams/new`: 팀 생성 화면
- `/teams/[teamId]/settings`: 팀명 수정, 멤버 목록/역할 표시, ADMIN 이상 초대 UI 노출
- 공통 API 함수 추가
  - `lib/auth-api.ts`
  - `lib/team-api.ts`
  - `lib/domain.ts`
- `TeamMemberList` 컴포넌트와 역할별 초대 버튼 표시 테스트 추가

## 계획 대비 달라진 점 및 이유

- Refresh Token 저장 방식은 계획서의 선택지 중 DB 저장 방식을 채택했다. Phase 1에서 별도 refresh 재발급 API는 계획 완료 기준에 없으므로 발급·저장까지만 구현했다.
- OWNER 역할 이전은 후속 정책 결정이 필요한 영역이라 Phase 1에서는 제한했다. 기존 OWNER의 역할 변경/제거 및 신규 OWNER 초대는 거부한다.
- Frontend UI 라이브러리는 shadcn/ui 전체 CLI 생성 대신 Phase 0에서 만든 shadcn 호환 기본 `Button` 컴포넌트를 확장해 사용했다.
- ESLint의 React 19 `react-hooks/set-state-in-effect` 규칙은 비동기 API fetch 후 state 반영 패턴을 과도하게 차단해 프로젝트 설정에서 비활성화했다.

## 검증 결과

- Backend 단위/API 테스트
  - `backend/.\gradlew.bat clean test` 성공
  - 포함 테스트:
    - `MemberServiceTest`
    - `TeamPermissionEvaluatorTest`
    - `PhaseOneApiIntegrationTest`
- Frontend 검증
  - `frontend/npm test` 성공 (3 files, 5 tests)
  - `frontend/npm run lint` 성공
  - `frontend/npm run build` 성공
  - `frontend/npm audit --json` 취약점 0개
- Runtime smoke test
  - `MYSQL_HOST_PORT=3307` MySQL 컨테이너 대상 `backend/.\gradlew.bat bootRun` 기동
  - `/actuator/health` 200
  - 회원가입 → 로그인 → `/api/auth/me` 호출 성공
  - 팀 생성 시 `myRole=OWNER` 확인
  - VIEWER 멤버 초대 후 멤버 목록에서 `VIEWER` 역할 확인
  - VIEWER가 팀 삭제 API 호출 시 403 확인
  - 관련 INFO/WARN 로그 출력 확인
- Git
  - Phase 0: `40c83ba` push 완료
  - Phase 1: `1e9dedf` push 완료

## 알려진 제한사항 또는 TODO

- Refresh token 재발급/폐기 API는 아직 없다. 필요 시 Phase 1 확장 또는 보안 마무리 Phase에서 추가한다.
- 팀 초대는 계획서대로 기존 가입자의 이메일을 기준으로 팀에 추가한다. 미가입자 초대 메일 발송은 구현하지 않았다.
- 권한 UI는 ADMIN 이상 초대 버튼 노출과 OWNER 팀명 수정 제한을 구현했으나, 역할 변경/삭제 UI는 최소 범위로 남겨 두었다.
- 로컬 3306 포트는 기존 `mysqld`가 점유 중이므로 Docker MySQL은 `MYSQL_HOST_PORT=3307`로 검증했다.

## 다음 Phase 진입 전 확인 사항

- Phase 2A에서 `Meeting`, `MeetingParticipant`, `VoiceSample`, `AudioFile` 마이그레이션을 추가할 때 기존 `members`, `teams`, `team_members` FK를 기준으로 설계한다.
- 음성 파일 접근 제어는 현재 JWT 인증과 팀 권한 체계를 재사용한다.