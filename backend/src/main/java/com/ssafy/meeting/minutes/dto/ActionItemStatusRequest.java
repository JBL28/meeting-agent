package com.ssafy.meeting.minutes.dto;

import com.ssafy.meeting.minutes.domain.ActionItemStatus;
import javax.validation.constraints.NotNull;

public class ActionItemStatusRequest {
    @NotNull
    private ActionItemStatus status;

    public ActionItemStatus getStatus() { return status; }
    public void setStatus(ActionItemStatus status) { this.status = status; }
}
