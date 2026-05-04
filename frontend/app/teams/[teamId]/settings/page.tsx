'use client';

import { FormEvent, useCallback, useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { FormField } from '@/components/form-field';
import { TeamMemberList } from '@/components/team-member-list';
import { Button } from '@/components/ui/button';
import { canInvite, type Team, type TeamMember, type TeamRole } from '@/lib/domain';
import { getTeam, inviteTeamMember, listTeamMembers, updateTeam } from '@/lib/team-api';

export default function TeamSettingsPage() {
  const params = useParams<{ teamId: string }>();
  const teamId = params.teamId;
  const [team, setTeam] = useState<Team | null>(null);
  const [members, setMembers] = useState<TeamMember[]>([]);
  const [name, setName] = useState('');
  const [inviteEmail, setInviteEmail] = useState('');
  const [inviteRole, setInviteRole] = useState<TeamRole>('MEMBER');
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    const [teamResponse, membersResponse] = await Promise.all([getTeam(teamId), listTeamMembers(teamId)]);
    setTeam(teamResponse);
    setName(teamResponse.name);
    setMembers(membersResponse);
  }, [teamId]);

  useEffect(() => {
    reload().catch((exception) => setError(exception instanceof Error ? exception.message : 'Could not load team.'));
  }, [reload]);

  async function onUpdate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    setError(null);
    try {
      const updated = await updateTeam(teamId, { name });
      setTeam(updated);
      setMessage('Team updated.');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not update team.');
    }
  }

  async function onInvite(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(null);
    setError(null);
    try {
      await inviteTeamMember(teamId, { email: inviteEmail, role: inviteRole });
      setInviteEmail('');
      setInviteRole('MEMBER');
      await reload();
      setMessage('Member invited.');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Could not invite member.');
    }
  }

  if (!team) {
    return <main className="mx-auto min-h-screen max-w-5xl px-6 py-10">Loading team...</main>;
  }

  return (
    <main className="mx-auto min-h-screen max-w-5xl space-y-6 px-6 py-10">
      <div>
        <h1 className="text-3xl font-bold">{team.name} Settings</h1>
        <p className="mt-2 text-slate-500">My role: <strong>{team.myRole}</strong></p>
      </div>
      {message ? <p className="rounded-md bg-green-50 p-3 text-sm text-green-700">{message}</p> : null}
      {error ? <p className="rounded-md bg-red-50 p-3 text-sm text-red-600">{error}</p> : null}
      <form className="rounded-2xl border bg-white p-6 shadow-sm" onSubmit={onUpdate}>
        <FormField label="Team name" value={name} onChange={(event) => setName(event.target.value)} disabled={team.myRole !== 'OWNER'} />
        {team.myRole === 'OWNER' ? <Button className="mt-4">Save</Button> : <p className="mt-3 text-sm text-slate-500">Only OWNER can rename the team.</p>}
      </form>
      <TeamMemberList members={members} myRole={team.myRole} />
      {canInvite(team.myRole) ? (
        <form className="rounded-2xl border bg-white p-6 shadow-sm" onSubmit={onInvite}>
          <h2 className="text-xl font-semibold">Invite Member</h2>
          <div className="mt-4 grid gap-4 md:grid-cols-[1fr_160px_auto]">
            <FormField label="Email" type="email" value={inviteEmail} onChange={(event) => setInviteEmail(event.target.value)} required />
            <label className="block text-sm font-medium text-slate-700">
              Role
              <select className="mt-2 w-full rounded-md border border-slate-300 px-3 py-2" value={inviteRole} onChange={(event) => setInviteRole(event.target.value as TeamRole)}>
                <option value="VIEWER">VIEWER</option>
                <option value="MEMBER">MEMBER</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </label>
            <div className="flex items-end"><Button>Invite</Button></div>
          </div>
        </form>
      ) : null}
    </main>
  );
}
