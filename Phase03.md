# Phase 3. STT 처리 파이프라인

## 구현한 기능 목록

### 공식 문서 확인
- OpenAI 공식 Speech to text 가이드와 Audio Transcriptions API Reference를 확인했다.
- 적용한 핵심 조건
  - 모델: `gpt-4o-transcribe-diarize`
  - 응답 형식: `response_format=diarized_json`
  - 긴 회의 음성 대응: `chunking_strategy=auto`
  - 화자 참고 샘플: `known_speaker_names[]`, `known_speaker_references[]` data URL 형식
- 참고 문서
  - `https://platform.openai.com/docs/guides/speech-to-text`
  - `https://platform.openai.com/docs/api-reference/audio/createTranscription`
  - `https://platform.openai.com/docs/models/gpt-4o-transcribe-diarize`

### Backend
- STT 도메인과 DB 마이그레이션 추가
  - `TranscriptionJob`: CREATED, PROCESSING, COMPLETED, FAILED, CANCELED 상태 관리
  - `TranscriptSegment`: speaker, startTime, endTime, text, sequence 저장
  - `SpeakerMapping`: speaker label과 팀 멤버 매핑 저장
  - Flyway `V4__transcription_pipeline.sql`
- 비동기 처리 구성 추가
  - `@EnableAsync`
  - `transcriptionExecutor` ThreadPoolTaskExecutor
  - `TranscriptionAsyncService`
- OpenAI 연동 경계 추가
  - `OpenAiTranscriptionClient` 인터페이스
  - `RestTemplateOpenAiTranscriptionClient` 구현
  - 테스트에서는 OpenAI 클라이언트를 mock 처리
- STT 처리 흐름 구현
  - RECORDED meeting에서만 STT 시작 가능
  - active job CREATED, PROCESSING 존재 시 409 응답
  - 시작 시 job CREATED 생성 후 202 Accepted 반환
  - worker에서 PROCESSING 및 meeting TRANSCRIBING 전이
  - 성공 시 raw JSON 저장, segment 저장, job COMPLETED, meeting TRANSCRIBED 전이
  - 실패 시 error JSON 저장, job FAILED, meeting RECORDED 전이
- known speaker reference 정책 구현
  - 참가자 4명 이하인 경우 최신 voice sample을 data URL로 변환해 OpenAI 요청에 포함
  - 참가자 5명 이상이면 reference 없이 diarization만 수행하고 WARN 로그 출력
- API 추가
  - `POST /api/meetings/{meetingId}/transcription`
  - `GET /api/transcription-jobs/{jobId}`
  - `GET /api/transcription-jobs/{jobId}/segments`
  - `POST /api/transcription-jobs/{jobId}/cancel`
  - `POST /api/transcription-jobs/{jobId}/retry`
  - `POST /api/transcription-jobs/{jobId}/speaker-mapping`
- 409 처리를 위한 `ConflictException` 추가

### Frontend
- 회의 상세 화면에 STT 시작, retry, polling UI 추가
- job 상태를 2초 간격으로 polling
- COMPLETED 또는 FAILED 도달 시 polling 자동 종료
- COMPLETED 시 transcript segment 목록 표시
- speaker별 참가자 선택 후 mapping 저장 UI 추가

## 계획 대비 달라진 점 및 이유

- Java 공식 OpenAI SDK 의존성은 추가하지 않았다.
  - 이유: 현재 프로젝트 의존성을 최소화하고, Phase 3 테스트에서 OpenAI 경계를 mock 하기 쉽도록 인터페이스와 RestTemplate 구현으로 분리했다.
- `chunking_strategy=auto`는 기본 요청에 항상 포함한다.
  - 이유: 회의 음성은 30초 초과 가능성이 높고, 공식 문서에서 diarize 모델의 긴 입력에 필요하다고 안내한다.
- async worker 내부에서 상태 전이 트랜잭션과 외부 API 호출을 분리했다.
  - 이유: polling 중 PROCESSING 상태가 DB에 보이도록 하기 위함이다.

## 검증 결과

- Backend tests: `./gradlew.bat test` 통과
- Frontend lint: `npm run lint` 통과
- Frontend tests: `npm test` 통과
- Frontend build: `npm run build` 통과
- Frontend audit: `npm audit --json` 취약점 0

### 주요 테스트
- `TranscriptionAsyncServiceTest`
  - OpenAI client mock 응답으로 segment 저장 검증
  - 성공 시 job COMPLETED 및 meeting TRANSCRIBED 전이 검증
  - OpenAI 실패 시 error JSON 저장, job FAILED 및 meeting RECORDED 전이 검증
  - 참가자 5명 이상일 때 known speaker reference 없이 요청하는지 검증
- `PhaseThreeApiIntegrationTest`
  - RECORDED 상태가 아닌 meeting에서 STT 시작 시 400 응답
  - active TranscriptionJob 존재 시 재시작 409 응답
  - PROCESSING 중 retry 409 응답

## 알려진 제한사항 또는 TODO

- 실제 OpenAI API 호출은 로컬 테스트에서 수행하지 않았다. `OPENAI_API_KEY` 설정 후 별도 smoke test가 필요하다.
- raw JSON 보관 기간 30일 정책은 문서화만 했고 자동 삭제 작업은 구현하지 않았다.
- async cancel은 DB 상태를 CANCELED로 바꾸지만 이미 시작된 외부 OpenAI HTTP 요청 자체를 강제 중단하지는 않는다.
- speaker auto mapping은 OpenAI 응답 speaker label과 known speaker name이 일치하는 경우까지 확장 가능하지만, 현재는 segment 저장과 수동 mapping UI를 우선 구현했다.

## 다음 Phase 진입 전 확인이 필요한 사항

- 실제 webm 또는 m4a 회의 파일로 OpenAI smoke test를 수행해야 한다.
- Phase 4 회의록 생성은 `TranscriptSegment`와 `SpeakerMapping`을 입력으로 사용하면 된다.
- 운영 환경에서는 `OPENAI_API_KEY`, `STORAGE_BASE_PATH`, raw 응답 파일 접근 권한을 확인해야 한다.