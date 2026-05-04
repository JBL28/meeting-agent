package com.ssafy.meeting.search.dto;

import com.ssafy.meeting.meeting.domain.MeetingStatus;
import java.time.LocalDateTime;

public class MeetingSearchResult {
    private final Long meetingId;
    private final String title;
    private final MeetingStatus status;
    private final String snippet;
    private final LocalDateTime createdAt;

    public MeetingSearchResult(Long meetingId, String title, MeetingStatus status, String snippet, LocalDateTime createdAt) {
        this.meetingId = meetingId;
        this.title = title;
        this.status = status;
        this.snippet = snippet;
        this.createdAt = createdAt;
    }

    public Long getMeetingId() { return meetingId; }
    public String getTitle() { return title; }
    public MeetingStatus getStatus() { return status; }
    public String getSnippet() { return snippet; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
