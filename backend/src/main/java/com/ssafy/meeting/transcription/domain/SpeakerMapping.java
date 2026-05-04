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
import javax.persistence.UniqueConstraint;

@Entity
@Table(
    name = "speaker_mappings",
    uniqueConstraints = @UniqueConstraint(name = "uk_speaker_mappings_job_speaker", columnNames = {"transcription_job_id", "speaker"})
)
public class SpeakerMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transcription_job_id", nullable = false)
    private TranscriptionJob transcriptionJob;

    @Column(nullable = false, length = 100)
    private String speaker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "is_auto_mapped", nullable = false)
    private Boolean autoMapped;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private Member confirmedBy;

    protected SpeakerMapping() {
    }

    public SpeakerMapping(TranscriptionJob transcriptionJob, String speaker, Member member, Boolean autoMapped, Member confirmedBy) {
        this.transcriptionJob = transcriptionJob;
        this.speaker = speaker;
        this.member = member;
        this.autoMapped = autoMapped;
        this.confirmedBy = confirmedBy;
    }

    public void update(Member member, Boolean autoMapped, Member confirmedBy) {
        this.member = member;
        this.autoMapped = autoMapped;
        this.confirmedBy = confirmedBy;
    }

    public Long getId() { return id; }
    public TranscriptionJob getTranscriptionJob() { return transcriptionJob; }
    public String getSpeaker() { return speaker; }
    public Member getMember() { return member; }
    public Boolean getAutoMapped() { return autoMapped; }
    public Member getConfirmedBy() { return confirmedBy; }
}
