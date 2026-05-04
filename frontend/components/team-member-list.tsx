import type { TeamMember, TeamRole } from '@/lib/domain';
import { canInvite } from '@/lib/domain';
import { Button } from './ui/button';

type TeamMemberListProps = {
  members: TeamMember[];
  myRole: TeamRole;
  onInviteClick?: () => void;
};

export function TeamMemberList({ members, myRole, onInviteClick }: TeamMemberListProps) {
  return (
    <section className="rounded-2xl border bg-white p-6 shadow-sm">
      <div className="flex items-center justify-between gap-3">
        <div>
          <h2 className="text-xl font-semibold">Team Members</h2>
          <p className="text-sm text-slate-500">Review roles and permissions.</p>
        </div>
        {canInvite(myRole) ? <Button onClick={onInviteClick}>Invite Member</Button> : null}
      </div>
      <ul className="mt-5 divide-y">
        {members.map((member) => (
          <li key={member.memberId} className="flex items-center justify-between py-3">
            <div>
              <p className="font-medium">{member.name}</p>
              <p className="text-sm text-slate-500">{member.email}</p>
            </div>
            <span className="rounded-full bg-slate-100 px-3 py-1 text-sm font-semibold text-slate-700">{member.role}</span>
          </li>
        ))}
      </ul>
    </section>
  );
}
