package com.ssafy.meeting.meeting.dto;

import com.ssafy.meeting.meeting.domain.MeetingParticipant;

public class MeetingParticipantResponse {
    private final Long memberId;
    private final String email;
    private final String name;

    public MeetingParticipantResponse(Long memberId, String email, String name) {
        this.memberId = memberId;
        this.email = email;
        this.name = name;
    }

    public static MeetingParticipantResponse from(MeetingParticipant participant) {
        return new MeetingParticipantResponse(participant.getMember().getId(), participant.getMember().getEmail(), participant.getMember().getName());
    }

    public Long getMemberId() { return memberId; }
    public String getEmail() { return email; }
    public String getName() { return name; }
}
