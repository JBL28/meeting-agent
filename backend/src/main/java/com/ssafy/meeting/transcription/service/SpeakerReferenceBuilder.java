package com.ssafy.meeting.transcription.service;

import com.ssafy.meeting.meeting.domain.MeetingParticipant;
import com.ssafy.meeting.storage.StorageService;
import com.ssafy.meeting.transcription.openai.SpeakerReference;
import com.ssafy.meeting.voice.domain.VoiceSample;
import com.ssafy.meeting.voice.repository.VoiceSampleRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class SpeakerReferenceBuilder {

    private final VoiceSampleRepository voiceSampleRepository;
    private final StorageService storageService;

    public SpeakerReferenceBuilder(VoiceSampleRepository voiceSampleRepository, StorageService storageService) {
        this.voiceSampleRepository = voiceSampleRepository;
        this.storageService = storageService;
    }

    public List<SpeakerReference> build(Long teamId, Long meetingId, List<MeetingParticipant> participants) {
        if (participants.size() > 4) {
            log.warn("STT known speaker references skipped meetingId={} participantCount={}", meetingId, participants.size());
            return new ArrayList<>();
        }
        List<SpeakerReference> references = new ArrayList<>();
        for (MeetingParticipant participant : participants) {
            voiceSampleRepository.findFirstByTeamIdAndMemberIdOrderByCreatedAtDesc(teamId, participant.getMember().getId())
                .ifPresent(sample -> addReference(references, participant, sample));
        }
        return references;
    }

    private void addReference(List<SpeakerReference> references, MeetingParticipant participant, VoiceSample sample) {
        Resource resource = storageService.load(sample.getFilePath());
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = readAllBytes(inputStream);
            String dataUrl = "data:" + mimeType(sample.getFileName()) + ";base64," + Base64.getEncoder().encodeToString(bytes);
            references.add(new SpeakerReference(speakerName(participant), dataUrl));
        } catch (IOException exception) {
            log.warn("Voice sample reference could not be read sampleId={} reason={}", sample.getId(), exception.getMessage());
        }
    }

    private String speakerName(MeetingParticipant participant) {
        String name = participant.getMember().getName();
        if (!StringUtils.hasText(name)) {
            name = "member_" + participant.getMember().getId();
        }
        return name.trim().replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private String mimeType(String fileName) {
        String lower = fileName == null ? "" : fileName.toLowerCase();
        if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (lower.endsWith(".wav")) {
            return "audio/wav";
        }
        return "audio/webm";
    }

    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int read;
        while ((read = inputStream.read(data)) != -1) {
            buffer.write(data, 0, read);
        }
        return buffer.toByteArray();
    }
}
