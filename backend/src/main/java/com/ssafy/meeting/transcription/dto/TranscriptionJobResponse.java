package com.ssafy.meeting.transcription.dto;

import com.ssafy.meeting.transcription.domain.TranscriptionJob;
import com.ssafy.meeting.transcription.domain.TranscriptionJobStatus;
import java.time.LocalDateTime;

public class TranscriptionJobResponse {
    private final Long id;
    private final Long meetingId;
    private final Long audioFileId;
    private final TranscriptionJobStatus status;
    private final String errorMessage;
    private final String rawResponsePath;
    private final LocalDateTime startedAt;
    private final LocalDateTime completedAt;
    private final LocalDateTime createdAt;

    private TranscriptionJobResponse(TranscriptionJob job) {
        this.id = job.getId();
        this.meetingId = job.getMeeting().getId();
        this.audioFileId = job.getAudioFile().getId();
        this.status = job.getStatus();
        this.errorMessage = job.getErrorMessage();
        this.rawResponsePath = job.getRawResponsePath();
        this.startedAt = job.getStartedAt();
        this.completedAt = job.getCompletedAt();
        this.createdAt = job.getCreatedAt();
    }

    public static TranscriptionJobResponse from(TranscriptionJob job) {
        return new TranscriptionJobResponse(job);
    }

    public Long getId() { return id; }
    public Long getMeetingId() { return meetingId; }
    public Long getAudioFileId() { return audioFileId; }
    public TranscriptionJobStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public String getRawResponsePath() { return rawResponsePath; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
