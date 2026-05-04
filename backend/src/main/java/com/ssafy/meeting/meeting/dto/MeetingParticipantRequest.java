package com.ssafy.meeting.meeting.dto;

import javax.validation.constraints.NotNull;

public class MeetingParticipantRequest {
    @NotNull
    private Long memberId;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
}
