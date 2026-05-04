package com.ssafy.meeting.team.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ssafy.meeting.common.exception.ForbiddenException;
import com.ssafy.meeting.team.domain.TeamMember;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.repository.TeamMemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TeamPermissionEvaluatorTest {

    private final TeamMemberRepository teamMemberRepository = mock(TeamMemberRepository.class);
    private final TeamPermissionEvaluator evaluator = new TeamPermissionEvaluator(teamMemberRepository);

    @Test
    void hasTeamRoleUsesOwnerAdminMemberViewerOrder() {
        TeamMember viewer = mock(TeamMember.class);
        when(viewer.getRole()).thenReturn(TeamRole.VIEWER);
        when(teamMemberRepository.findByTeamIdAndMemberId(1L, 10L)).thenReturn(Optional.of(viewer));

        assertThat(evaluator.hasTeamRole(10L, 1L, "VIEWER")).isTrue();
        assertThat(evaluator.hasTeamRole(10L, 1L, "MEMBER")).isFalse();
    }

    @Test
    void requireRoleThrowsForbiddenWhenRoleIsInsufficient() {
        TeamMember viewer = mock(TeamMember.class);
        when(viewer.getRole()).thenReturn(TeamRole.VIEWER);
        when(teamMemberRepository.findByTeamIdAndMemberId(1L, 10L)).thenReturn(Optional.of(viewer));

        assertThatThrownBy(() -> evaluator.requireRole(1L, 10L, TeamRole.ADMIN))
            .isInstanceOf(ForbiddenException.class);
    }
}
