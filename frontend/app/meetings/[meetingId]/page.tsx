'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { FormField } from '@/components/form-field';
import { Button } from '@/components/ui/button';
import type { Meeting } from '@/lib/domain';
import { addMeetingParticipant, getMeeting, uploadMeetingAudio } from '@/lib/upload-api';

export default function MeetingDetailPage() {
  const params = useParams<{ meetingId: string }>();
  const meetingId = params.meetingId;
  const [meeting, setMeeting] = useState<Meeting | null>(null);
  const [participantId, setParticipantId] = useState('');
  const [audioFile, setAudioFile] = useState<File | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setMeeting(await getMeeting(meetingId));
  }, [meetingId]);

  useEffect(() => {
    reload().catch((exception) => setError(exception instanceof Error ? exception.message : 'Could not load meeting.'));
  }, [reload]);

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
    </main>
  );
}
