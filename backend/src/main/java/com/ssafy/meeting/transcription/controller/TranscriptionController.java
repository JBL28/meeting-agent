package com.ssafy.meeting.transcription.controller;

import com.ssafy.meeting.auth.security.UserPrincipal;
import com.ssafy.meeting.common.api.ApiResponse;
import com.ssafy.meeting.transcription.dto.SpeakerMappingRequest;
import com.ssafy.meeting.transcription.dto.SpeakerMappingResponse;
import com.ssafy.meeting.transcription.dto.TranscriptSegmentResponse;
import com.ssafy.meeting.transcription.dto.TranscriptionJobResponse;
import com.ssafy.meeting.transcription.dto.TranscriptionJobStartResponse;
import com.ssafy.meeting.transcription.service.TranscriptionService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TranscriptionController {

    private final TranscriptionService transcriptionService;

    public TranscriptionController(TranscriptionService transcriptionService) {
        this.transcriptionService = transcriptionService;
    }

    @PostMapping("/api/meetings/{meetingId}/transcription")
    public ResponseEntity<ApiResponse<TranscriptionJobStartResponse>> start(
        @PathVariable Long meetingId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(transcriptionService.start(meetingId, principal.getId())));
    }

    @GetMapping("/api/transcription-jobs/{jobId}")
    public ApiResponse<TranscriptionJobResponse> get(@PathVariable Long jobId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(transcriptionService.get(jobId, principal.getId()));
    }

    @GetMapping("/api/transcription-jobs/{jobId}/segments")
    public ApiResponse<List<TranscriptSegmentResponse>> segments(@PathVariable Long jobId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(transcriptionService.segments(jobId, principal.getId()));
    }

    @PostMapping("/api/transcription-jobs/{jobId}/cancel")
    public ApiResponse<TranscriptionJobResponse> cancel(@PathVariable Long jobId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(transcriptionService.cancel(jobId, principal.getId()));
    }

    @PostMapping("/api/transcription-jobs/{jobId}/retry")
    public ResponseEntity<ApiResponse<TranscriptionJobStartResponse>> retry(
        @PathVariable Long jobId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(transcriptionService.retry(jobId, principal.getId())));
    }

    @PostMapping("/api/transcription-jobs/{jobId}/speaker-mapping")
    public ApiResponse<List<SpeakerMappingResponse>> saveMappings(
        @PathVariable Long jobId,
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody SpeakerMappingRequest request
    ) {
        return ApiResponse.success(transcriptionService.saveMappings(jobId, principal.getId(), request));
    }
}
