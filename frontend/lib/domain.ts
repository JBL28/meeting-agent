export type TeamRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';

export type Member = {
  id: number;
  email: string;
  name: string;
};

export type AuthResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
  member: Member;
};

export type Team = {
  id: number;
  name: string;
  ownerId: number;
  myRole: TeamRole;
};

export type TeamMember = {
  memberId: number;
  email: string;
  name: string;
  role: TeamRole;
};

export type VoiceSample = {
  id: number;
  memberId: number;
  teamId: number;
  fileName: string;
  durationSeconds: number | null;
  createdAt: string | null;
  consentAgreedAt: string;
};

export type MeetingStatus = 'DRAFT' | 'RECORDING' | 'RECORDED' | 'TRANSCRIBING' | 'TRANSCRIBED' | 'MINUTES_GENERATED' | 'ARCHIVED';

export type MeetingParticipant = {
  memberId: number;
  email: string;
  name: string;
};

export type Meeting = {
  id: number;
  teamId: number;
  title: string;
  scheduledAt: string | null;
  status: MeetingStatus;
  createdBy: number;
  createdAt: string | null;
  participants: MeetingParticipant[];
};

export type AudioFile = {
  id: number;
  meetingId: number;
  fileName: string;
  fileSize: number;
  durationSeconds: number | null;
  uploadedAt: string | null;
};

export type TranscriptionJobStatus = 'CREATED' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELED';

export type TranscriptionJob = {
  id: number;
  meetingId: number;
  audioFileId: number;
  status: TranscriptionJobStatus;
  errorMessage: string | null;
  rawResponsePath: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string | null;
};

export type TranscriptionJobStart = {
  jobId: number;
};

export type TranscriptSegment = {
  id: number;
  speaker: string;
  memberId: number | null;
  startTime: number;
  endTime: number;
  text: string;
  sequence: number;
};

export type SpeakerMapping = {
  id: number;
  speaker: string;
  memberId: number;
  memberName: string;
  autoMapped: boolean;
};

export type MinutesGenerationJobStatus = 'CREATED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export type MinutesGenerationJob = {
  id: number;
  meetingId: number;
  status: MinutesGenerationJobStatus;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
  createdAt: string | null;
};

export type MinutesGenerationJobStart = {
  jobId: number;
};

export type ActionItemStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export type MinutesActionItem = {
  id: number;
  assigneeId: number | null;
  assigneeName: string | null;
  content: string;
  dueDate: string | null;
  status: ActionItemStatus;
};

export type MemberMinutesSummary = {
  id: number;
  memberId: number;
  memberName: string;
  progress: string;
  issues: string;
  nextTasks: string;
};

export type MeetingMinutes = {
  id: number;
  meetingId: number;
  generationJobId: number;
  title: string;
  meetingDate: string | null;
  fullSummary: string;
  decisions: string[];
  memberSummaries: MemberMinutesSummary[];
  actionItems: MinutesActionItem[];
  createdAt: string | null;
  updatedAt: string | null;
};

export type MeetingSearchResult = {
  meetingId: number;
  title: string;
  status: MeetingStatus;
  snippet: string;
  createdAt: string | null;
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export function canInvite(role: TeamRole) {
  return role === 'OWNER' || role === 'ADMIN';
}
