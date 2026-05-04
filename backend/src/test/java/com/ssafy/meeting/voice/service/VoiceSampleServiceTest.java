package com.ssafy.meeting.voice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.meeting.common.exception.ValidationException;
import com.ssafy.meeting.member.repository.MemberRepository;
import com.ssafy.meeting.storage.AudioDurationProbe;
import com.ssafy.meeting.storage.FilePolicyValidator;
import com.ssafy.meeting.storage.StorageService;
import com.ssafy.meeting.team.repository.TeamRepository;
import com.ssafy.meeting.team.service.TeamPermissionEvaluator;
import com.ssafy.meeting.voice.repository.VoiceSampleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class VoiceSampleServiceTest {

    private final VoiceSampleRepository voiceSampleRepository = mock(VoiceSampleRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final TeamRepository teamRepository = mock(TeamRepository.class);
    private final TeamPermissionEvaluator permissionEvaluator = mock(TeamPermissionEvaluator.class);
    private final StorageService storageService = mock(StorageService.class);
    private final FilePolicyValidator filePolicyValidator = new FilePolicyValidator();
    private final AudioDurationProbe audioDurationProbe = mock(AudioDurationProbe.class);
    private final VoiceSampleService voiceSampleService = new VoiceSampleService(
        voiceSampleRepository,
        memberRepository,
        teamRepository,
        permissionEvaluator,
        storageService,
        filePolicyValidator,
        audioDurationProbe
    );

    @Test
    void uploadRejectsMissingConsentBeforeStorage() {
        MultipartFile file = mock(MultipartFile.class);

        assertThatThrownBy(() -> voiceSampleService.upload(1L, 10L, 10L, false, file))
            .isInstanceOf(ValidationException.class);

        verify(storageService, never()).save(file, "voice-samples");
    }

    @Test
    void uploadRejectsVoiceSampleOverTenMegabytes() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L * 1024L * 1024L + 1L);
        when(file.getOriginalFilename()).thenReturn("sample.mp3");

        assertThatThrownBy(() -> voiceSampleService.upload(1L, 10L, 10L, true, file))
            .isInstanceOf(ValidationException.class);

        verify(storageService, never()).save(file, "voice-samples");
    }
}
