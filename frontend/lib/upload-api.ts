import { apiFetch } from './api';
import type { ActionItemStatus, AudioFile, Meeting, MeetingMinutes, MeetingParticipant, MeetingSearchResult, MinutesActionItem, MinutesGenerationJob, MinutesGenerationJobStart, Page, SpeakerMapping, TranscriptSegment, TranscriptionJob, TranscriptionJobStart, VoiceSample } from './domain';

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

export function markMeetingRecording(meetingId: string | number) {
  return apiFetch<Meeting>(`/api/meetings/${meetingId}/recording`, {
    method: 'POST',
  });
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

export function startTranscription(meetingId: string | number) {
  return apiFetch<TranscriptionJobStart>(`/api/meetings/${meetingId}/transcription`, {
    method: 'POST',
  });
}

export function getTranscriptionJob(jobId: string | number) {
  return apiFetch<TranscriptionJob>(`/api/transcription-jobs/${jobId}`);
}

export function listTranscriptSegments(jobId: string | number) {
  return apiFetch<TranscriptSegment[]>(`/api/transcription-jobs/${jobId}/segments`);
}

export function retryTranscription(jobId: string | number) {
  return apiFetch<TranscriptionJobStart>(`/api/transcription-jobs/${jobId}/retry`, {
    method: 'POST',
  });
}

export function saveSpeakerMappings(jobId: string | number, mappings: Array<{ speaker: string; memberId: number }>) {
  return apiFetch<SpeakerMapping[]>(`/api/transcription-jobs/${jobId}/speaker-mapping`, {
    method: 'POST',
    body: JSON.stringify({ mappings }),
  });
}

export function startMinutesGeneration(meetingId: string | number) {
  return apiFetch<MinutesGenerationJobStart>(`/api/meetings/${meetingId}/minutes/generate`, {
    method: 'POST',
  });
}

export function getMinutesGenerationJob(jobId: string | number) {
  return apiFetch<MinutesGenerationJob>(`/api/minutes-generation-jobs/${jobId}`);
}

export function getMeetingMinutes(meetingId: string | number) {
  return apiFetch<MeetingMinutes>(`/api/meetings/${meetingId}/minutes`);
}

export function updateMeetingMinutes(meetingId: string | number, input: { title: string; fullSummary: string }) {
  return apiFetch<MeetingMinutes>(`/api/meetings/${meetingId}/minutes`, {
    method: 'PUT',
    body: JSON.stringify(input),
  });
}

export function listMinutesSegments(meetingId: string | number) {
  return apiFetch<TranscriptSegment[]>(`/api/meetings/${meetingId}/minutes/segments`);
}

export function updateActionItemStatus(actionItemId: string | number, status: ActionItemStatus) {
  return apiFetch<MinutesActionItem>(`/api/action-items/${actionItemId}/status`, {
    method: 'PUT',
    body: JSON.stringify({ status }),
  });
}

export function searchMeetings(teamId: string | number, keyword: string, page = 0, size = 20) {
  const params = new URLSearchParams({ keyword, page: String(page), size: String(size) });
  return apiFetch<Page<MeetingSearchResult>>(`/api/teams/${teamId}/meetings/search?${params.toString()}`);
}
