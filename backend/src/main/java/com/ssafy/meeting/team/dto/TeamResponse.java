package com.ssafy.meeting.team.dto;

import com.ssafy.meeting.team.domain.Team;
import com.ssafy.meeting.team.domain.TeamRole;

public class TeamResponse {
    private final Long id;
    private final String name;
    private final Long ownerId;
    private final TeamRole myRole;

    public TeamResponse(Long id, String name, Long ownerId, TeamRole myRole) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.myRole = myRole;
    }

    public static TeamResponse of(Team team, TeamRole myRole) {
        return new TeamResponse(team.getId(), team.getName(), team.getOwner().getId(), myRole);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public Long getOwnerId() { return ownerId; }
    public TeamRole getMyRole() { return myRole; }
}
