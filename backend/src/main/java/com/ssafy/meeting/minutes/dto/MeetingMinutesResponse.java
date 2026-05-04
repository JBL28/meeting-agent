package com.ssafy.meeting.minutes.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meeting.minutes.domain.MeetingMinutes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MeetingMinutesResponse {
    private final Long id;
    private final Long meetingId;
    private final Long generationJobId;
    private final String title;
    private final LocalDate meetingDate;
    private final String fullSummary;
    private final List<String> decisions;
    private final List<MemberMinutesSummaryResponse> memberSummaries;
    private final List<ActionItemResponse> actionItems;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public MeetingMinutesResponse(
        MeetingMinutes minutes,
        List<MemberMinutesSummaryResponse> memberSummaries,
        List<ActionItemResponse> actionItems,
        ObjectMapper objectMapper
    ) {
        this.id = minutes.getId();
        this.meetingId = minutes.getMeeting().getId();
        this.generationJobId = minutes.getGenerationJob().getId();
        this.title = minutes.getTitle();
        this.meetingDate = minutes.getMeetingDate();
        this.fullSummary = minutes.getFullSummary();
        this.decisions = decisions(minutes.getRawContent(), objectMapper);
        this.memberSummaries = memberSummaries;
        this.actionItems = actionItems;
        this.createdAt = minutes.getCreatedAt();
        this.updatedAt = minutes.getUpdatedAt();
    }

    private List<String> decisions(String rawContent, ObjectMapper objectMapper) {
        List<String> values = new ArrayList<>();
        try {
            JsonNode decisions = objectMapper.readTree(rawContent).path("decisions");
            if (decisions.isArray()) {
                for (JsonNode decision : decisions) {
                    values.add(decision.asText());
                }
            }
        } catch (Exception ignored) {
            return values;
        }
        return values;
    }

    public Long getId() { return id; }
    public Long getMeetingId() { return meetingId; }
    public Long getGenerationJobId() { return generationJobId; }
    public String getTitle() { return title; }
    public LocalDate getMeetingDate() { return meetingDate; }
    public String getFullSummary() { return fullSummary; }
    public List<String> getDecisions() { return decisions; }
    public List<MemberMinutesSummaryResponse> getMemberSummaries() { return memberSummaries; }
    public List<ActionItemResponse> getActionItems() { return actionItems; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
