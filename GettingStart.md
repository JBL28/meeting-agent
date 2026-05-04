# 프로젝트 개요
- 회의 음성을 STT를 활용해 텍스트로 변환하고, Agentic하게 처리하는 협업 관리 프로젝트

## Main Feature
    - STT를 활용한 회의록 텍스트 변환
        - STT 모델은 gpt-4o-transcribe-diarize를 사용
        - API 호출 코드 작성 시 최신 공식 문서를 확인해야함
        - gpt-4o-transcribe-diarize의 known speaker reference를 활용하기 위해 서비스 단에서 팀원의 목소리를 10초정도 길이의 파일로 등록 가능해야함
        - 웹 서비스 상에서 회의록을 녹음해 파일로 저장 후 모델로 옮기는 방식
    - MCP를 활용한 Agentic
        - jira, notion 등 MCP를 활용해 일정, 티켓 등을 관리
        - MCP를 활용하는 경우 사용자의 승인이 필요하도록 워크플로우 확립
        - API 활용을 위한 LLM API 사용 시 최신 API 공식 문서를 확인해야함
    - 회의록 텍스트 변환 필요
        - 멤버 별로 진행 상황, 주요 이슈, 진행할 작업 등 섹션을 나눠 정리 필요
    - 변환된 회의록 문서 검색 기능
        - MVP에서는 단순 텍스트 매칭 기반 검색 기능을 구현
        - 추후 Elastic Search 혹은 Vector 기반 검색 기능으로 전환 예정
    
## 아키텍쳐
    - 프론트엔드 (Next.js)
        - 화면
        - 녹음 UI
        - 파일 업로드 UI
        - 회의록 편집/검색 UI
        - Agentic 작업 승인 UI
    - 백엔드 (Spring Boot)
        - 인증/인가
        - 회의/팀/멤버 관리
        - 음성 파일 저장
        - STT Job 관리
        - OpenAI Transcription API 호출
        - 회의록 segment 저장
        - 요약/액션아이템 생성
        - MCP 작업 승인 플로우
        - Jira/Notion 연동
        - 검색 API
    - DB (MySQL)

## MVP 범위
- 팀/멤버 관리
- 멤버별 voice sample 등록
- 웹에서 회의 녹음 후 파일 저장
- 음성 파일 업로드
- gpt-4o-transcribe-diarize 기반 STT 변환
- known speaker reference 기반 발화자 식별
- STT 결과 segment 저장
- 멤버별 진행 상황 / 이슈 / 할 일 구조화
- 회의록 조회 / 수정 / 삭제
- 단순 텍스트 매칭 기반 검색
- Agentic 작업 제안 생성
- 사용자의 명시적 승인 후 Jira/Notion MCP 실행

## MVP 제외
- 실시간 STT
- 실시간 자막
- 실시간 스트리밍 전송
- chunk upload
- Elastic Search
- Vector Search
- 자동 Jira/Notion 실행
- 복잡한 권한 체계

## 주요 도메인
- Team
- Member
- Project
- Meeting
- MeetingParticipant
- VoiceSample
- AudioFile
- TranscriptionJob
- TranscriptSegment
- SpeakerMapping
- MeetingMinutes
- ActionItem
- Issue
- Decision
- AgentRun
- AgentProposal
- ApprovalRequest
- IntegrationAccount
- McpExecutionLog

VoiceSample
- 멤버의 목소리 샘플 파일 정보를 저장한다.
- known speaker reference에 사용된다.

TranscriptionJob
- STT 처리 상태를 관리한다.
- CREATED, PROCESSING, COMPLETED, FAILED 상태를 가진다.

TranscriptSegment
- STT 결과의 발화 단위 텍스트를 저장한다.
- speaker label, start time, end time, text를 포함한다.

SpeakerMapping
- STT 결과의 Speaker 1, Speaker 2를 실제 멤버와 연결한다.

AgentProposal
- Jira 티켓 생성, Notion 문서 생성 등 Agentic 작업 제안을 저장한다.
- 사용자 승인 전에는 외부 MCP를 실행하지 않는다.

## TranscriptionJob 상태
- CREATED: STT 작업 생성됨
- UPLOADED: 음성 파일 저장 완료
- PROCESSING: STT API 호출 중
- COMPLETED: STT 완료
- FAILED: STT 실패
- CANCELED: 사용자가 취소

## Meeting 상태
- DRAFT: 회의 생성됨
- RECORDING: 녹음 중
- RECORDED: 녹음 완료
- TRANSCRIBING: STT 처리 중
- TRANSCRIBED: 전사 완료
- MINUTES_GENERATED: 회의록 생성 완료
- ARCHIVED: 보관됨

## AgentProposal 상태
- PROPOSED: Agent가 작업을 제안함
- PENDING_APPROVAL: 사용자 승인 대기
- APPROVED: 승인됨
- REJECTED: 거절됨
- EXECUTING: MCP 실행 중
- EXECUTED: 실행 완료
- FAILED: 실행 실패

