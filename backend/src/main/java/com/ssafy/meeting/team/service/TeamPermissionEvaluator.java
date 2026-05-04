package com.ssafy.meeting.team.service;

import com.ssafy.meeting.common.exception.ForbiddenException;
import com.ssafy.meeting.common.exception.ResourceNotFoundException;
import com.ssafy.meeting.team.domain.TeamMember;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.repository.TeamMemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("teamPermissionEvaluator")
public class TeamPermissionEvaluator {

    private final TeamMemberRepository teamMemberRepository;

    public TeamPermissionEvaluator(TeamMemberRepository teamMemberRepository) {
        this.teamMemberRepository = teamMemberRepository;
    }

    @Transactional(readOnly = true)
    public boolean hasTeamRole(Long memberId, Long teamId, String minimumRole) {
        if (memberId == null || teamId == null || minimumRole == null) {
            return false;
        }
        return teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId)
            .map(teamMember -> teamMember.getRole().atLeast(TeamRole.valueOf(minimumRole)))
            .orElse(false);
    }

    @Transactional(readOnly = true)
    public TeamMember requireRole(Long teamId, Long memberId, TeamRole minimumRole) {
        TeamMember teamMember = teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Team membership not found"));
        if (!teamMember.getRole().atLeast(minimumRole)) {
            throw new ForbiddenException("Required team role: " + minimumRole);
        }
        return teamMember;
    }
}
