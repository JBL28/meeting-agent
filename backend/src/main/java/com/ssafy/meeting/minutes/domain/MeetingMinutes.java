package com.ssafy.meeting.minutes.domain;

import com.ssafy.meeting.meeting.domain.Meeting;
import java.time.LocalDate;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

@Entity
@Table(name = "meeting_minutes")
public class MeetingMinutes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false, unique = true)
    private Meeting meeting;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "generation_job_id", nullable = false)
    private MinutesGenerationJob generationJob;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(name = "meeting_date")
    private LocalDate meetingDate;

    @Column(name = "full_summary", nullable = false, columnDefinition = "TEXT")
    private String fullSummary;

    @Column(name = "raw_content", nullable = false, columnDefinition = "LONGTEXT")
    private String rawContent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected MeetingMinutes() {
    }

    public MeetingMinutes(Meeting meeting, MinutesGenerationJob generationJob, String title, LocalDate meetingDate, String fullSummary, String rawContent) {
        this.meeting = meeting;
        this.generationJob = generationJob;
        this.title = title;
        this.meetingDate = meetingDate;
        this.fullSummary = fullSummary;
        this.rawContent = rawContent;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void update(String title, String fullSummary, String rawContent) {
        this.title = title;
        this.fullSummary = fullSummary;
        this.rawContent = rawContent;
    }

    public Long getId() { return id; }
    public Meeting getMeeting() { return meeting; }
    public MinutesGenerationJob getGenerationJob() { return generationJob; }
    public String getTitle() { return title; }
    public LocalDate getMeetingDate() { return meetingDate; }
    public String getFullSummary() { return fullSummary; }
    public String getRawContent() { return rawContent; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
