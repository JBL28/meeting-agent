package com.ssafy.meeting.team.domain;

import com.ssafy.meeting.member.domain.Member;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "team_members", uniqueConstraints = @UniqueConstraint(name = "uk_team_members_team_member", columnNames = {"team_id", "member_id"}))
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamRole role;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    protected TeamMember() {
    }

    public TeamMember(Team team, Member member, TeamRole role) {
        this.team = team;
        this.member = member;
        this.role = role;
    }

    @PrePersist
    void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    public void changeRole(TeamRole role) {
        this.role = role;
    }

    public Long getId() { return id; }
    public Team getTeam() { return team; }
    public Member getMember() { return member; }
    public TeamRole getRole() { return role; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
}
