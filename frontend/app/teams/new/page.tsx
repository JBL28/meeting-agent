'use client';

import { useRouter } from 'next/navigation';
import { FormEvent, useState } from 'react';
import { FormField } from '@/components/form-field';
import { Button } from '@/components/ui/button';
import { createTeam } from '@/lib/team-api';

export default function NewTeamPage() {
  const router = useRouter();
  const [name, setName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const team = await createTeam({ name });
      router.push(`/teams/${team.id}/settings`);
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : '? ??? ??????.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="mx-auto min-h-screen max-w-2xl px-6 py-10">
      <h1 className="text-3xl font-bold">? ??</h1>
      <form className="mt-8 space-y-4 rounded-2xl border bg-white p-6 shadow-sm" onSubmit={onSubmit}>
        <FormField label="? ??" value={name} onChange={(event) => setName(event.target.value)} required maxLength={200} />
        {error ? <p className="rounded-md bg-red-50 p-3 text-sm text-red-600">{error}</p> : null}
        <Button disabled={loading}>{loading ? '?? ?...' : '? ??'}</Button>
      </form>
    </main>
  );
}
