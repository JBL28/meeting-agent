'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/lib/auth-store';
import type { VoiceSample } from '@/lib/domain';
import { getMe } from '@/lib/auth-api';
import { listVoiceSamples, uploadVoiceSample } from '@/lib/upload-api';
import { formatDuration, useAudioRecorder } from '@/lib/use-audio-recorder';

export default function TeamProfilePage() {
  const params = useParams<{ teamId: string }>();
  const teamId = params.teamId;
  const user = useAuthStore((state) => state.user);
  const setSession = useAuthStore((state) => state.setSession);
  const [memberId, setMemberId] = useState<number | null>(user?.id ?? null);
  const [file, setFile] = useState<File | null>(null);
  const [consent, setConsent] = useState(false);
  const [samples, setSamples] = useState<VoiceSample[]>([]);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const voiceRecorder = useAudioRecorder({
    maxDurationSeconds: 10,
    minDurationSeconds: 2,
    upload: async (blob) => {
      if (!memberId) {
        throw new Error('Could not identify current member.');
      }
      const recordedFile = new File([blob], `voice-sample-${memberId}-${Date.now()}.webm`, {
        type: 'audio/webm;codecs=opus',
      });
      await uploadVoiceSample(teamId, memberId, { file: recordedFile, consent });
      setSamples(await listVoiceSamples(teamId, memberId));
      setMessage('Voice sample recording uploaded.');
    },
  });

  useEffect(() => {
    if (memberId) {
      listVoiceSamples(teamId, memberId).then(setSamples).catch(() => undefined);
      return;
    }
    getMe()
      .then((me) => {
        setMemberId(me.id);
        setSession(me, window.localStorage.getItem('accessToken') ?? '');
        return listVoiceSamples(teamId, me.id);
      })
      .then(setSamples)
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Could not load profile.'));
  }, [memberId, setSession, teamId]);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!file || !memberId) return;
    setMessage(null);
    setError(null);
    try {
      await uploadVoiceSample(teamId, memberId, { file, consent });
      setFile(null);
      setConsent(false);
      setSamples(await listVoiceSamples(teamId, memberId));
      setMessage('Voice sample uploaded.');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not upload voice sample.');
    }
  }

  return (
    <main className="mx-auto min-h-screen max-w-3xl px-6 py-10">
      <h1 className="text-3xl font-bold">Voice Sample Upload</h1>
      <p className="mt-2 text-slate-500">Upload wav, mp3, or webm files, or record a 2-10 second sample in the browser.</p>
      {message ? <p className="mt-6 rounded-md bg-green-50 p-3 text-sm text-green-700">{message}</p> : null}
      {error ? <p className="mt-6 rounded-md bg-red-50 p-3 text-sm text-red-600">{error}</p> : null}
      {voiceRecorder.error ? <p className="mt-6 rounded-md bg-red-50 p-3 text-sm text-red-600">{voiceRecorder.error}</p> : null}
      <form className="mt-8 space-y-5 rounded-2xl border bg-white p-6 shadow-sm" onSubmit={onSubmit}>
        <label className="block text-sm font-medium text-slate-700">
          Voice sample file
          <input className="mt-2 block w-full rounded-md border border-slate-300 p-2" type="file" accept=".wav,.mp3,.webm,audio/*" onChange={(event) => setFile(event.target.files?.[0] ?? null)} required />
        </label>
        <label className="flex items-center gap-2 text-sm text-slate-700">
          <input type="checkbox" checked={consent} onChange={(event) => setConsent(event.target.checked)} />
          I agree to store this voice sample for STT known speaker reference.
        </label>
        <Button disabled={!file}>Upload Sample</Button>
      </form>
      <section className="mt-8 rounded-2xl border bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold">Record Voice Sample</h2>
        <p className="mt-2 text-sm text-slate-500">
          Record for at least 2 seconds. Recording stops automatically at 10 seconds.
        </p>
        <div className="mt-5 rounded-xl bg-slate-50 p-5 text-center">
          <p className="text-sm uppercase tracking-wide text-slate-500">Elapsed</p>
          <p className="mt-2 font-mono text-4xl font-bold">{formatDuration(voiceRecorder.elapsedSeconds)}</p>
          <p className="mt-2 text-sm text-slate-500">Status: {voiceRecorder.status}</p>
        </div>
        {voiceRecorder.autoStopped ? (
          <p className="mt-4 rounded-md bg-amber-50 p-3 text-sm text-amber-700">
            The 10 second limit was reached, so recording stopped automatically.
          </p>
        ) : null}
        <div className="mt-5 flex flex-wrap gap-3">
          <Button
            type="button"
            onClick={voiceRecorder.start}
            disabled={!consent || !memberId || voiceRecorder.isRecording || voiceRecorder.status === 'requesting' || voiceRecorder.status === 'uploading'}
          >
            Start Sample Recording
          </Button>
          <Button type="button" variant="outline" onClick={() => voiceRecorder.stop()} disabled={!voiceRecorder.isRecording}>
            Stop and Upload
          </Button>
          <Button type="button" variant="outline" onClick={voiceRecorder.reset} disabled={voiceRecorder.isRecording || voiceRecorder.status === 'uploading'}>
            Reset Recorder
          </Button>
        </div>
        {!consent ? <p className="mt-3 text-sm text-slate-500">Consent is required before recording upload.</p> : null}
      </section>
      <section className="mt-8 rounded-2xl border bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold">Saved Samples</h2>
        <ul className="mt-4 divide-y">
          {samples.map((sample) => (
            <li key={sample.id} className="py-3 text-sm">
              <span className="font-medium">{sample.fileName}</span>
              <span className="ml-3 text-slate-500">{sample.durationSeconds ? `${sample.durationSeconds}s` : 'Duration not measured'}</span>
            </li>
          ))}
          {samples.length === 0 ? <li className="py-3 text-sm text-slate-500">No samples yet.</li> : null}
        </ul>
      </section>
    </main>
  );
}
