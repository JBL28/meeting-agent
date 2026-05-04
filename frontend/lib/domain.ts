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

export function canInvite(role: TeamRole) {
  return role === 'OWNER' || role === 'ADMIN';
}
