package com.ssafy.meeting.meeting.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class MeetingCreateRequest {
    @NotBlank
    @Size(max = 300)
    private String title;

    private LocalDateTime scheduledAt;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
}
