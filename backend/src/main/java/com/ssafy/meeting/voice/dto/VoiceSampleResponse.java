package com.ssafy.meeting.voice.dto;

import com.ssafy.meeting.voice.domain.VoiceSample;
import java.time.LocalDateTime;

public class VoiceSampleResponse {
    private final Long id;
    private final Long memberId;
    private final Long teamId;
    private final String fileName;
    private final Integer durationSeconds;
    private final LocalDateTime createdAt;
    private final LocalDateTime consentAgreedAt;

    public VoiceSampleResponse(Long id, Long memberId, Long teamId, String fileName, Integer durationSeconds, LocalDateTime createdAt, LocalDateTime consentAgreedAt) {
        this.id = id;
        this.memberId = memberId;
        this.teamId = teamId;
        this.fileName = fileName;
        this.durationSeconds = durationSeconds;
        this.createdAt = createdAt;
        this.consentAgreedAt = consentAgreedAt;
    }

    public static VoiceSampleResponse from(VoiceSample sample) {
        return new VoiceSampleResponse(
            sample.getId(),
            sample.getMember().getId(),
            sample.getTeam().getId(),
            sample.getFileName(),
            sample.getDurationSeconds(),
            sample.getCreatedAt(),
            sample.getConsentAgreedAt()
        );
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public Long getTeamId() { return teamId; }
    public String getFileName() { return fileName; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getConsentAgreedAt() { return consentAgreedAt; }
}
