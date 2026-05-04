import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { TeamMemberList } from './team-member-list';

const members = [
  { memberId: 1, email: 'owner@example.com', name: 'Owner', role: 'OWNER' as const },
  { memberId: 2, email: 'viewer@example.com', name: 'Viewer', role: 'VIEWER' as const },
];

describe('TeamMemberList', () => {
  it('shows member roles', () => {
    render(<TeamMemberList members={members} myRole="VIEWER" />);

    expect(screen.getByText('owner@example.com')).toBeInTheDocument();
    expect(screen.getByText('VIEWER')).toBeInTheDocument();
  });

  it('shows invite button only for admin or owner', () => {
    const { rerender } = render(<TeamMemberList members={members} myRole="VIEWER" />);
    expect(screen.queryByRole('button', { name: '?? ??' })).not.toBeInTheDocument();

    rerender(<TeamMemberList members={members} myRole="ADMIN" />);
    expect(screen.getByRole('button', { name: '?? ??' })).toBeInTheDocument();
  });
});
