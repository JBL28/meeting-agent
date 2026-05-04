package com.ssafy.meeting.minutes.dto;

import com.ssafy.meeting.minutes.domain.ActionItem;
import com.ssafy.meeting.minutes.domain.ActionItemStatus;
import java.time.LocalDate;

public class ActionItemResponse {
    private final Long id;
    private final Long assigneeId;
    private final String assigneeName;
    private final String content;
    private final LocalDate dueDate;
    private final ActionItemStatus status;

    private ActionItemResponse(ActionItem item) {
        this.id = item.getId();
        this.assigneeId = item.getAssignee() == null ? null : item.getAssignee().getId();
        this.assigneeName = item.getAssignee() == null ? null : item.getAssignee().getName();
        this.content = item.getContent();
        this.dueDate = item.getDueDate();
        this.status = item.getStatus();
    }

    public static ActionItemResponse from(ActionItem item) {
        return new ActionItemResponse(item);
    }

    public Long getId() { return id; }
    public Long getAssigneeId() { return assigneeId; }
    public String getAssigneeName() { return assigneeName; }
    public String getContent() { return content; }
    public LocalDate getDueDate() { return dueDate; }
    public ActionItemStatus getStatus() { return status; }
}
