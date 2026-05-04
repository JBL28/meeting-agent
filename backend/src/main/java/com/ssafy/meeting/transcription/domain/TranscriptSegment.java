package com.ssafy.meeting.transcription.domain;

import com.ssafy.meeting.member.domain.Member;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "transcript_segments")
public class TranscriptSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transcription_job_id", nullable = false)
    private TranscriptionJob transcriptionJob;

    @Column(nullable = false, length = 100)
    private String speaker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "start_time", nullable = false)
    private Double startTime;

    @Column(name = "end_time", nullable = false)
    private Double endTime;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(nullable = false)
    private Integer sequence;

    protected TranscriptSegment() {
    }

    public TranscriptSegment(TranscriptionJob transcriptionJob, String speaker, Double startTime, Double endTime, String text, Integer sequence) {
        this.transcriptionJob = transcriptionJob;
        this.speaker = speaker;
        this.startTime = startTime;
        this.endTime = endTime;
        this.text = text;
        this.sequence = sequence;
    }

    public void mapMember(Member member) {
        this.member = member;
    }

    public Long getId() { return id; }
    public TranscriptionJob getTranscriptionJob() { return transcriptionJob; }
    public String getSpeaker() { return speaker; }
    public Member getMember() { return member; }
    public Double getStartTime() { return startTime; }
    public Double getEndTime() { return endTime; }
    public String getText() { return text; }
    public Integer getSequence() { return sequence; }
}
