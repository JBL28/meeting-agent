package com.ssafy.meeting.meeting.dto;

import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.domain.MeetingStatus;
import java.time.LocalDateTime;
import java.util.List;

public class MeetingResponse {
    private final Long id;
    private final Long teamId;
    private final String title;
    private final LocalDateTime scheduledAt;
    private final MeetingStatus status;
    private final Long createdBy;
    private final LocalDateTime createdAt;
    private final List<MeetingParticipantResponse> participants;

    public MeetingResponse(Long id, Long teamId, String title, LocalDateTime scheduledAt, MeetingStatus status, Long createdBy, LocalDateTime createdAt, List<MeetingParticipantResponse> participants) {
        this.id = id;
        this.teamId = teamId;
        this.title = title;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.participants = participants;
    }

    public static MeetingResponse of(Meeting meeting, List<MeetingParticipantResponse> participants) {
        return new MeetingResponse(
            meeting.getId(),
            meeting.getTeam().getId(),
            meeting.getTitle(),
            meeting.getScheduledAt(),
            meeting.getStatus(),
            meeting.getCreatedBy().getId(),
            meeting.getCreatedAt(),
            participants
        );
    }

    public Long getId() { return id; }
    public Long getTeamId() { return teamId; }
    public String getTitle() { return title; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public MeetingStatus getStatus() { return status; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<MeetingParticipantResponse> getParticipants() { return participants; }
}
