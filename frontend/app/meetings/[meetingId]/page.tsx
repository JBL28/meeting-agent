'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { FormField } from '@/components/form-field';
import { Button } from '@/components/ui/button';
import type { Meeting, TranscriptSegment, TranscriptionJob } from '@/lib/domain';
import { addMeetingParticipant, getMeeting, getTranscriptionJob, listTranscriptSegments, retryTranscription, saveSpeakerMappings, startTranscription, uploadMeetingAudio } from '@/lib/upload-api';

export default function MeetingDetailPage() {
  const params = useParams<{ meetingId: string }>();
  const meetingId = params.meetingId;
  const [meeting, setMeeting] = useState<Meeting | null>(null);
  const [participantId, setParticipantId] = useState('');
  const [audioFile, setAudioFile] = useState<File | null>(null);
  const [transcriptionJob, setTranscriptionJob] = useState<TranscriptionJob | null>(null);
  const [segments, setSegments] = useState<TranscriptSegment[]>([]);
  const [speakerMappings, setSpeakerMappings] = useState<Record<string, string>>({});
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setMeeting(await getMeeting(meetingId));
  }, [meetingId]);

  useEffect(() => {
    reload().catch((exception) => setError(exception instanceof Error ? exception.message : 'Could not load meeting.'));
  }, [reload]);

  useEffect(() => {
    if (!transcriptionJob || transcriptionJob.status === 'COMPLETED' || transcriptionJob.status === 'FAILED' || transcriptionJob.status === 'CANCELED') {
      return;
    }
    const timer = window.setInterval(async () => {
      try {
        const nextJob = await getTranscriptionJob(transcriptionJob.id);
        setTranscriptionJob(nextJob);
        if (nextJob.status === 'COMPLETED') {
          window.clearInterval(timer);
          setSegments(await listTranscriptSegments(nextJob.id));
          await reload();
          setMessage('Transcription completed.');
        }
        if (nextJob.status === 'FAILED') {
          window.clearInterval(timer);
          setError(nextJob.errorMessage ?? 'Transcription failed.');
          await reload();
        }
      } catch (exception) {
        setError(exception instanceof Error ? exception.message : 'Could not poll transcription job.');
      }
    }, 2000);
    return () => window.clearInterval(timer);
  }, [reload, transcriptionJob]);

  async function onAddParticipant(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    setError(null);
    try {
      await addMeetingParticipant(meetingId, Number(participantId));
      setParticipantId('');
      await reload();
      setMessage('Participant added.');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not add participant.');
    }
  }

  async function onUploadAudio(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!audioFile) return;
    setMessage(null);
    setError(null);
    try {
      await uploadMeetingAudio(meetingId, audioFile);
      setAudioFile(null);
      await reload();
      setMessage('Meeting audio uploaded.');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not upload meeting audio.');
    }
  }

  async function onStartTranscription() {
    setMessage(null);
    setError(null);
    setSegments([]);
    try {
      const started = await startTranscription(meetingId);
      const job = await getTranscriptionJob(started.jobId);
      setTranscriptionJob(job);
      setMessage('Transcription started.');
      await reload();
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not start transcription.');
    }
  }

  async function onRetryTranscription() {
    if (!transcriptionJob) return;
    setMessage(null);
    setError(null);
    try {
      const started = await retryTranscription(transcriptionJob.id);
      setTranscriptionJob(await getTranscriptionJob(started.jobId));
      setMessage('Transcription retry started.');
      await reload();
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not retry transcription.');
    }
  }

  async function onSaveMappings() {
    if (!transcriptionJob) return;
    const mappings = Object.entries(speakerMappings)
      .filter(([, memberId]) => memberId)
      .map(([speaker, memberId]) => ({ speaker, memberId: Number(memberId) }));
    if (mappings.length === 0) return;
    setMessage(null);
    setError(null);
    try {
      await saveSpeakerMappings(transcriptionJob.id, mappings);
      setSegments(await listTranscriptSegments(transcriptionJob.id));
      setMessage('Speaker mappings saved.');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not save speaker mappings.');
    }
  }

  if (!meeting) {
    return <main className="mx-auto min-h-screen max-w-4xl px-6 py-10">Loading meeting...</main>;
  }

  return (
    <main className="mx-auto min-h-screen max-w-4xl space-y-6 px-6 py-10">
      <section>
        <h1 className="text-3xl font-bold">{meeting.title}</h1>
        <p className="mt-2 text-slate-500">Status: <strong>{meeting.status}</strong> - Team ID: {meeting.teamId}</p>
        <Link className="mt-4 inline-flex text-sm font-medium text-slate-700 underline" href={`/meetings/${meetingId}/record`}>
          Open browser recorder
        </Link>
        <Link className="ml-4 mt-4 inline-flex text-sm font-medium text-slate-700 underline" href={`/meetings/${meetingId}/minutes`}>
          Open minutes
        </Link>
      </section>
      {message ? <p className="rounded-md bg-green-50 p-3 text-sm text-green-700">{message}</p> : null}
      {error ? <p className="rounded-md bg-red-50 p-3 text-sm text-red-600">{error}</p> : null}
      <section className="rounded-2xl border bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold">Participants</h2>
        <ul className="mt-4 divide-y">
          {meeting.participants.map((participant) => (
            <li key={participant.memberId} className="py-3 text-sm">
              <span className="font-medium">{participant.name}</span>
              <span className="ml-3 text-slate-500">{participant.email}</span>
            </li>
          ))}
        </ul>
        <form className="mt-4 flex gap-3" onSubmit={onAddParticipant}>
          <FormField label="Member ID" value={participantId} onChange={(event) => setParticipantId(event.target.value)} required />
          <div className="flex items-end"><Button>Add Participant</Button></div>
        </form>
      </section>
      <form className="rounded-2xl border bg-white p-6 shadow-sm" onSubmit={onUploadAudio}>
        <h2 className="text-xl font-semibold">Meeting Audio Upload</h2>
        <p className="mt-2 text-sm text-slate-500">Select or drop a wav, mp3, webm, or m4a file.</p>
        <input className="mt-4 block w-full rounded-md border border-dashed border-slate-300 p-6" type="file" accept=".wav,.mp3,.webm,.m4a,audio/*" onChange={(event) => setAudioFile(event.target.files?.[0] ?? null)} required />
        <Button className="mt-4" disabled={!audioFile}>Upload Audio</Button>
      </form>
      <section className="rounded-2xl border bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold">STT Transcription</h2>
        <p className="mt-2 text-sm text-slate-500">Start diarized transcription after meeting audio is RECORDED.</p>
        <div className="mt-4 flex flex-wrap gap-3">
          <Button type="button" onClick={onStartTranscription} disabled={meeting.status !== 'RECORDED' || transcriptionJob?.status === 'CREATED' || transcriptionJob?.status === 'PROCESSING'}>
            Start STT
          </Button>
          <Button type="button" variant="outline" onClick={onRetryTranscription} disabled={transcriptionJob?.status !== 'FAILED'}>
            Retry Failed STT
          </Button>
        </div>
        {transcriptionJob ? (
          <div className="mt-4 rounded-md bg-slate-50 p-4 text-sm">
            <p>Job ID: {transcriptionJob.id}</p>
            <p>Status: <strong>{transcriptionJob.status}</strong></p>
            {transcriptionJob.status === 'CREATED' || transcriptionJob.status === 'PROCESSING' ? <p className="mt-2">Polling every 2 seconds...</p> : null}
            {transcriptionJob.errorMessage ? <p className="mt-2 text-red-600">{transcriptionJob.errorMessage}</p> : null}
          </div>
        ) : null}
      </section>
      {segments.length > 0 ? (
        <section className="rounded-2xl border bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold">Transcript Segments</h2>
          <div className="mt-4 space-y-3">
            {segments.map((segment) => (
              <div key={segment.id} className="rounded-md border p-3 text-sm">
                <p className="font-medium">{segment.speaker} [{segment.startTime.toFixed(1)}s - {segment.endTime.toFixed(1)}s]</p>
                <p className="mt-1 text-slate-700">{segment.text}</p>
              </div>
            ))}
          </div>
          <div className="mt-6 space-y-3">
            <h3 className="font-semibold">Speaker Mapping</h3>
            {Array.from(new Set(segments.map((segment) => segment.speaker))).map((speaker) => (
              <label key={speaker} className="block text-sm font-medium text-slate-700">
                {speaker}
                <select
                  className="mt-1 block w-full rounded-md border border-slate-300 p-2"
                  value={speakerMappings[speaker] ?? ''}
                  onChange={(event) => setSpeakerMappings((previous) => ({ ...previous, [speaker]: event.target.value }))}
                >
                  <option value="">Select participant</option>
                  {meeting.participants.map((participant) => (
                    <option key={participant.memberId} value={participant.memberId}>{participant.name} ({participant.email})</option>
                  ))}
                </select>
              </label>
            ))}
            <Button type="button" onClick={onSaveMappings}>Save Mappings</Button>
          </div>
        </section>
      ) : null}
    </main>
  );
}
