package com.ssafy.meeting.audio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.meeting.audio.domain.AudioFile;
import com.ssafy.meeting.audio.repository.AudioFileRepository;
import com.ssafy.meeting.common.exception.ValidationException;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.domain.MeetingStatus;
import com.ssafy.meeting.meeting.repository.MeetingRepository;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.storage.AudioDurationProbe;
import com.ssafy.meeting.storage.FilePolicyValidator;
import com.ssafy.meeting.storage.StorageService;
import com.ssafy.meeting.team.domain.Team;
import com.ssafy.meeting.team.service.TeamPermissionEvaluator;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

class AudioFileServiceTest {

    private final AudioFileRepository audioFileRepository = mock(AudioFileRepository.class);
    private final MeetingRepository meetingRepository = mock(MeetingRepository.class);
    private final TeamPermissionEvaluator permissionEvaluator = mock(TeamPermissionEvaluator.class);
    private final StorageService storageService = mock(StorageService.class);
    private final FilePolicyValidator filePolicyValidator = new FilePolicyValidator();
    private final AudioDurationProbe audioDurationProbe = mock(AudioDurationProbe.class);
    private final AudioFileService audioFileService = new AudioFileService(
        audioFileRepository,
        meetingRepository,
        permissionEvaluator,
        storageService,
        filePolicyValidator,
        audioDurationProbe
    );

    @Test
    void uploadMeetingAudioStoresFileAndMarksMeetingRecorded() throws Exception {
        Team team = new Team("Team", new Member("owner@example.com", "pw", "Owner"));
        ReflectionTestUtils.setField(team, "id", 1L);
        Member creator = new Member("creator@example.com", "pw", "Creator");
        ReflectionTestUtils.setField(creator, "id", 10L);
        Meeting meeting = new Meeting(team, "Weekly", null, creator);
        ReflectionTestUtils.setField(meeting, "id", 100L);
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getOriginalFilename()).thenReturn("meeting.mp3");
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));
        when(storageService.save(file, "meetings")).thenReturn("meetings/2026/05/audio.mp3");
        when(audioDurationProbe.probeSeconds("meetings/2026/05/audio.mp3")).thenReturn(OptionalInt.empty());
        when(audioFileRepository.save(any(AudioFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        audioFileService.uploadMeetingAudio(100L, 10L, file);

        assertThat(meeting.getStatus()).isEqualTo(MeetingStatus.RECORDED);
        verify(audioFileRepository).save(any(AudioFile.class));
    }

    @Test
    void uploadMeetingAudioRejectsOverHundredMegabytes() {
        Team team = new Team("Team", new Member("owner@example.com", "pw", "Owner"));
        ReflectionTestUtils.setField(team, "id", 1L);
        Member creator = new Member("creator@example.com", "pw", "Creator");
        ReflectionTestUtils.setField(creator, "id", 10L);
        Meeting meeting = new Meeting(team, "Weekly", null, creator);
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(100L * 1024L * 1024L + 1L);
        when(file.getOriginalFilename()).thenReturn("meeting.mp3");
        when(meetingRepository.findById(100L)).thenReturn(Optional.of(meeting));

        assertThatThrownBy(() -> audioFileService.uploadMeetingAudio(100L, 10L, file))
            .isInstanceOf(ValidationException.class);

        verify(storageService, never()).save(file, "meetings");
    }
}
