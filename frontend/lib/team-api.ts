import { apiFetch } from './api';
import type { Team, TeamMember, TeamRole } from './domain';

export function listTeams() {
  return apiFetch<Team[]>('/api/teams');
}

export function createTeam(input: { name: string }) {
  return apiFetch<Team>('/api/teams', {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function getTeam(teamId: string | number) {
  return apiFetch<Team>(`/api/teams/${teamId}`);
}

export function updateTeam(teamId: string | number, input: { name: string }) {
  return apiFetch<Team>(`/api/teams/${teamId}`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

export function listTeamMembers(teamId: string | number) {
  return apiFetch<TeamMember[]>(`/api/teams/${teamId}/members`);
}

export function inviteTeamMember(teamId: string | number, input: { email: string; role: TeamRole }) {
  return apiFetch<TeamMember>(`/api/teams/${teamId}/members`, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}
