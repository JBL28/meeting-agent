# Phase 2A 구현 기록

## 구현한 기능 목록

### Backend

- 도메인 및 마이그레이션
  - `VoiceSample`, `Meeting`, `MeetingParticipant`, `AudioFile` JPA 엔티티 추가
  - `MeetingStatus` enum 추가: `DRAFT`, `RECORDING`, `RECORDED`, `TRANSCRIBING`, `TRANSCRIBED`, `MINUTES_GENERATED`, `ARCHIVED`
  - Flyway `V3__meeting_audio_voice.sql` 추가
- 파일 저장
  - `StorageService` 인터페이스 추가
  - `LocalStorageService` 구현: `${STORAGE_BASE_PATH}/{category}/{yyyy}/{MM}/{uuid}.{ext}` 저장
  - `AudioDurationProbe` 인터페이스와 `FfprobeAudioDurationProbe` 구현
  - `ffprobe` 미설치 시 WARN 로그 후 duration 검증 생략
- 파일 정책 검증
  - Voice sample: `wav`, `mp3`, `webm`, 최대 10MB
  - Meeting audio: `wav`, `mp3`, `webm`, `m4a`, 최대 100MB
  - ffprobe 사용 가능 시 voice sample 2~10초, meeting audio 60분 이하 검증
- Voice sample API
  - `POST /api/teams/{teamId}/members/{memberId}/voice-samples`
  - `GET /api/teams/{teamId}/members/{memberId}/voice-samples`
  - `DELETE /api/voice-samples/{sampleId}`
  - 동의(`consent=true`) 필수 검증
  - 본인 또는 ADMIN만 업로드 가능
  - 삭제는 샘플 소유자 본인만 가능
- Meeting / Audio API
  - `POST /api/teams/{teamId}/meetings`
  - `GET /api/teams/{teamId}/meetings`
  - `GET /api/meetings/{meetingId}`
  - `POST /api/meetings/{meetingId}/participants`
  - `POST /api/meetings/{meetingId}/audio`
  - `GET /api/audio/{audioFileId}/stream`
  - 오디오 업로드 성공 시 `AudioFile` 저장 및 `Meeting.status=RECORDED` 변경
  - 스트리밍 URL은 인증 및 팀 VIEWER 이상 권한 필요

### Frontend

- Phase 1에서 남아 있던 깨진 UI 문자열을 제거했다.
- 인코딩 재발 방지를 위해 사용자 화면 문구는 ASCII 영문으로 정리했다.
- `/teams/{teamId}/profile`
  - voice sample 파일 업로드
  - 동의 체크박스
  - 등록된 샘플 목록 표시
- `/meetings/new`
  - 팀 ID, 회의 제목, 예정 일시 입력으로 회의 생성
- `/meetings/{meetingId}`
  - 회의 상세
  - 참가자 목록
  - 참가자 추가
  - 파일 선택 기반 meeting audio 업로드
- `lib/upload-api.ts` 및 Phase 2A 타입 추가
- `apiFetch`가 `FormData` 업로드 시 `Content-Type`을 직접 지정하지 않도록 수정

## 계획 대비 달라진 점 및 이유

- 계획서의 `StreamingResponseBody` 대신 `Resource` 기반 `ResponseEntity<Resource>`로 스트리밍 응답을 구현했다. 인증/인가 보호와 파일 응답 요구사항을 만족하며 MVP 구현이 더 단순하다.
- `GET /api/teams/{teamId}/members/{memberId}/voice-samples` 권한은 계획서의 MEMBER 이상을 그대로 적용했다.
- Voice sample 업로드는 본인 또는 ADMIN으로 구현했다. 계획서 권한 설명의 "본인 또는 ADMIN"을 따른다.
- Frontend의 `/meetings/new`는 아직 팀 선택 UI가 없으므로 팀 ID를 직접 입력하게 했다. 팀 목록 기반 선택 UI는 후속 UX 개선으로 남겼다.
- ffprobe가 로컬에 설치되어 있지 않은 환경에서는 계획서대로 WARN 로그를 남기고 duration 검증을 생략한다.

## 검증 결과

- Backend
  - `backend/.\gradlew.bat clean test` 성공
  - 추가 테스트:
    - `VoiceSampleServiceTest`: 동의 누락, 10MB 초과 검증
    - `AudioFileServiceTest`: 100MB 초과, 업로드 성공 시 RECORDED 상태 전환 검증
    - `PhaseTwoAApiIntegrationTest`: voice sample 동의 검증, 타인 삭제 403, audio 업로드, RECORDED 상태, 비인증 스트리밍 401
- Frontend
  - `frontend/npm run lint` 성공
  - `frontend/npm test` 성공
  - `frontend/npm run build` 성공
  - `frontend/npm audit --json` 취약점 0개
- Runtime smoke test
  - Docker MySQL `3307 -> 3306` 대상 `backend/.\gradlew.bat bootRun` 기동
  - Flyway V3 마이그레이션 적용 확인
  - 동의 없는 voice sample 업로드 400 확인
  - voice sample 업로드 성공 확인
  - 타 멤버의 voice sample 삭제 403 확인
  - meeting 생성 및 meeting audio 업로드 성공 확인
  - audio 업로드 후 meeting status `RECORDED` 확인
  - 인증 없이 `/api/audio/{audioFileId}/stream` 접근 시 401 확인
  - ffprobe 미설치에 따른 WARN 로그 확인

## 알려진 제한사항 또는 TODO

- 로컬 환경에 ffprobe가 없어 duration 검증은 WARN 로그 후 생략되었다. 운영 환경에서는 ffprobe 설치를 필수로 해야 한다.
- Frontend meeting 생성은 팀 ID 직접 입력 방식이다. 팀 목록 선택 UI는 추후 개선한다.
- Voice sample 삭제 UI는 아직 없다. Backend API와 권한 검증은 구현되어 있다.
- 파일 저장은 local filesystem 구현만 제공한다. S3 호환 저장소 전환은 `StorageService` 구현체 교체로 진행한다.
- known speaker reference 최대 4명 정책은 Phase 3에서 STT API 연동 시 공식 문서 확인 후 적용한다.

## 다음 Phase 진입 전 확인 사항

- Phase 2B에서는 MediaRecorder 기반 녹음 UI만 추가하고, 업로드 API는 이번 Phase의 meeting audio API를 재사용한다.
- Phase 3에서는 `MeetingParticipant`와 `VoiceSample`을 기준으로 known speaker reference 후보를 구성한다.