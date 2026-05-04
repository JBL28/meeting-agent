package com.ssafy.meeting.audio.domain;

import com.ssafy.meeting.meeting.domain.Meeting;
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
@Table(name = "audio_files")
public class AudioFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    protected AudioFile() {
    }

    public AudioFile(Meeting meeting, String filePath, String fileName, Long fileSize, Integer durationSeconds) {
        this.meeting = meeting;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.durationSeconds = durationSeconds;
    }

    @PrePersist
    void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Meeting getMeeting() { return meeting; }
    public String getFilePath() { return filePath; }
    public String getFileName() { return fileName; }
    public Long getFileSize() { return fileSize; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
