package com.ssafy.meeting.transcription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ssafy.meeting.audio.domain.AudioFile;
import com.ssafy.meeting.audio.repository.AudioFileRepository;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.domain.MeetingStatus;
import com.ssafy.meeting.meeting.repository.MeetingParticipantRepository;
import com.ssafy.meeting.meeting.repository.MeetingRepository;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.repository.MemberRepository;
import com.ssafy.meeting.storage.StorageService;
import com.ssafy.meeting.team.domain.Team;
import com.ssafy.meeting.team.domain.TeamMember;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.repository.TeamMemberRepository;
import com.ssafy.meeting.team.repository.TeamRepository;
import com.ssafy.meeting.transcription.domain.TranscriptionJob;
import com.ssafy.meeting.transcription.domain.TranscriptionJobStatus;
import com.ssafy.meeting.transcription.openai.OpenAiTranscriptionClient;
import com.ssafy.meeting.transcription.openai.OpenAiTranscriptionRequest;
import com.ssafy.meeting.transcription.repository.TranscriptSegmentRepository;
import com.ssafy.meeting.transcription.repository.TranscriptionJobRepository;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "storage.base-path=build/test-stt-data")
@ActiveProfiles("test")
class TranscriptionAsyncServiceTest {

    @Autowired
    private TranscriptionAsyncService asyncService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MeetingParticipantRepository participantRepository;

    @Autowired
    private AudioFileRepository audioFileRepository;

    @Autowired
    private TranscriptionJobRepository jobRepository;

    @Autowired
    private TranscriptSegmentRepository segmentRepository;

    @MockBean
    private OpenAiTranscriptionClient openAiClient;

    @MockBean
    private StorageService storageService;

    @Test
    void storesSegmentsAndTransitionsMeetingToTranscribed() throws Exception {
        TestData data = createRecordedMeeting(1);
        TranscriptionJob job = jobRepository.saveAndFlush(new TranscriptionJob(data.meeting, data.audioFile));
        when(storageService.load("audio/test.webm")).thenReturn(new ByteArrayResource(new byte[] {1, 2, 3}));
        when(openAiClient.transcribe(any(OpenAiTranscriptionRequest.class))).thenReturn("{\"text\":\"hello\",\"segments\":[{\"speaker\":\"A\",\"start\":0.0,\"end\":1.5,\"text\":\"hello\"}]}");

        asyncService.process(job.getId());

        TranscriptionJob savedJob = jobRepository.findById(job.getId()).orElseThrow(IllegalStateException::new);
        Meeting savedMeeting = meetingRepository.findById(data.meeting.getId()).orElseThrow(IllegalStateException::new);
        assertThat(savedJob.getStatus()).isEqualTo(TranscriptionJobStatus.COMPLETED);
        assertThat(savedJob.getRawResponsePath()).isEqualTo("stt-raw/" + job.getId() + ".json");
        assertThat(Files.exists(Paths.get("build/test-stt-data").resolve(savedJob.getRawResponsePath()))).isTrue();
        assertThat(savedMeeting.getStatus()).isEqualTo(MeetingStatus.TRANSCRIBED);
        assertThat(segmentRepository.findAllByTranscriptionJobIdOrderBySequenceAsc(job.getId()))
            .singleElement()
            .satisfies(segment -> {
                assertThat(segment.getSpeaker()).isEqualTo("A");
                assertThat(segment.getStartTime()).isEqualTo(0.0);
                assertThat(segment.getEndTime()).isEqualTo(1.5);
                assertThat(segment.getText()).isEqualTo("hello");
            });
    }

    @Test
    void marksJobFailedAndMeetingRecordedWhenOpenAiFails() {
        TestData data = createRecordedMeeting(1);
        TranscriptionJob job = jobRepository.saveAndFlush(new TranscriptionJob(data.meeting, data.audioFile));
        when(storageService.load("audio/test.webm")).thenReturn(new ByteArrayResource(new byte[] {1, 2, 3}));
        when(openAiClient.transcribe(any(OpenAiTranscriptionRequest.class))).thenThrow(new IllegalStateException("boom"));

        asyncService.process(job.getId());

        TranscriptionJob savedJob = jobRepository.findById(job.getId()).orElseThrow(IllegalStateException::new);
        Meeting savedMeeting = meetingRepository.findById(data.meeting.getId()).orElseThrow(IllegalStateException::new);
        assertThat(savedJob.getStatus()).isEqualTo(TranscriptionJobStatus.FAILED);
        assertThat(savedJob.getRawResponsePath()).isEqualTo("stt-raw/" + job.getId() + ".error.json");
        assertThat(savedJob.getErrorMessage()).contains("boom");
        assertThat(savedMeeting.getStatus()).isEqualTo(MeetingStatus.RECORDED);
    }

    @Test
    void skipsKnownSpeakerReferencesWhenParticipantCountExceedsFour() {
        TestData data = createRecordedMeeting(5);
        TranscriptionJob job = jobRepository.saveAndFlush(new TranscriptionJob(data.meeting, data.audioFile));
        when(storageService.load("audio/test.webm")).thenReturn(new ByteArrayResource(new byte[] {1, 2, 3}));
        when(openAiClient.transcribe(any(OpenAiTranscriptionRequest.class))).thenReturn("{\"text\":\"ok\",\"segments\":[]}");

        asyncService.process(job.getId());

        ArgumentCaptor<OpenAiTranscriptionRequest> captor = ArgumentCaptor.forClass(OpenAiTranscriptionRequest.class);
        verify(openAiClient).transcribe(captor.capture());
        assertThat(captor.getValue().getSpeakerReferences()).isEmpty();
    }

    private TestData createRecordedMeeting(int participantCount) {
        String suffix = String.valueOf(System.nanoTime());
        Member owner = memberRepository.save(new Member("stt-owner-" + suffix + "@example.com", "password", "Owner"));
        Team team = teamRepository.save(new Team("STT Team " + suffix, owner));
        teamMemberRepository.save(new TeamMember(team, owner, TeamRole.OWNER));
        Meeting meeting = new Meeting(team, "STT Meeting", null, owner);
        meeting.markRecorded();
        meeting = meetingRepository.save(meeting);
        participantRepository.save(new com.ssafy.meeting.meeting.domain.MeetingParticipant(meeting, owner));
        for (int i = 1; i < participantCount; i++) {
            Member member = memberRepository.save(new Member("stt-member-" + suffix + "-" + i + "@example.com", "password", "Member" + i));
            teamMemberRepository.save(new TeamMember(team, member, TeamRole.MEMBER));
            participantRepository.save(new com.ssafy.meeting.meeting.domain.MeetingParticipant(meeting, member));
        }
        AudioFile audioFile = audioFileRepository.save(new AudioFile(meeting, "audio/test.webm", "test.webm", 3L, 1));
        return new TestData(meeting, audioFile);
    }

    private static class TestData {
        private final Meeting meeting;
        private final AudioFile audioFile;

        private TestData(Meeting meeting, AudioFile audioFile) {
            this.meeting = meeting;
            this.audioFile = audioFile;
        }
    }
}
