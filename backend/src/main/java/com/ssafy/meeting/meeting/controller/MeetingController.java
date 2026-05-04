package com.ssafy.meeting.meeting.controller;

import com.ssafy.meeting.auth.security.UserPrincipal;
import com.ssafy.meeting.common.api.ApiResponse;
import com.ssafy.meeting.meeting.dto.MeetingCreateRequest;
import com.ssafy.meeting.meeting.dto.MeetingParticipantRequest;
import com.ssafy.meeting.meeting.dto.MeetingParticipantResponse;
import com.ssafy.meeting.meeting.dto.MeetingResponse;
import com.ssafy.meeting.meeting.service.MeetingService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping("/api/teams/{teamId}/meetings")
    public ApiResponse<MeetingResponse> create(
        @PathVariable Long teamId,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody MeetingCreateRequest request
    ) {
        return ApiResponse.success(meetingService.create(teamId, principal.getId(), request));
    }

    @GetMapping("/api/teams/{teamId}/meetings")
    public ApiResponse<List<MeetingResponse>> list(@PathVariable Long teamId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(meetingService.list(teamId, principal.getId()));
    }

    @GetMapping("/api/meetings/{meetingId}")
    public ApiResponse<MeetingResponse> get(@PathVariable Long meetingId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(meetingService.get(meetingId, principal.getId()));
    }

    @PostMapping("/api/meetings/{meetingId}/recording")
    public ApiResponse<MeetingResponse> markRecording(@PathVariable Long meetingId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(meetingService.markRecording(meetingId, principal.getId()));
    }

    @PostMapping("/api/meetings/{meetingId}/participants")
    public ApiResponse<MeetingParticipantResponse> addParticipant(
        @PathVariable Long meetingId,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody MeetingParticipantRequest request
    ) {
        return ApiResponse.success(meetingService.addParticipant(meetingId, principal.getId(), request));
    }
}
