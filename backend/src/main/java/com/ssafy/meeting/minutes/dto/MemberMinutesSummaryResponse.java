package com.ssafy.meeting.minutes.dto;

import com.ssafy.meeting.minutes.domain.MemberMinutesSummary;

public class MemberMinutesSummaryResponse {
    private final Long id;
    private final Long memberId;
    private final String memberName;
    private final String progress;
    private final String issues;
    private final String nextTasks;

    private MemberMinutesSummaryResponse(MemberMinutesSummary summary) {
        this.id = summary.getId();
        this.memberId = summary.getMember().getId();
        this.memberName = summary.getMember().getName();
        this.progress = summary.getProgress();
        this.issues = summary.getIssues();
        this.nextTasks = summary.getNextTasks();
    }

    public static MemberMinutesSummaryResponse from(MemberMinutesSummary summary) {
        return new MemberMinutesSummaryResponse(summary);
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public String getProgress() { return progress; }
    public String getIssues() { return issues; }
    public String getNextTasks() { return nextTasks; }
}
