'use client';

import { FormEvent, useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { Button } from '@/components/ui/button';
import { useAuthStore } from '@/lib/auth-store';
import type { VoiceSample } from '@/lib/domain';
import { getMe } from '@/lib/auth-api';
import { listVoiceSamples, uploadVoiceSample } from '@/lib/upload-api';

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
      <p className="mt-2 text-slate-500">Upload wav, mp3, or webm files. Recording UI is planned for Phase 2B.</p>
      {message ? <p className="mt-6 rounded-md bg-green-50 p-3 text-sm text-green-700">{message}</p> : null}
      {error ? <p className="mt-6 rounded-md bg-red-50 p-3 text-sm text-red-600">{error}</p> : null}
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
