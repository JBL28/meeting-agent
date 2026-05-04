package com.ssafy.meeting.team.dto;

import com.ssafy.meeting.team.domain.TeamMember;
import com.ssafy.meeting.team.domain.TeamRole;

public class TeamMemberResponse {
    private final Long memberId;
    private final String email;
    private final String name;
    private final TeamRole role;

    public TeamMemberResponse(Long memberId, String email, String name, TeamRole role) {
        this.memberId = memberId;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public static TeamMemberResponse from(TeamMember teamMember) {
        return new TeamMemberResponse(
            teamMember.getMember().getId(),
            teamMember.getMember().getEmail(),
            teamMember.getMember().getName(),
            teamMember.getRole()
        );
    }

    public Long getMemberId() { return memberId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public TeamRole getRole() { return role; }
}
