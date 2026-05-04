package com.ssafy.meeting.minutes.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meeting.common.exception.ResourceNotFoundException;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.domain.MeetingParticipant;
import com.ssafy.meeting.meeting.repository.MeetingParticipantRepository;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.minutes.domain.ActionItem;
import com.ssafy.meeting.minutes.domain.MeetingMinutes;
import com.ssafy.meeting.minutes.domain.MemberMinutesSummary;
import com.ssafy.meeting.minutes.domain.MinutesGenerationJob;
import com.ssafy.meeting.minutes.openai.MinutesLlmClient;
import com.ssafy.meeting.minutes.repository.ActionItemRepository;
import com.ssafy.meeting.minutes.repository.MeetingMinutesRepository;
import com.ssafy.meeting.minutes.repository.MemberMinutesSummaryRepository;
import com.ssafy.meeting.minutes.repository.MinutesGenerationJobRepository;
import com.ssafy.meeting.transcription.domain.TranscriptSegment;
import com.ssafy.meeting.transcription.domain.TranscriptionJob;
import com.ssafy.meeting.transcription.domain.TranscriptionJobStatus;
import com.ssafy.meeting.transcription.repository.TranscriptSegmentRepository;
import com.ssafy.meeting.transcription.repository.TranscriptionJobRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class MinutesGenerationAsyncService {

    private final MinutesGenerationJobRepository jobRepository;
    private final MeetingMinutesRepository minutesRepository;
    private final MemberMinutesSummaryRepository memberSummaryRepository;
    private final ActionItemRepository actionItemRepository;
    private final MeetingParticipantRepository participantRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final TranscriptionJobRepository transcriptionJobRepository;
    private final MinutesLlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public MinutesGenerationAsyncService(
        MinutesGenerationJobRepository jobRepository,
        MeetingMinutesRepository minutesRepository,
        MemberMinutesSummaryRepository memberSummaryRepository,
        ActionItemRepository actionItemRepository,
        MeetingParticipantRepository participantRepository,
        TranscriptSegmentRepository segmentRepository,
        TranscriptionJobRepository transcriptionJobRepository,
        MinutesLlmClient llmClient,
        ObjectMapper objectMapper,
        TransactionTemplate transactionTemplate
    ) {
        this.jobRepository = jobRepository;
        this.minutesRepository = minutesRepository;
        this.memberSummaryRepository = memberSummaryRepository;
        this.actionItemRepository = actionItemRepository;
        this.participantRepository = participantRepository;
        this.segmentRepository = segmentRepository;
        this.transcriptionJobRepository = transcriptionJobRepository;
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Async("transcriptionExecutor")
    public CompletableFuture<Void> processGeneration(Long jobId) {
        try {
            String transcript = markProcessingAndBuildTranscript(jobId);
            String rawJson = llmClient.generate(transcript);
            markCompleted(jobId, rawJson);
        } catch (Exception exception) {
            failJob(jobId, exception);
        }
        return CompletableFuture.completedFuture(null);
    }

    public void process(Long jobId) {
        processGeneration(jobId).join();
    }

    private String markProcessingAndBuildTranscript(Long jobId) {
        return transactionTemplate.execute(status -> {
            MinutesGenerationJob job = findJob(jobId);
            Meeting meeting = job.getMeeting();
            job.markProcessing();
            log.info("Minutes generation started jobId={} meetingId={}", jobId, meeting.getId());
            List<TranscriptSegment> segments = latestSegments(meeting.getId());
            if (segments.isEmpty()) {
                throw new IllegalStateException("Transcript segments are required for minutes generation");
            }
            StringBuilder builder = new StringBuilder();
            builder.append("Meeting title: ").append(meeting.getTitle()).append('\n');
            builder.append("Transcript:\n");
            for (TranscriptSegment segment : segments) {
                String speaker = segment.getMember() == null ? segment.getSpeaker() : segment.getMember().getName();
                builder.append('[')
                    .append(segment.getStartTime()).append('-').append(segment.getEndTime()).append("] ")
                    .append(speaker).append(": ")
                    .append(segment.getText()).append('\n');
            }
            return builder.toString();
        });
    }

    private void markCompleted(Long jobId, String rawJson) {
        transactionTemplate.executeWithoutResult(status -> {
            MinutesGenerationJob job = findJob(jobId);
            JsonNode root = parseMinutes(rawJson);
            Meeting meeting = job.getMeeting();
            MeetingMinutes minutes = minutesRepository.findByMeetingId(meeting.getId())
                .orElse(new MeetingMinutes(meeting, job, title(root), meetingDate(meeting), fullSummary(root), rawJson));
            if (minutes.getId() != null) {
                actionItemRepository.deleteAllByMinutesId(minutes.getId());
                memberSummaryRepository.deleteAllByMinutesId(minutes.getId());
                minutes.update(title(root), fullSummary(root), rawJson);
            }
            minutes = minutesRepository.save(minutes);
            Map<String, Member> membersByName = membersByName(meeting.getId());
            saveMemberSummaries(minutes, root, membersByName);
            saveActionItems(minutes, root, membersByName);
            job.markCompleted();
            meeting.markMinutesGenerated();
            log.info("Minutes generation completed jobId={} meetingId={} minutesId={}", jobId, meeting.getId(), minutes.getId());
        });
    }

    private void failJob(Long jobId, Exception exception) {
        transactionTemplate.executeWithoutResult(status -> {
            MinutesGenerationJob job = findJob(jobId);
            job.markFailed(exception.getMessage());
            log.error("Minutes generation failed jobId={} error={}", jobId, exception.getMessage(), exception);
        });
    }

    private List<TranscriptSegment> latestSegments(Long meetingId) {
        TranscriptionJob job = transcriptionJobRepository.findFirstByMeetingIdAndStatusOrderByCreatedAtDesc(meetingId, TranscriptionJobStatus.COMPLETED)
            .orElseThrow(() -> new IllegalStateException("Completed transcription job is required for minutes generation"));
        return segmentRepository.findAllByTranscriptionJobIdOrderBySequenceAsc(job.getId());
    }

    private JsonNode parseMinutes(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            requireText(root, "title");
            requireText(root, "full_summary");
            if (!root.path("member_summaries").isArray() || !root.path("decisions").isArray() || !root.path("action_items").isArray()) {
                throw new IllegalStateException("Minutes response arrays are missing");
            }
            return root;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not parse structured minutes response", exception);
        }
    }

    private void saveMemberSummaries(MeetingMinutes minutes, JsonNode root, Map<String, Member> membersByName) {
        for (JsonNode node : root.path("member_summaries")) {
            Member member = membersByName.get(normalize(node.path("member_name").asText()));
            if (member != null) {
                memberSummaryRepository.save(new MemberMinutesSummary(
                    minutes,
                    member,
                    node.path("progress").asText(""),
                    node.path("issues").asText(""),
                    node.path("next_tasks").asText("")
                ));
            }
        }
    }

    private void saveActionItems(MeetingMinutes minutes, JsonNode root, Map<String, Member> membersByName) {
        for (JsonNode node : root.path("action_items")) {
            Member assignee = membersByName.get(normalize(node.path("assignee").asText()));
            actionItemRepository.save(new ActionItem(
                minutes,
                assignee,
                node.path("content").asText(""),
                parseDate(node.path("due_date"))
            ));
        }
    }

    private LocalDate parseDate(JsonNode node) {
        if (node == null || node.isNull() || !StringUtils.hasText(node.asText())) {
            return null;
        }
        return LocalDate.parse(node.asText());
    }

    private Map<String, Member> membersByName(Long meetingId) {
        Map<String, Member> members = new HashMap<>();
        for (MeetingParticipant participant : participantRepository.findAllByMeetingIdOrderByIdAsc(meetingId)) {
            Member member = participant.getMember();
            members.put(normalize(member.getName()), member);
            members.put(normalize(member.getEmail()), member);
            members.put(normalize(String.valueOf(member.getId())), member);
        }
        return members;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String requireText(JsonNode root, String field) {
        String value = root.path(field).asText(null);
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Minutes response missing field: " + field);
        }
        return value;
    }

    private String title(JsonNode root) {
        return requireText(root, "title");
    }

    private String fullSummary(JsonNode root) {
        return requireText(root, "full_summary");
    }

    private LocalDate meetingDate(Meeting meeting) {
        if (meeting.getScheduledAt() != null) {
            return meeting.getScheduledAt().toLocalDate();
        }
        return meeting.getCreatedAt() == null ? LocalDate.now() : meeting.getCreatedAt().toLocalDate();
    }

    private MinutesGenerationJob findJob(Long jobId) {
        return jobRepository.findWithMeetingById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Minutes generation job not found"));
    }
}
