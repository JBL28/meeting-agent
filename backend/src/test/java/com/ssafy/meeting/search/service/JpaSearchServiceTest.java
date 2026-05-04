package com.ssafy.meeting.search.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafy.meeting.audio.domain.AudioFile;
import com.ssafy.meeting.audio.repository.AudioFileRepository;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.repository.MeetingRepository;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.repository.MemberRepository;
import com.ssafy.meeting.minutes.domain.ActionItem;
import com.ssafy.meeting.minutes.domain.MeetingMinutes;
import com.ssafy.meeting.minutes.domain.MinutesGenerationJob;
import com.ssafy.meeting.minutes.repository.ActionItemRepository;
import com.ssafy.meeting.minutes.repository.MeetingMinutesRepository;
import com.ssafy.meeting.minutes.repository.MinutesGenerationJobRepository;
import com.ssafy.meeting.search.dto.MeetingSearchResult;
import com.ssafy.meeting.team.domain.Team;
import com.ssafy.meeting.team.domain.TeamMember;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.repository.TeamMemberRepository;
import com.ssafy.meeting.team.repository.TeamRepository;
import com.ssafy.meeting.transcription.domain.TranscriptSegment;
import com.ssafy.meeting.transcription.domain.TranscriptionJob;
import com.ssafy.meeting.transcription.repository.TranscriptSegmentRepository;
import com.ssafy.meeting.transcription.repository.TranscriptionJobRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JpaSearchServiceTest {

    @Autowired
    private JpaSearchService searchService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private MeetingRepository meetingRepository;

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
    private ActionItemRepository actionItemRepository;

    @Test
    void searchesOnlyRequestedTeamAndReturnsSnippet() {
        String suffix = String.valueOf(System.nanoTime());
        Member owner = memberRepository.save(new Member("search-owner-" + suffix + "@example.com", "password", "Owner"));
        Team team = teamRepository.save(new Team("Search Team " + suffix, owner));
        teamMemberRepository.save(new TeamMember(team, owner, TeamRole.OWNER));
        Meeting meeting = meetingRepository.save(new Meeting(team, "Weekly Planning", null, owner));
        AudioFile audioFile = audioFileRepository.save(new AudioFile(meeting, "audio/search.webm", "search.webm", 1L, 1));
        TranscriptionJob transcriptionJob = new TranscriptionJob(meeting, audioFile);
        transcriptionJob.markProcessing();
        transcriptionJob.markCompleted("stt-raw/search.json");
        transcriptionJob = transcriptionJobRepository.save(transcriptionJob);
        segmentRepository.save(new TranscriptSegment(transcriptionJob, "A", 0.0, 1.0, "Discussed vector indexing rollout.", 0));
        MinutesGenerationJob job = jobRepository.save(new MinutesGenerationJob(meeting));
        MeetingMinutes minutes = minutesRepository.save(new MeetingMinutes(meeting, job, "Weekly Planning", null, "Search summary includes indexing.", "{\"decisions\":[]}"));
        actionItemRepository.save(new ActionItem(minutes, owner, "Prepare indexing checklist", null));

        Member otherOwner = memberRepository.save(new Member("other-owner-" + suffix + "@example.com", "password", "Other"));
        Team otherTeam = teamRepository.save(new Team("Other Team " + suffix, otherOwner));
        teamMemberRepository.save(new TeamMember(otherTeam, otherOwner, TeamRole.OWNER));
        meetingRepository.save(new Meeting(otherTeam, "Indexing secret", null, otherOwner));

        Page<MeetingSearchResult> result = searchService.search(team.getId(), owner.getId(), "indexing", PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent())
            .singleElement()
            .satisfies(item -> {
                assertThat(item.getMeetingId()).isEqualTo(meeting.getId());
                assertThat(item.getSnippet()).containsIgnoringCase("indexing");
            });
    }
}
