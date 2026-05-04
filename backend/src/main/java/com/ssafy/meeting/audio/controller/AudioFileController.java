package com.ssafy.meeting.audio.controller;

import com.ssafy.meeting.audio.dto.AudioFileResponse;
import com.ssafy.meeting.audio.service.AudioFileService;
import com.ssafy.meeting.auth.security.UserPrincipal;
import com.ssafy.meeting.common.api.ApiResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class AudioFileController {

    private final AudioFileService audioFileService;

    public AudioFileController(AudioFileService audioFileService) {
        this.audioFileService = audioFileService;
    }

    @PostMapping("/api/meetings/{meetingId}/audio")
    public ApiResponse<AudioFileResponse> upload(
        @PathVariable Long meetingId,
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ApiResponse.success(audioFileService.uploadMeetingAudio(meetingId, principal.getId(), file));
    }

    @GetMapping("/api/audio/{audioFileId}/stream")
    public ResponseEntity<Resource> stream(@PathVariable Long audioFileId, @AuthenticationPrincipal UserPrincipal principal) {
        Resource resource = audioFileService.stream(audioFileId, principal.getId());
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .body(resource);
    }
}
