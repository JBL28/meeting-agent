package com.ssafy.meeting.transcription.dto;

import com.ssafy.meeting.transcription.domain.SpeakerMapping;

public class SpeakerMappingResponse {
    private final Long id;
    private final String speaker;
    private final Long memberId;
    private final String memberName;
    private final Boolean autoMapped;

    private SpeakerMappingResponse(SpeakerMapping mapping) {
        this.id = mapping.getId();
        this.speaker = mapping.getSpeaker();
        this.memberId = mapping.getMember().getId();
        this.memberName = mapping.getMember().getName();
        this.autoMapped = mapping.getAutoMapped();
    }

    public static SpeakerMappingResponse from(SpeakerMapping mapping) {
        return new SpeakerMappingResponse(mapping);
    }

    public Long getId() { return id; }
    public String getSpeaker() { return speaker; }
    public Long getMemberId() { return memberId; }
    public String getMemberName() { return memberName; }
    public Boolean getAutoMapped() { return autoMapped; }
}
