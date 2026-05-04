package com.ssafy.meeting.team.domain;

public enum TeamRole {
    VIEWER(0),
    MEMBER(1),
    ADMIN(2),
    OWNER(3);

    private final int level;

    TeamRole(int level) {
        this.level = level;
    }

    public boolean atLeast(TeamRole minimum) {
        return this.level >= minimum.level;
    }
}
