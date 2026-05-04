# Phase 2B. 웹 녹음 UI

## 구현한 기능 목록

### Frontend
- 공통 녹음 훅 `useAudioRecorder` 추가
  - `MediaRecorder` 기반 webm Blob 생성
  - 녹음 시작/중지/자동 중지 상태 관리
  - `mm:ss` 타이머 표시용 `formatDuration` 제공
  - 업로드 콜백 재사용 구조
  - 녹음 시작, 중지, 업로드 완료 시 `console.info` 로그 출력
- 회의 녹음 페이지 추가: `/meetings/{meetingId}/record`
  - 마이크 권한 요청
  - 녹음 시작 시 meeting status를 `RECORDING`으로 변경
  - 녹음 중 타이머 표시
  - 60분 도달 시 자동 중지 후 기존 meeting audio 업로드 API 사용
  - 업로드 성공 시 기존 API 흐름에 따라 meeting status가 `RECORDED`로 변경
- 회의 상세 페이지에서 브라우저 녹음 페이지로 이동하는 링크 추가
- Voice Sample 녹음 UI 추가: `/teams/{teamId}/profile`
  - 2초 미만 녹음은 업로드하지 않고 안내 메시지 표시
  - 10초 도달 시 자동 중지
  - 동의 체크 후 기존 voice sample 업로드 API 재사용

### Backend
- Phase 2B 계획은 원칙적으로 백엔드 변경 없음이지만, 완료 기준의 `RECORDING -> RECORDED` 실제 DB 상태 전이를 만족하기 위해 최소 API를 추가했다.
- `POST /api/meetings/{meetingId}/recording`
  - MEMBER 이상 권한 필요
  - meeting status를 `RECORDING`으로 변경
  - 기존 `POST /api/meetings/{meetingId}/audio` 업로드 성공 시 `RECORDED`로 변경되는 Phase 2A 흐름을 그대로 사용

## 계획 대비 달라진 점 및 이유

- 변경점: 백엔드에 recording 상태 전이 API 1개를 추가했다.
- 이유: 기존 Phase 2A API만으로는 프론트에서 녹음 시작 시점의 meeting status를 DB에 `RECORDING`으로 저장할 방법이 없었다. 완료 기준이 `RECORDING -> RECORDED` 순차 변경 확인을 요구하므로, 프론트 전용 구현만으로는 기준을 충족할 수 없었다.
- 유지한 제약: 오디오 저장, 업로드, 스트리밍, duration 검증은 Phase 2A API와 서비스를 재사용했다. chunk upload, 실시간 STT, streaming 녹음은 구현하지 않았다.

## 검증 결과

- Frontend lint: `npm run lint` 통과
- Frontend tests: `npm test` 통과
  - MediaRecorder mock 기반 녹음 훅 Vitest 테스트 추가
  - 시작/중지/업로드, 권한 거부, 최대 시간 자동 중지, 최소 시간 미달 업로드 차단 검증
- Frontend build: `npm run build` 통과
  - `/meetings/[meetingId]/record` 동적 라우트 생성 확인
- Backend tests: `./gradlew.bat test` 통과
  - 통합 테스트에서 `RECORDING -> RECORDED` 상태 전이 확인 추가

## 알려진 제한사항 또는 TODO

- 실제 브라우저 마이크 권한 및 녹음 업로드는 로컬 수동 브라우저 테스트가 추가로 필요하다.
- `audio/webm;codecs=opus`는 Chrome 기준이다. 일부 브라우저는 mime type 지원이 다를 수 있으므로 Phase 6 QA에서 호환성 점검이 필요하다.
- ffprobe가 로컬에 없으면 Phase 2A 정책대로 duration 검증은 WARN fallback으로 동작한다.

## 다음 Phase 진입 전 확인이 필요한 사항

- Phase 3 STT 시작 전, 업로드된 AudioFile의 저장 경로와 인증 스트리밍 경로를 그대로 재사용한다.
- OpenAI STT 연동 전에는 반드시 공식 Audio Transcriptions API 문서를 확인해야 한다.
- 실제 녹음 파일로 meeting audio 업로드 후 AudioFile DB 저장과 `RECORDED` 상태를 수동 smoke test로 한 번 더 확인하는 것을 권장한다.