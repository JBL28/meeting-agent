package com.ssafy.meeting.minutes.dto;

public class MinutesGenerationJobStartResponse {
    private final Long jobId;

    public MinutesGenerationJobStartResponse(Long jobId) {
        this.jobId = jobId;
    }

    public Long getJobId() {
        return jobId;
    }
}
