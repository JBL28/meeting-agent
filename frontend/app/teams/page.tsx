'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { listTeams } from '@/lib/team-api';
import type { Team } from '@/lib/domain';

export default function TeamsPage() {
  const [teams, setTeams] = useState<Team[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listTeams()
      .then(setTeams)
      .catch((exception) => setError(exception instanceof Error ? exception.message : 'Could not load teams.'));
  }, []);

  return (
    <main className="mx-auto min-h-screen max-w-5xl px-6 py-10">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold">My Teams</h1>
          <p className="mt-2 text-slate-500">Review teams you belong to and your current role.</p>
        </div>
        <Button asChild><Link href="/teams/new">New Team</Link></Button>
      </div>
      <div className="mt-4 flex gap-3"><Button asChild variant="outline"><Link href="/meetings/new">New Meeting</Link></Button></div>
      {error ? <p className="mt-6 rounded-md bg-red-50 p-3 text-sm text-red-600">{error}</p> : null}
      <div className="mt-8 grid gap-4">
        {teams.map((team) => (
          <div key={team.id} className="rounded-2xl border bg-white p-5 shadow-sm">
            <Link href={`/teams/${team.id}/settings`} className="block transition hover:text-primary">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-semibold">{team.name}</h2>
                  <p className="text-sm text-slate-500">Owner ID: {team.ownerId}</p>
                </div>
                <span className="rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold">{team.myRole}</span>
              </div>
            </Link>
            <div className="mt-4 flex gap-3 text-sm font-semibold text-primary">
              <Link href={`/teams/${team.id}/settings`}>Settings</Link>
              <Link href={`/teams/${team.id}/profile`}>Voice Samples</Link>
            </div>
          </div>
        ))}
        {teams.length === 0 && !error ? <p className="rounded-2xl border border-dashed p-8 text-center text-slate-500">No teams yet.</p> : null}
      </div>
    </main>
  );
}