## 회의 STT 처리 플로우
1. 사용자가 회의를 생성한다.
2. 회의 참가자를 등록한다.
3. 참가자는 10초 내외의 voice sample을 등록한다.
4. 사용자가 웹에서 회의를 녹음한다.
5. 브라우저는 녹음 완료 후 audio file을 backend에 업로드한다.
6. backend는 audio file을 저장한다.
7. backend는 TranscriptionJob을 생성한다.
8. backend는 gpt-4o-transcribe-diarize API를 호출한다.
9. known speaker reference로 등록된 voice sample을 함께 전달한다.
10. backend는 diarized transcription 결과를 TranscriptSegment로 저장한다.
11. backend는 speaker label과 member를 매핑한다.
12. LLM을 사용해 멤버별 진행 상황, 주요 이슈, 진행할 작업을 구조화한다.
13. 사용자는 회의록을 조회하고 수정할 수 있다.

## Agentic 작업 승인 플로우
1. backend는 회의록을 기반으로 가능한 후속 작업을 생성한다.
   - Jira 티켓 생성
   - Notion 회의록 문서 생성
   - 담당자별 할 일 생성
2. Agent는 외부 서비스를 바로 수정하지 않고 AgentProposal을 생성한다.
3. 사용자는 제안 내용을 확인한다.
4. 사용자가 승인하면 ApprovalRequest가 APPROVED 상태가 된다.
5. 승인된 제안만 MCP를 통해 Jira/Notion에 반영한다.
6. 실행 결과와 실패 사유는 McpExecutionLog에 저장한다.

## MCP 실행 정책
- Agent는 사용자 승인 없이 외부 시스템을 변경할 수 없다.
- Agent는 Jira/Notion에 쓰기 작업을 하기 전 반드시 변경 내용을 요약해 보여준다.
- 사용자는 제안을 승인, 거절, 수정 요청할 수 있다.
- 모든 MCP 실행 요청과 응답은 로그로 저장한다.

## 보안 및 개인정보
- OpenAI API Key는 backend 환경변수로만 관리한다.
- API Key는 frontend에 절대 노출하지 않는다.
- 음성 파일과 voice sample은 인증된 사용자만 접근할 수 있다.
- voice sample 등록 시 사용자 동의를 받는다.
- 사용자는 본인의 voice sample을 삭제할 수 있다.
- STT 원본 응답은 디버깅 목적의 보관 기간을 정한다.
- 회의 음성 파일은 삭제 정책을 가진다.

## 파일 저장 정책
- MVP에서는 local storage 또는 서버 파일 시스템에 저장한다.
- 추후 S3 호환 Object Storage로 전환할 수 있도록 StorageService 인터페이스를 둔다.
- 저장 대상:
  - 회의 녹음 파일
  - 멤버 voice sample 파일
  - STT 원본 응답 JSON
- 파일 메타데이터는 DB에 저장한다.

## 비동기 처리
- 음성 파일 업로드 요청은 즉시 TranscriptionJob ID를 반환한다.
- STT 처리는 backend worker에서 비동기로 수행한다.
- frontend는 job status를 polling하여 진행 상태를 확인한다.
- MVP에서는 Spring @Async 또는 Scheduler 기반 polling을 사용한다.
- 추후 Redis Queue, RabbitMQ로 확장할 수 있다.

## 에러 처리
- STT API 호출 실패 시 TranscriptionJob 상태를 FAILED로 변경한다.
- 실패 사유를 errorMessage에 저장한다.
- 사용자는 FAILED job을 다시 실행할 수 있다.
- 파일 업로드 실패와 STT 처리 실패는 구분한다.
- MCP 실행 실패 시 AgentProposal은 FAILED로 변경하고 실행 로그를 저장한다.

## 검색 기능
- MVP에서는 회의 제목, 회의록 본문, 발화자 이름, 액션 아이템 내용에 대해 LIKE 기반 검색을 제공한다.
- 검색 결과는 회의 단위로 반환한다.
- 검색 결과에는 매칭된 일부 텍스트 snippet을 포함한다.
- 추후 Elastic Search 또는 Vector Search로 전환할 수 있도록 SearchService 인터페이스를 둔다.

## 회의록 구조
- 다음 주제 중 해당하는 섹션에 정리한다.
    - 회의 제목
    - 회의 일시
    - 참가자
    - 전체 요약
    - 멤버별 진행 상황
    - 멤버별 주요 이슈
    - 멤버별 다음 작업
    - 결정사항
    - 액션 아이템
    - 담당자
    - 내용
    - 마감일
    - 상태
    - 원본 발화 로그

## 환경변수
- OPENAI_API_KEY
- OPENAI_TRANSCRIBE_MODEL=gpt-4o-transcribe-diarize
- JIRA_MCP_SERVER_URL
- NOTION_MCP_SERVER_URL
- DB_URL
- DB_USERNAME
- DB_PASSWORD
- STORAGE_BASE_PATH

## 권한
- OWNER: 팀 생성자, 모든 권한
- ADMIN: 멤버 관리, 회의록 수정, MCP 승인 가능
- MEMBER: 회의 생성, 회의록 조회/수정 가능
- VIEWER: 조회만 가능