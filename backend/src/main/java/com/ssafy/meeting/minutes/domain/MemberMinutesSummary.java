package com.ssafy.meeting.minutes.domain;

import com.ssafy.meeting.member.domain.Member;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "member_minutes_summaries")
public class MemberMinutesSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "minutes_id", nullable = false)
    private MeetingMinutes minutes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String progress;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String issues;

    @Column(name = "next_tasks", nullable = false, columnDefinition = "TEXT")
    private String nextTasks;

    protected MemberMinutesSummary() {
    }

    public MemberMinutesSummary(MeetingMinutes minutes, Member member, String progress, String issues, String nextTasks) {
        this.minutes = minutes;
        this.member = member;
        this.progress = progress;
        this.issues = issues;
        this.nextTasks = nextTasks;
    }

    public Long getId() { return id; }
    public MeetingMinutes getMinutes() { return minutes; }
    public Member getMember() { return member; }
    public String getProgress() { return progress; }
    public String getIssues() { return issues; }
    public String getNextTasks() { return nextTasks; }
}
