# Phase 4. 회의록 생성·관리 및 검색

## 구현한 기능 목록

### 공식 문서 확인
- OpenAI Structured Outputs와 Responses API 문서를 확인했다.
- 적용한 핵심 조건
  - Responses API 사용
  - 구조화 출력은 `text.format`의 `type=json_schema`로 요청
  - `strict=true` JSON Schema로 title, full_summary, member_summaries, decisions, action_items 고정
  - JSON mode 대신 Structured Outputs 사용
- 참고 문서
  - `https://platform.openai.com/docs/guides/structured-outputs`
  - `https://platform.openai.com/docs/api-reference/responses`

### Backend
- 회의록 생성 도메인과 Flyway V5 추가
  - `MinutesGenerationJob`: CREATED, PROCESSING, COMPLETED, FAILED
  - `MeetingMinutes`: 회의별 UNIQUE 회의록, raw_content LONGTEXT
  - `ActionItem`: TODO, IN_PROGRESS, DONE
  - `MemberMinutesSummary`: 멤버별 진행 상황, 이슈, 다음 작업
- 회의록 생성 비동기 파이프라인 구현
  - TRANSCRIBED meeting에서만 생성 시작 가능
  - active job CREATED, PROCESSING 존재 시 409 응답
  - 생성 시작 시 202 + jobId 반환
  - worker에서 PROCESSING 전이 후 TranscriptSegment를 LLM 입력으로 구성
  - Structured Outputs JSON 파싱 성공 시 MeetingMinutes, MemberMinutesSummary, ActionItem 저장
  - 성공 시 job COMPLETED, meeting MINUTES_GENERATED 전이
  - 파싱 실패 또는 LLM 실패 시 job FAILED, 빈 회의록 미저장, meeting은 TRANSCRIBED 유지
- OpenAI 연동 경계 추가
  - `MinutesLlmClient` 인터페이스
  - `RestTemplateMinutesLlmClient` 구현
  - 테스트는 LLM client mock 사용
- 회의록 관리 API 추가
  - `POST /api/meetings/{meetingId}/minutes/generate`
  - `GET /api/minutes-generation-jobs/{jobId}`
  - `GET /api/meetings/{meetingId}/minutes`
  - `PUT /api/meetings/{meetingId}/minutes`
  - `DELETE /api/meetings/{meetingId}/minutes`
  - `GET /api/meetings/{meetingId}/minutes/segments`
  - `PUT /api/action-items/{actionItemId}`
  - `PUT /api/action-items/{actionItemId}/status`
- 검색 기능 추가
  - `SearchService` 인터페이스
  - `JpaSearchService` MVP 구현
  - meeting title, minutes full_summary, transcript segment text, action item content 검색
  - 결과는 meeting 단위 dedupe 및 snippet 포함
  - `GET /api/teams/{teamId}/meetings/search?keyword=...`

### Frontend
- 회의 상세에서 회의록 페이지 링크 추가
- `/meetings/{meetingId}/minutes`
  - 회의록 생성 시작
  - MinutesGenerationJob 2초 polling
  - 완료 후 회의록 조회
  - summary 편집
  - member summaries, decisions, action items 표시
  - action item status 변경
  - 원본 transcript 표시
- `/teams/{teamId}/search`
  - 키워드 검색 UI
  - snippet 포함 결과 카드 표시
- 팀 목록에서 검색 페이지 링크 추가

## 계획 대비 달라진 점 및 이유

- 별도 OpenAI SDK 의존성은 추가하지 않았다.
  - 이유: 현재 프로젝트 의존성 추가를 최소화하고, LLM 호출 경계를 mock 하기 쉽게 하기 위함이다.
- 검색은 JPA 기반 repository query와 service-level dedupe를 조합했다.
  - 이유: MVP 범위에서는 ES/Vector 도입 전 교체 가능한 `SearchService` 인터페이스가 중요하며, 데이터 규모가 작다는 전제에서 충분하다.
- decisions는 계획대로 별도 테이블 없이 `raw_content`에서 파싱해 응답한다.

## 검증 결과

- Backend tests: `./gradlew.bat test` 통과
- Frontend lint: `npm run lint` 통과
- Frontend tests: `npm test` 통과
- Frontend build: `npm run build` 통과
- Frontend audit: `npm audit --json` 취약점 0
- Docker MySQL boot smoke: Flyway V5 포함 서버 기동 확인
- `git diff --check` 통과

### 주요 테스트
- `MinutesGenerationServiceTest`
  - LLM mock 응답 파싱 성공 시 MeetingMinutes, MemberMinutesSummary, ActionItem 생성 검증
  - 성공 시 job COMPLETED 및 meeting MINUTES_GENERATED 전이 검증
  - 파싱 실패 시 job FAILED, 빈 회의록 미저장, meeting TRANSCRIBED 유지 검증
- `JpaSearchServiceTest`
  - 키워드 검색 결과 snippet 포함 검증
  - 요청 팀의 회의만 반환되는지 검증
- `PhaseFourApiIntegrationTest`
  - TRANSCRIBED가 아닌 meeting에서 generate 시 400 응답 검증
  - VIEWER가 회의록 수정 API 호출 시 403 응답 검증

## 알려진 제한사항 또는 TODO

- 실제 OpenAI API 호출은 `OPENAI_API_KEY`가 없어 mock 테스트로 검증했다.
- action item assignee 매칭은 LLM 응답의 assignee 문자열과 회의 참가자의 name/email/id 정확 일치를 우선한다.
- raw_content 보관/정리 정책은 아직 자동화하지 않았다.
- 검색은 JPA LIKE 기반 MVP 구현이며, 대용량 데이터에서는 ES 또는 Vector 검색으로 교체해야 한다.

## 다음 Phase 진입 전 확인이 필요한 사항

- 실제 OpenAI minutes generation smoke test가 필요하다.
- Phase 5 외부 협업 연동은 ActionItem과 MeetingMinutes를 입력으로 사용할 수 있다.
- 운영 전에는 `OPENAI_API_KEY`, `openai.minutes-model`, raw_content 개인정보 보관 정책을 확인해야 한다.