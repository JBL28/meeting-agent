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

export function canInvite(role: TeamRole) {
  return role === 'OWNER' || role === 'ADMIN';
}
