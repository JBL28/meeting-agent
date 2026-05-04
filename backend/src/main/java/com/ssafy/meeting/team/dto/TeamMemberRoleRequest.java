package com.ssafy.meeting.team.dto;

import com.ssafy.meeting.team.domain.TeamRole;
import javax.validation.constraints.NotNull;

public class TeamMemberRoleRequest {
    @NotNull
    private TeamRole role;

    public TeamRole getRole() { return role; }
    public void setRole(TeamRole role) { this.role = role; }
}
