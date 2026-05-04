package com.ssafy.meeting.team.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class TeamCreateRequest {
    @NotBlank
    @Size(max = 200)
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
