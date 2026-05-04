'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import type { MeetingMinutes, MinutesGenerationJob, TranscriptSegment } from '@/lib/domain';
import { getMeetingMinutes, getMinutesGenerationJob, listMinutesSegments, startMinutesGeneration, updateActionItemStatus, updateMeetingMinutes } from '@/lib/upload-api';

export default function MeetingMinutesPage() {
  const params = useParams<{ meetingId: string }>();
  const meetingId = params.meetingId;
  const [minutes, setMinutes] = useState<MeetingMinutes | null>(null);
  const [job, setJob] = useState<MinutesGenerationJob | null>(null);
  const [segments, setSegments] = useState<TranscriptSegment[]>([]);
  const [editTitle, setEditTitle] = useState('');
  const [editSummary, setEditSummary] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadMinutes = useCallback(async () => {
    const loaded = await getMeetingMinutes(meetingId);
    setMinutes(loaded);
    setEditTitle(loaded.title);
    setEditSummary(loaded.fullSummary);
    setSegments(await listMinutesSegments(meetingId));
  }, [meetingId]);

  useEffect(() => {
    loadMinutes().catch(() => undefined);
  }, [loadMinutes]);

  useEffect(() => {
    if (!job || job.status === 'COMPLETED' || job.status === 'FAILED') {
      return;
    }
    const timer = window.setInterval(async () => {
      try {
        const next = await getMinutesGenerationJob(job.id);
        setJob(next);
        if (next.status === 'COMPLETED') {
          window.clearInterval(timer);
          await loadMinutes();
          setMessage('Minutes generated.');
        }
        if (next.status === 'FAILED') {
          window.clearInterval(timer);
          setError(next.errorMessage ?? 'Minutes generation failed.');
        }
      } catch (exception) {
        setError(exception instanceof Error ? exception.message : 'Could not poll minutes job.');
      }
    }, 2000);
    return () => window.clearInterval(timer);
  }, [job, loadMinutes]);

  async function onGenerate() {
    setMessage(null);
    setError(null);
    try {
      const started = await startMinutesGeneration(meetingId);
      setJob(await getMinutesGenerationJob(started.jobId));
      setMessage('Minutes generation started.');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not start minutes generation.');
    }
  }

  async function onSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    setError(null);
    try {
      const updated = await updateMeetingMinutes(meetingId, { title: editTitle, fullSummary: editSummary });
      setMinutes(updated);
      setMessage('Minutes updated.');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not update minutes.');
    }
  }

  async function onStatus(actionItemId: number, status: 'TODO' | 'IN_PROGRESS' | 'DONE') {
    setMessage(null);
    setError(null);
    try {
      await updateActionItemStatus(actionItemId, status);
      await loadMinutes();
      setMessage('Action item status updated.');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not update action item.');
    }
  }

  return (
    <main className="mx-auto min-h-screen max-w-5xl space-y-6 px-6 py-10">
      <section>
        <p className="text-sm text-slate-500">Meeting ID: {meetingId}</p>
        <h1 className="mt-2 text-3xl font-bold">Meeting Minutes</h1>
        <div className="mt-4 flex gap-4">
          <Link className="text-sm font-medium text-slate-700 underline" href={`/meetings/${meetingId}`}>Back to meeting</Link>
          <Button type="button" onClick={onGenerate} disabled={job?.status === 'CREATED' || job?.status === 'PROCESSING'}>Generate Minutes</Button>
        </div>
      </section>

      {message ? <p className="rounded-md bg-green-50 p-3 text-sm text-green-700">{message}</p> : null}
      {error ? <p className="rounded-md bg-red-50 p-3 text-sm text-red-600">{error}</p> : null}
      {job ? (
        <section className="rounded-2xl border bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold">Generation Job</h2>
          <p className="mt-2 text-sm">Job ID: {job.id}</p>
          <p className="text-sm">Status: <strong>{job.status}</strong></p>
          {job.status === 'CREATED' || job.status === 'PROCESSING' ? <p className="mt-2 text-sm text-slate-500">Polling every 2 seconds...</p> : null}
        </section>
      ) : null}

      {minutes ? (
        <>
          <form className="rounded-2xl border bg-white p-6 shadow-sm" onSubmit={onSave}>
            <h2 className="text-xl font-semibold">Summary</h2>
            <label className="mt-4 block text-sm font-medium text-slate-700">
              Title
              <input className="mt-1 block w-full rounded-md border border-slate-300 p-2" value={editTitle} onChange={(event) => setEditTitle(event.target.value)} />
            </label>
            <label className="mt-4 block text-sm font-medium text-slate-700">
              Full summary
              <textarea className="mt-1 block min-h-40 w-full rounded-md border border-slate-300 p-2" value={editSummary} onChange={(event) => setEditSummary(event.target.value)} />
            </label>
            <Button className="mt-4">Save Minutes</Button>
          </form>

          <section className="rounded-2xl border bg-white p-6 shadow-sm">
            <h2 className="text-xl font-semibold">Member Summaries</h2>
            <div className="mt-4 grid gap-4 md:grid-cols-2">
              {minutes.memberSummaries.map((summary) => (
                <article key={summary.id} className="rounded-md border p-4 text-sm">
                  <h3 className="font-semibold">{summary.memberName}</h3>
                  <p className="mt-2"><strong>Progress:</strong> {summary.progress}</p>
                  <p className="mt-2"><strong>Issues:</strong> {summary.issues}</p>
                  <p className="mt-2"><strong>Next:</strong> {summary.nextTasks}</p>
                </article>
              ))}
            </div>
          </section>

          <section className="rounded-2xl border bg-white p-6 shadow-sm">
            <h2 className="text-xl font-semibold">Decisions</h2>
            <ul className="mt-4 list-disc space-y-2 pl-5 text-sm">
              {minutes.decisions.map((decision) => <li key={decision}>{decision}</li>)}
            </ul>
          </section>

          <section className="rounded-2xl border bg-white p-6 shadow-sm">
            <h2 className="text-xl font-semibold">Action Items</h2>
            <div className="mt-4 space-y-3">
              {minutes.actionItems.map((item) => (
                <div key={item.id} className="rounded-md border p-4 text-sm">
                  <p className="font-medium">{item.content}</p>
                  <p className="mt-1 text-slate-500">Assignee: {item.assigneeName ?? 'Unassigned'} / Due: {item.dueDate ?? 'None'}</p>
                  <select className="mt-3 rounded-md border border-slate-300 p-2" value={item.status} onChange={(event) => onStatus(item.id, event.target.value as 'TODO' | 'IN_PROGRESS' | 'DONE')}>
                    <option value="TODO">TODO</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="DONE">DONE</option>
                  </select>
                </div>
              ))}
            </div>
          </section>
        </>
      ) : (
        <section className="rounded-2xl border bg-white p-6 text-sm text-slate-500 shadow-sm">
          No minutes yet. Generate after the meeting reaches TRANSCRIBED.
        </section>
      )}

      {segments.length > 0 ? (
        <section className="rounded-2xl border bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold">Original Transcript</h2>
          <div className="mt-4 space-y-2">
            {segments.map((segment) => (
              <p key={segment.id} className="text-sm"><strong>{segment.speaker}</strong>: {segment.text}</p>
            ))}
          </div>
        </section>
      ) : null}
    </main>
  );
}
