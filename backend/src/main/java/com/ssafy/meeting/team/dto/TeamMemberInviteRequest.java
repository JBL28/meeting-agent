package com.ssafy.meeting.team.dto;

import com.ssafy.meeting.team.domain.TeamRole;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

public class TeamMemberInviteRequest {
    @Email
    @NotBlank
    private String email;

    private TeamRole role = TeamRole.MEMBER;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public TeamRole getRole() { return role; }
    public void setRole(TeamRole role) { this.role = role; }
}
