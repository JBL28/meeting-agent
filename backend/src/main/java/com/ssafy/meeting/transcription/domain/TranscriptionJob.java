package com.ssafy.meeting.transcription.domain;

import com.ssafy.meeting.audio.domain.AudioFile;
import com.ssafy.meeting.meeting.domain.Meeting;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

@Entity
@Table(name = "transcription_jobs")
public class TranscriptionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "audio_file_id", nullable = false)
    private AudioFile audioFile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TranscriptionJobStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "raw_response_path", length = 500)
    private String rawResponsePath;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TranscriptionJob() {
    }

    public TranscriptionJob(Meeting meeting, AudioFile audioFile) {
        this.meeting = meeting;
        this.audioFile = audioFile;
        this.status = TranscriptionJobStatus.CREATED;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void markProcessing() {
        this.status = TranscriptionJobStatus.PROCESSING;
        this.startedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markCompleted(String rawResponsePath) {
        this.status = TranscriptionJobStatus.COMPLETED;
        this.rawResponsePath = rawResponsePath;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage, String rawResponsePath) {
        this.status = TranscriptionJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.rawResponsePath = rawResponsePath;
        this.completedAt = LocalDateTime.now();
    }

    public void markCanceled() {
        this.status = TranscriptionJobStatus.CANCELED;
        this.completedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Meeting getMeeting() { return meeting; }
    public AudioFile getAudioFile() { return audioFile; }
    public TranscriptionJobStatus getStatus() { return status; }
    public String getErrorMessage() { return errorMessage; }
    public String getRawResponsePath() { return rawResponsePath; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
