package com.ssafy.meeting.audio.service;

import com.ssafy.meeting.audio.domain.AudioFile;
import com.ssafy.meeting.audio.dto.AudioFileResponse;
import com.ssafy.meeting.audio.repository.AudioFileRepository;
import com.ssafy.meeting.common.exception.ResourceNotFoundException;
import com.ssafy.meeting.common.exception.ValidationException;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.repository.MeetingRepository;
import com.ssafy.meeting.storage.AudioDurationProbe;
import com.ssafy.meeting.storage.FilePolicyValidator;
import com.ssafy.meeting.storage.StorageService;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.service.TeamPermissionEvaluator;
import java.util.OptionalInt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class AudioFileService {

    private final AudioFileRepository audioFileRepository;
    private final MeetingRepository meetingRepository;
    private final TeamPermissionEvaluator permissionEvaluator;
    private final StorageService storageService;
    private final FilePolicyValidator filePolicyValidator;
    private final AudioDurationProbe audioDurationProbe;

    public AudioFileService(
        AudioFileRepository audioFileRepository,
        MeetingRepository meetingRepository,
        TeamPermissionEvaluator permissionEvaluator,
        StorageService storageService,
        FilePolicyValidator filePolicyValidator,
        AudioDurationProbe audioDurationProbe
    ) {
        this.audioFileRepository = audioFileRepository;
        this.meetingRepository = meetingRepository;
        this.permissionEvaluator = permissionEvaluator;
        this.storageService = storageService;
        this.filePolicyValidator = filePolicyValidator;
        this.audioDurationProbe = audioDurationProbe;
    }

    @Transactional
    public AudioFileResponse uploadMeetingAudio(Long meetingId, Long requesterId, MultipartFile file) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        permissionEvaluator.requireRole(meeting.getTeam().getId(), requesterId, TeamRole.MEMBER);
        filePolicyValidator.validateMeetingAudio(file);

        String filePath = storageService.save(file, "meetings");
        Integer durationSeconds = null;
        try {
            OptionalInt duration = audioDurationProbe.probeSeconds(filePath);
            if (duration.isPresent()) {
                durationSeconds = duration.getAsInt();
                if (durationSeconds > 3600) {
                    throw new ValidationException("Meeting audio duration exceeds 60 minutes");
                }
                log.info("Meeting audio duration validated meetingId={} seconds={}", meetingId, durationSeconds);
            } else {
                log.warn("Meeting audio duration skipped because ffprobe is unavailable meetingId={} fileName={}", meetingId, file.getOriginalFilename());
            }
            meeting.markRecorded();
            AudioFile audioFile = audioFileRepository.save(new AudioFile(meeting, filePath, file.getOriginalFilename(), file.getSize(), durationSeconds));
            log.info("Meeting audio uploaded audioFileId={} meetingId={} requesterId={}", audioFile.getId(), meetingId, requesterId);
            return AudioFileResponse.from(audioFile);
        } catch (RuntimeException exception) {
            storageService.delete(filePath);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Resource stream(Long audioFileId, Long requesterId) {
        AudioFile audioFile = audioFileRepository.findWithMeetingById(audioFileId)
            .orElseThrow(() -> new ResourceNotFoundException("Audio file not found"));
        permissionEvaluator.requireRole(audioFile.getMeeting().getTeam().getId(), requesterId, TeamRole.VIEWER);
        log.info("Meeting audio stream requested audioFileId={} requesterId={}", audioFileId, requesterId);
        return storageService.load(audioFile.getFilePath());
    }
}
