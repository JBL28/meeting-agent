package com.ssafy.meeting.voice.domain;

import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.team.domain.Team;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "voice_samples")
public class VoiceSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "consent_agreed_at", nullable = false)
    private LocalDateTime consentAgreedAt;

    protected VoiceSample() {
    }

    public VoiceSample(Member member, Team team, String filePath, String fileName, Integer durationSeconds, LocalDateTime consentAgreedAt) {
        this.member = member;
        this.team = team;
        this.filePath = filePath;
        this.fileName = fileName;
        this.durationSeconds = durationSeconds;
        this.consentAgreedAt = consentAgreedAt;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Member getMember() { return member; }
    public Team getTeam() { return team; }
    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getConsentAgreedAt() { return consentAgreedAt; }
}
