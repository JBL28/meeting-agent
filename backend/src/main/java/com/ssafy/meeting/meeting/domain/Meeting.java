package com.ssafy.meeting.meeting.domain;

import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.team.domain.Team;
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

@Entity
@Table(name = "meetings")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MeetingStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private Member createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Meeting() {
    }

    public Meeting(Team team, String title, LocalDateTime scheduledAt, Member createdBy) {
        this.team = team;
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.createdBy = createdBy;
        this.status = MeetingStatus.DRAFT;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markRecorded() {
        this.status = MeetingStatus.RECORDED;
    }

    public Long getId() { return id; }
    public Team getTeam() { return team; }
    public String getTitle() { return title; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public MeetingStatus getStatus() { return status; }
    public Member getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
