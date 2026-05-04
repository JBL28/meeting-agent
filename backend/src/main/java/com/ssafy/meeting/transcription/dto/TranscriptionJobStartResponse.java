package com.ssafy.meeting.transcription.dto;

public class TranscriptionJobStartResponse {
    private final Long jobId;

    public TranscriptionJobStartResponse(Long jobId) {
        this.jobId = jobId;
    }

    public Long getJobId() {
        return jobId;
    }
}
