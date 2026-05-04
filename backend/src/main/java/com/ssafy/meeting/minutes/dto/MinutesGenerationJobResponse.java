package com.ssafy.meeting.minutes.dto;

import com.ssafy.meeting.minutes.domain.MinutesGenerationJob;
import com.ssafy.meeting.minutes.domain.MinutesGenerationJobStatus;
import java.time.LocalDateTime;

public class MinutesGenerationJobResponse {
    private final Long id;
    private final Long meetingId;
    private final MinutesGenerationJobStatus status;
    private final String errorMessage;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final LocalDateTime createdAt;

    private MinutesGenerationJobResponse(MinutesGenerationJob job) {
        this.id = job.getId();
        this.meetingId = job.getMeeting().getId();
        this.status = job.getStatus();
        this.errorMessage = job.getErrorMessage();
        this.startedAt = job.getStartedAt();
        this.completedAt = job.getCompletedAt();
        this.createdAt = job.getCreatedAt();
    }

    public static MinutesGenerationJobResponse from(MinutesGenerationJob job) {
        return new MinutesGenerationJobResponse(job);
    }

    public Long getId() { return id; }
    public Long getMeetingId() { return meetingId; }
    public MinutesGenerationJobStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
