# Phase 0 구현 기록

## 구현한 기능 목록

- 레포지토리 골격 생성
  - `backend/`: Spring Boot 백엔드
  - `frontend/`: Next.js 프론트엔드
  - `docker-compose.yml`: 로컬 MySQL 8.0 개발 환경
- Backend 초기 설정
  - Spring Boot Web, Data JPA, Security, Validation, Actuator, Flyway, MySQL, JJWT, Lombok 구성
  - `ApiResponse<T>` / `ErrorResponse` 공통 응답 포맷 추가
  - `GlobalExceptionHandler` 및 공통 예외 4종 추가
  - 개발 CORS(`http://localhost:3000`) 허용
  - `/actuator/health` 공개 설정
  - Flyway `V1__init.sql`로 `members`, `teams`, `team_members` 생성
  - 애플리케이션 시작/종료 INFO 로그 리스너 추가
  - Phase 0 검증 테스트 추가: DB 연결, actuator health, ApiResponse
- Frontend 초기 설정
  - Next.js App Router 기반 앱 골격 생성
  - Tailwind CSS + shadcn/ui 호환 설정(`components.json`, 기본 `Button`) 추가
  - `NEXT_PUBLIC_API_BASE_URL` 기반 `lib/api.ts` 공통 fetch 래퍼 추가
  - Zustand 인증 상태 스토어 추가
  - Vitest + Testing Library 기반 테스트 추가
  - ESLint flat config 추가
- 개발 환경
  - `.env.example`, `frontend/.env.example` 추가
  - `.gitignore` 추가

## 계획 대비 달라진 점 및 이유

- 로컬 Java가 1.8만 설치되어 있어, 현재 환경에서 바로 `gradlew bootRun` 검증이 가능한 Spring Boot `2.7.18` / Gradle `7.6.4` 조합으로 구성했다.
- 계획서의 `application.yml` 중 외부 연동 URL/키는 값이 없으면 기동 실패할 수 있어 기본값을 빈 문자열로 두었다.
- `team_members.role`은 이후 Phase 1에서 enum 도메인으로 매핑하되, H2 기반 검증 테스트와 MySQL 호환성을 위해 DB 컬럼은 `VARCHAR(20)`로 생성했다.
- 로컬 PC의 `mysqld` 프로세스가 호스트 3306 포트를 이미 점유하고 있어, `docker-compose.yml`은 기본값 3306을 유지하면서 `MYSQL_HOST_PORT`로 호스트 포트를 바꿀 수 있게 했다. 검증은 `MYSQL_HOST_PORT=3307`로 수행했다.
- `npm audit`에서 Next.js 내부 PostCSS 취약점 경고가 발생해 `postcss` override를 `8.5.13`으로 고정했다.

## 검증 결과

- Docker Desktop 확인: `docker info` 정상 응답
- MySQL 컨테이너 확인: `MYSQL_HOST_PORT=3307 docker compose up -d mysql` 후 `meeting-agent-mysql` healthy
- Backend 테스트: `backend/.\gradlew.bat clean test` 성공
- Backend 기동 확인: `SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3307/meetingdb?... backend/.\gradlew.bat bootRun` 후 `GET /actuator/health` 200, Flyway 마이그레이션 및 startup INFO 로그 확인
- Frontend 테스트: `frontend/npm test` 성공 (2 files, 3 tests)
- Frontend lint: `frontend/npm run lint` 성공
- Frontend build: `frontend/npm run build` 성공
- Frontend dev 서버: `frontend/npm run dev` 후 `http://localhost:3000` 200 확인
- Frontend audit: `frontend/npm audit --json` 결과 취약점 0개

## 알려진 제한사항 또는 TODO

- 현재 디렉터리는 Git 저장소가 아니므로 Phase Ground Rule의 브랜치 push는 수행하지 못했다.
- 로컬 3306 포트가 이미 `mysqld`에 의해 사용 중이다. Docker MySQL을 기본 3306으로 띄우려면 기존 MySQL을 중지하거나 `MYSQL_HOST_PORT=3307`처럼 대체 포트를 사용해야 한다.
- Phase 1에서 Spring Security는 JWT 기반 인증/인가로 강화해야 한다. 현재는 Phase 0 health 및 골격 검증을 위해 요청을 permit-all 처리했다.

## 다음 Phase 진입 전 확인 사항

- Git 원격 저장소/브랜치가 준비되면 Phase 0 변경분을 커밋 및 push한다.
- Phase 1 시작 시 `members`, `teams`, `team_members` JPA 엔티티/Repository/Service를 현재 마이그레이션에 맞춰 구현한다.
- 3307 포트를 계속 사용할 경우 Backend 실행 시 `SPRING_DATASOURCE_URL`도 3307로 맞춘다.