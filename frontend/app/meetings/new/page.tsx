'use client';

import { useRouter } from 'next/navigation';
import { FormEvent, useState } from 'react';
import { FormField } from '@/components/form-field';
import { Button } from '@/components/ui/button';
import { createMeeting } from '@/lib/upload-api';

export default function NewMeetingPage() {
  const router = useRouter();
  const [teamId, setTeamId] = useState('');
  const [title, setTitle] = useState('');
  const [scheduledAt, setScheduledAt] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const meeting = await createMeeting(teamId, { title, scheduledAt: scheduledAt || undefined });
      router.push(`/meetings/${meeting.id}`);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not create meeting.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="mx-auto min-h-screen max-w-2xl px-6 py-10">
      <h1 className="text-3xl font-bold">New Meeting</h1>
      <form className="mt-8 space-y-4 rounded-2xl border bg-white p-6 shadow-sm" onSubmit={onSubmit}>
        <FormField label="Team ID" value={teamId} onChange={(event) => setTeamId(event.target.value)} required />
        <FormField label="Meeting title" value={title} onChange={(event) => setTitle(event.target.value)} required maxLength={300} />
        <FormField label="Scheduled at" type="datetime-local" value={scheduledAt} onChange={(event) => setScheduledAt(event.target.value)} />
        {error ? <p className="rounded-md bg-red-50 p-3 text-sm text-red-600">{error}</p> : null}
        <Button disabled={loading}>{loading ? 'Creating...' : 'Create Meeting'}</Button>
      </form>
    </main>
  );
}
