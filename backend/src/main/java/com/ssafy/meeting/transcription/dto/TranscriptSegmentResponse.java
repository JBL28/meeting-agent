package com.ssafy.meeting.transcription.dto;

import com.ssafy.meeting.transcription.domain.TranscriptSegment;

public class TranscriptSegmentResponse {
    private final Long id;
    private final String speaker;
    private final Long memberId;
    private final Double startTime;
    private final Double endTime;
    private final String text;
    private final Integer sequence;

    private TranscriptSegmentResponse(TranscriptSegment segment) {
        this.id = segment.getId();
        this.speaker = segment.getSpeaker();
        this.memberId = segment.getMember() == null ? null : segment.getMember().getId();
        this.startTime = segment.getStartTime();
        this.endTime = segment.getEndTime();
        this.text = segment.getText();
        this.sequence = segment.getSequence();
    }

    public static TranscriptSegmentResponse from(TranscriptSegment segment) {
        return new TranscriptSegmentResponse(segment);
    }

    public Long getId() { return id; }
    public String getSpeaker() { return speaker; }
    public Long getMemberId() { return memberId; }
    public Double getStartTime() { return startTime; }
    public Double getEndTime() { return endTime; }
    public String getText() { return text; }
    public Integer getSequence() { return sequence; }
}
