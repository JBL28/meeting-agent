import { apiFetch } from './api';
import type { AudioFile, Meeting, MeetingParticipant, VoiceSample } from './domain';

export function uploadVoiceSample(teamId: string | number, memberId: string | number, input: { file: File; consent: boolean }) {
  const formData = new FormData();
  formData.set('file', input.file);
  formData.set('consent', String(input.consent));
  return apiFetch<VoiceSample>(`/api/teams/${teamId}/members/${memberId}/voice-samples`, {
    method: 'POST',
    body: formData,
  });
}

export function listVoiceSamples(teamId: string | number, memberId: string | number) {
  return apiFetch<VoiceSample[]>(`/api/teams/${teamId}/members/${memberId}/voice-samples`);
}

export function createMeeting(teamId: string | number, input: { title: string; scheduledAt?: string }) {
  return apiFetch<Meeting>(`/api/teams/${teamId}/meetings`, {
    method: 'POST',
    body: JSON.stringify(input),
  });
}

export function listMeetings(teamId: string | number) {
  return apiFetch<Meeting[]>(`/api/teams/${teamId}/meetings`);
}

export function getMeeting(meetingId: string | number) {
  return apiFetch<Meeting>(`/api/meetings/${meetingId}`);
}

export function addMeetingParticipant(meetingId: string | number, memberId: number) {
  return apiFetch<MeetingParticipant>(`/api/meetings/${meetingId}/participants`, {
    method: 'POST',
    body: JSON.stringify({ memberId }),
  });
}

export function uploadMeetingAudio(meetingId: string | number, file: File) {
  const formData = new FormData();
  formData.set('file', file);
  return apiFetch<AudioFile>(`/api/meetings/${meetingId}/audio`, {
    method: 'POST',
    body: formData,
  });
}
