package com.ssafy.meeting.minutes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ssafy.meeting.audio.domain.AudioFile;
import com.ssafy.meeting.audio.repository.AudioFileRepository;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.domain.MeetingParticipant;
import com.ssafy.meeting.meeting.domain.MeetingStatus;
import com.ssafy.meeting.meeting.repository.MeetingParticipantRepository;
import com.ssafy.meeting.meeting.repository.MeetingRepository;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.repository.MemberRepository;
import com.ssafy.meeting.minutes.domain.MinutesGenerationJob;
import com.ssafy.meeting.minutes.domain.MinutesGenerationJobStatus;
import com.ssafy.meeting.minutes.openai.MinutesLlmClient;
import com.ssafy.meeting.minutes.repository.ActionItemRepository;
import com.ssafy.meeting.minutes.repository.MeetingMinutesRepository;
import com.ssafy.meeting.minutes.repository.MemberMinutesSummaryRepository;
import com.ssafy.meeting.minutes.repository.MinutesGenerationJobRepository;
import com.ssafy.meeting.team.domain.Team;
import com.ssafy.meeting.team.domain.TeamMember;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.repository.TeamMemberRepository;
import com.ssafy.meeting.team.repository.TeamRepository;
import com.ssafy.meeting.transcription.domain.TranscriptSegment;
import com.ssafy.meeting.transcription.domain.TranscriptionJob;
import com.ssafy.meeting.transcription.domain.TranscriptionJobStatus;
import com.ssafy.meeting.transcription.repository.TranscriptSegmentRepository;
import com.ssafy.meeting.transcription.repository.TranscriptionJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MinutesGenerationServiceTest {

    @Autowired
    private MinutesGenerationAsyncService asyncService;

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
    private TranscriptionJobRepository transcriptionJobRepository;

    @Autowired
    private TranscriptSegmentRepository segmentRepository;

    @Autowired
    private MinutesGenerationJobRepository jobRepository;

    @Autowired
    private MeetingMinutesRepository minutesRepository;

    @Autowired
    private MemberMinutesSummaryRepository summaryRepository;

    @Autowired
    private ActionItemRepository actionItemRepository;

    @MockBean
    private MinutesLlmClient llmClient;

    @Test
    void createsMinutesSummariesAndActionItemsFromStructuredOutput() {
        TestData data = createTranscribedMeeting();
        MinutesGenerationJob job = jobRepository.saveAndFlush(new MinutesGenerationJob(data.meeting));
        when(llmClient.generate(anyString())).thenReturn("{\"title\":\"Sprint Sync\",\"full_summary\":\"backend search work\",\"member_summaries\":[{\"member_name\":\"Owner\",\"progress\":\"API done\",\"issues\":\"none\",\"next_tasks\":\"QA\"}],\"decisions\":[\"Ship MVP\"],\"action_items\":[{\"assignee\":\"Owner\",\"content\":\"Write docs\",\"due_date\":\"2026-05-10\"}]}");

        asyncService.process(job.getId());

        MinutesGenerationJob savedJob = jobRepository.findById(job.getId()).orElseThrow(IllegalStateException::new);
        Meeting savedMeeting = meetingRepository.findById(data.meeting.getId()).orElseThrow(IllegalStateException::new);
        assertThat(savedJob.getStatus()).isEqualTo(MinutesGenerationJobStatus.COMPLETED);
        assertThat(savedMeeting.getStatus()).isEqualTo(MeetingStatus.MINUTES_GENERATED);
        assertThat(minutesRepository.findByMeetingId(data.meeting.getId())).hasValueSatisfying(minutes -> {
            assertThat(minutes.getTitle()).isEqualTo("Sprint Sync");
            assertThat(minutes.getFullSummary()).contains("backend search");
            assertThat(summaryRepository.findAllByMinutesIdOrderByIdAsc(minutes.getId())).hasSize(1);
            assertThat(actionItemRepository.findAllByMinutesIdOrderByIdAsc(minutes.getId()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getAssignee().getId()).isEqualTo(data.owner.getId());
                    assertThat(item.getContent()).isEqualTo("Write docs");
                });
        });
    }

    @Test
    void marksJobFailedAndDoesNotCreateEmptyMinutesWhenParsingFails() {
        TestData data = createTranscribedMeeting();
        MinutesGenerationJob job = jobRepository.saveAndFlush(new MinutesGenerationJob(data.meeting));
        when(llmClient.generate(anyString())).thenReturn("{\"title\":\"missing arrays\"}");

        asyncService.process(job.getId());

        MinutesGenerationJob savedJob = jobRepository.findById(job.getId()).orElseThrow(IllegalStateException::new);
        Meeting savedMeeting = meetingRepository.findById(data.meeting.getId()).orElseThrow(IllegalStateException::new);
        assertThat(savedJob.getStatus()).isEqualTo(MinutesGenerationJobStatus.FAILED);
        assertThat(savedMeeting.getStatus()).isEqualTo(MeetingStatus.TRANSCRIBED);
        assertThat(minutesRepository.findByMeetingId(data.meeting.getId())).isEmpty();
    }

    private TestData createTranscribedMeeting() {
        String suffix = String.valueOf(System.nanoTime());
        Member owner = memberRepository.save(new Member("minutes-owner-" + suffix + "@example.com", "password", "Owner"));
        Team team = teamRepository.save(new Team("Minutes Team " + suffix, owner));
        teamMemberRepository.save(new TeamMember(team, owner, TeamRole.OWNER));
        Meeting meeting = new Meeting(team, "Minutes Meeting", null, owner);
        meeting.markTranscribed();
        meeting = meetingRepository.save(meeting);
        participantRepository.save(new MeetingParticipant(meeting, owner));
        AudioFile audioFile = audioFileRepository.save(new AudioFile(meeting, "audio/minutes.webm", "minutes.webm", 3L, 1));
        TranscriptionJob transcriptionJob = new TranscriptionJob(meeting, audioFile);
        transcriptionJob.markProcessing();
        transcriptionJob.markCompleted("stt-raw/minutes.json");
        transcriptionJob = transcriptionJobRepository.save(transcriptionJob);
        segmentRepository.save(new TranscriptSegment(transcriptionJob, "A", 0.0, 2.0, "We finished backend search work.", 0));
        return new TestData(owner, meeting);
    }

    private static class TestData {
        private final Member owner;
        private final Meeting meeting;

        private TestData(Member owner, Meeting meeting) {
            this.owner = owner;
            this.meeting = meeting;
        }
    }
}
