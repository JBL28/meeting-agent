package com.ssafy.meeting.audio.dto;

import com.ssafy.meeting.audio.domain.AudioFile;
import java.time.LocalDateTime;

public class AudioFileResponse {
    private final Long id;
    private final Long meetingId;
    private final String fileName;
    private final Long fileSize;
    private final Integer durationSeconds;
    private final LocalDateTime uploadedAt;

    public AudioFileResponse(Long id, Long meetingId, String fileName, Long fileSize, Integer durationSeconds, LocalDateTime uploadedAt) {
        this.id = id;
        this.meetingId = meetingId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.durationSeconds = durationSeconds;
        this.uploadedAt = uploadedAt;
    }

    public static AudioFileResponse from(AudioFile audioFile) {
        return new AudioFileResponse(
            audioFile.getId(),
            audioFile.getMeeting().getId(),
            audioFile.getFileName(),
            audioFile.getFileSize(),
            audioFile.getDurationSeconds(),
            audioFile.getUploadedAt()
        );
    }

    public Long getId() { return id; }
    public Long getMeetingId() { return meetingId; }
    public String getFileName() { return fileName; }
    public Long getFileSize() { return fileSize; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
