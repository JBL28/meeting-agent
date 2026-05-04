package com.ssafy.meeting.minutes.controller;

import com.ssafy.meeting.auth.security.UserPrincipal;
import com.ssafy.meeting.common.api.ApiResponse;
import com.ssafy.meeting.minutes.dto.ActionItemResponse;
import com.ssafy.meeting.minutes.dto.ActionItemStatusRequest;
import com.ssafy.meeting.minutes.dto.ActionItemUpdateRequest;
import com.ssafy.meeting.minutes.dto.MeetingMinutesResponse;
import com.ssafy.meeting.minutes.dto.MeetingMinutesUpdateRequest;
import com.ssafy.meeting.minutes.dto.MinutesGenerationJobResponse;
import com.ssafy.meeting.minutes.dto.MinutesGenerationJobStartResponse;
import com.ssafy.meeting.minutes.service.MinutesGenerationService;
import com.ssafy.meeting.transcription.dto.TranscriptSegmentResponse;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MinutesController {

    private final MinutesGenerationService minutesService;

    public MinutesController(MinutesGenerationService minutesService) {
        this.minutesService = minutesService;
    }

    @PostMapping("/api/meetings/{meetingId}/minutes/generate")
    public ResponseEntity<ApiResponse<MinutesGenerationJobStartResponse>> generate(
        @PathVariable Long meetingId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(minutesService.start(meetingId, principal.getId())));
    }

    @GetMapping("/api/minutes-generation-jobs/{jobId}")
    public ApiResponse<MinutesGenerationJobResponse> getJob(@PathVariable Long jobId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(minutesService.getJob(jobId, principal.getId()));
    }

    @GetMapping("/api/meetings/{meetingId}/minutes")
    public ApiResponse<MeetingMinutesResponse> getMinutes(@PathVariable Long meetingId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(minutesService.getMinutes(meetingId, principal.getId()));
    }

    @GetMapping("/api/meetings/{meetingId}/minutes/segments")
    public ApiResponse<List<TranscriptSegmentResponse>> originalSegments(@PathVariable Long meetingId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(minutesService.originalSegments(meetingId, principal.getId()));
    }

    @PutMapping("/api/meetings/{meetingId}/minutes")
    public ApiResponse<MeetingMinutesResponse> updateMinutes(
        @PathVariable Long meetingId,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody MeetingMinutesUpdateRequest request
    ) {
        return ApiResponse.success(minutesService.updateMinutes(meetingId, principal.getId(), request));
    }

    @DeleteMapping("/api/meetings/{meetingId}/minutes")
    public ApiResponse<Void> deleteMinutes(@PathVariable Long meetingId, @AuthenticationPrincipal UserPrincipal principal) {
        minutesService.deleteMinutes(meetingId, principal.getId());
        return ApiResponse.success(null);
    }

    @PutMapping("/api/action-items/{actionItemId}")
    public ApiResponse<ActionItemResponse> updateActionItem(
        @PathVariable Long actionItemId,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody ActionItemUpdateRequest request
    ) {
        return ApiResponse.success(minutesService.updateActionItem(actionItemId, principal.getId(), request));
    }

    @PutMapping("/api/action-items/{actionItemId}/status")
    public ApiResponse<ActionItemResponse> changeActionStatus(
        @PathVariable Long actionItemId,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody ActionItemStatusRequest request
    ) {
        return ApiResponse.success(minutesService.changeActionStatus(actionItemId, principal.getId(), request));
    }
}
