package com.ssafy.meeting.minutes.dto;

import javax.validation.constraints.NotBlank;

public class MeetingMinutesUpdateRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String fullSummary;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFullSummary() { return fullSummary; }
    public void setFullSummary(String fullSummary) { this.fullSummary = fullSummary; }
}
