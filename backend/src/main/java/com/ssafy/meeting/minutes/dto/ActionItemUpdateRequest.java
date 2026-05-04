package com.ssafy.meeting.minutes.dto;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;

public class ActionItemUpdateRequest {
    private Long assigneeId;

    @NotBlank
    private String content;

    private LocalDate dueDate;

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}
