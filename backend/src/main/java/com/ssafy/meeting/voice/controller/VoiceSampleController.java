package com.ssafy.meeting.voice.controller;

import com.ssafy.meeting.auth.security.UserPrincipal;
import com.ssafy.meeting.common.api.ApiResponse;
import com.ssafy.meeting.voice.dto.VoiceSampleResponse;
import com.ssafy.meeting.voice.service.VoiceSampleService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class VoiceSampleController {

    private final VoiceSampleService voiceSampleService;

    public VoiceSampleController(VoiceSampleService voiceSampleService) {
        this.voiceSampleService = voiceSampleService;
    }

    @PostMapping("/api/teams/{teamId}/members/{memberId}/voice-samples")
    public ApiResponse<VoiceSampleResponse> upload(
        @PathVariable Long teamId,
        @PathVariable Long memberId,
        @RequestParam("consent") boolean consent,
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(voiceSampleService.upload(teamId, memberId, principal.getId(), consent, file));
    }

    @GetMapping("/api/teams/{teamId}/members/{memberId}/voice-samples")
    public ApiResponse<List<VoiceSampleResponse>> list(
        @PathVariable Long teamId,
        @PathVariable Long memberId,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(voiceSampleService.list(teamId, memberId, principal.getId()));
    }

    @DeleteMapping("/api/voice-samples/{sampleId}")
    public ApiResponse<Void> delete(@PathVariable Long sampleId, @AuthenticationPrincipal UserPrincipal principal) {
        voiceSampleService.delete(sampleId, principal.getId());
        return ApiResponse.success(null);
    }
}
