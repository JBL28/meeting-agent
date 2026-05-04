package com.ssafy.meeting.minutes.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meeting.common.exception.ConflictException;
import com.ssafy.meeting.common.exception.ForbiddenException;
import com.ssafy.meeting.common.exception.ResourceNotFoundException;
import com.ssafy.meeting.common.exception.ValidationException;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.domain.MeetingStatus;
import com.ssafy.meeting.meeting.repository.MeetingRepository;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.repository.MemberRepository;
import com.ssafy.meeting.minutes.domain.ActionItem;
import com.ssafy.meeting.minutes.domain.ActionItemStatus;
import com.ssafy.meeting.minutes.domain.MeetingMinutes;
import com.ssafy.meeting.minutes.domain.MinutesGenerationJob;
import com.ssafy.meeting.minutes.domain.MinutesGenerationJobStatus;
import com.ssafy.meeting.minutes.dto.ActionItemResponse;
import com.ssafy.meeting.minutes.dto.ActionItemStatusRequest;
import com.ssafy.meeting.minutes.dto.ActionItemUpdateRequest;
import com.ssafy.meeting.minutes.dto.MeetingMinutesResponse;
import com.ssafy.meeting.minutes.dto.MeetingMinutesUpdateRequest;
import com.ssafy.meeting.minutes.dto.MemberMinutesSummaryResponse;
import com.ssafy.meeting.minutes.dto.MinutesGenerationJobResponse;
import com.ssafy.meeting.minutes.dto.MinutesGenerationJobStartResponse;
import com.ssafy.meeting.minutes.repository.ActionItemRepository;
import com.ssafy.meeting.minutes.repository.MeetingMinutesRepository;
import com.ssafy.meeting.minutes.repository.MemberMinutesSummaryRepository;
import com.ssafy.meeting.minutes.repository.MinutesGenerationJobRepository;
import com.ssafy.meeting.team.domain.TeamMember;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.service.TeamPermissionEvaluator;
import com.ssafy.meeting.transcription.domain.TranscriptionJobStatus;
import com.ssafy.meeting.transcription.dto.TranscriptSegmentResponse;
import com.ssafy.meeting.transcription.repository.TranscriptSegmentRepository;
import com.ssafy.meeting.transcription.repository.TranscriptionJobRepository;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MinutesGenerationService {

    private static final List<MinutesGenerationJobStatus> ACTIVE_STATUSES = Arrays.asList(
        MinutesGenerationJobStatus.CREATED,
        MinutesGenerationJobStatus.PROCESSING
    );

    private final MeetingRepository meetingRepository;
    private final MinutesGenerationJobRepository jobRepository;
    private final MeetingMinutesRepository minutesRepository;
    private final MemberMinutesSummaryRepository memberSummaryRepository;
    private final ActionItemRepository actionItemRepository;
    private final MemberRepository memberRepository;
    private final TranscriptionJobRepository transcriptionJobRepository;
    private final TranscriptSegmentRepository transcriptSegmentRepository;
    private final TeamPermissionEvaluator permissionEvaluator;
    private final MinutesGenerationAsyncService asyncService;
    private final ObjectMapper objectMapper;

    public MinutesGenerationService(
        MeetingRepository meetingRepository,
        MinutesGenerationJobRepository jobRepository,
        MeetingMinutesRepository minutesRepository,
        MemberMinutesSummaryRepository memberSummaryRepository,
        ActionItemRepository actionItemRepository,
        MemberRepository memberRepository,
        TranscriptionJobRepository transcriptionJobRepository,
        TranscriptSegmentRepository transcriptSegmentRepository,
        TeamPermissionEvaluator permissionEvaluator,
        MinutesGenerationAsyncService asyncService,
        ObjectMapper objectMapper
    ) {
        this.meetingRepository = meetingRepository;
        this.jobRepository = jobRepository;
        this.minutesRepository = minutesRepository;
        this.memberSummaryRepository = memberSummaryRepository;
        this.actionItemRepository = actionItemRepository;
        this.memberRepository = memberRepository;
        this.transcriptionJobRepository = transcriptionJobRepository;
        this.transcriptSegmentRepository = transcriptSegmentRepository;
        this.permissionEvaluator = permissionEvaluator;
        this.asyncService = asyncService;
        this.objectMapper = objectMapper;
    }

    public MinutesGenerationJobStartResponse start(Long meetingId, Long requesterId) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        permissionEvaluator.requireRole(meeting.getTeam().getId(), requesterId, TeamRole.MEMBER);
        if (meeting.getStatus() != MeetingStatus.TRANSCRIBED) {
            throw new ValidationException("Meeting must be TRANSCRIBED before minutes generation");
        }
        if (jobRepository.existsByMeetingIdAndStatusIn(meetingId, ACTIVE_STATUSES)) {
            throw new ConflictException("Active minutes generation job already exists");
        }
        MinutesGenerationJob job = jobRepository.saveAndFlush(new MinutesGenerationJob(meeting));
        asyncService.processGeneration(job.getId());
        return new MinutesGenerationJobStartResponse(job.getId());
    }

    @Transactional(readOnly = true)
    public MinutesGenerationJobResponse getJob(Long jobId, Long requesterId) {
        MinutesGenerationJob job = findJob(jobId);
        permissionEvaluator.requireRole(job.getMeeting().getTeam().getId(), requesterId, TeamRole.VIEWER);
        return MinutesGenerationJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public MeetingMinutesResponse getMinutes(Long meetingId, Long requesterId) {
        MeetingMinutes minutes = findMinutes(meetingId);
        permissionEvaluator.requireRole(minutes.getMeeting().getTeam().getId(), requesterId, TeamRole.VIEWER);
        return response(minutes);
    }

    @Transactional
    public MeetingMinutesResponse updateMinutes(Long meetingId, Long requesterId, MeetingMinutesUpdateRequest request) {
        MeetingMinutes minutes = findMinutes(meetingId);
        permissionEvaluator.requireRole(minutes.getMeeting().getTeam().getId(), requesterId, TeamRole.MEMBER);
        minutes.update(request.getTitle().trim(), request.getFullSummary().trim(), updateRaw(minutes.getRawContent(), request));
        return response(minutes);
    }

    @Transactional(readOnly = true)
    public List<TranscriptSegmentResponse> originalSegments(Long meetingId, Long requesterId) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
        permissionEvaluator.requireRole(meeting.getTeam().getId(), requesterId, TeamRole.VIEWER);
        com.ssafy.meeting.transcription.domain.TranscriptionJob job = transcriptionJobRepository
            .findFirstByMeetingIdAndStatusOrderByCreatedAtDesc(meetingId, TranscriptionJobStatus.COMPLETED)
            .orElseThrow(() -> new ResourceNotFoundException("Completed transcription job not found"));
        return transcriptSegmentRepository.findAllByTranscriptionJobIdOrderBySequenceAsc(job.getId()).stream()
            .map(TranscriptSegmentResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMinutes(Long meetingId, Long requesterId) {
        MeetingMinutes minutes = findMinutes(meetingId);
        permissionEvaluator.requireRole(minutes.getMeeting().getTeam().getId(), requesterId, TeamRole.ADMIN);
        actionItemRepository.deleteAllByMinutesId(minutes.getId());
        memberSummaryRepository.deleteAllByMinutesId(minutes.getId());
        minutesRepository.delete(minutes);
    }

    @Transactional
    public ActionItemResponse updateActionItem(Long actionItemId, Long requesterId, ActionItemUpdateRequest request) {
        ActionItem item = findActionItem(actionItemId);
        permissionEvaluator.requireRole(item.getMinutes().getMeeting().getTeam().getId(), requesterId, TeamRole.MEMBER);
        Member assignee = null;
        if (request.getAssigneeId() != null) {
            permissionEvaluator.requireRole(item.getMinutes().getMeeting().getTeam().getId(), request.getAssigneeId(), TeamRole.VIEWER);
            assignee = memberRepository.findById(request.getAssigneeId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        }
        item.update(assignee, request.getContent().trim(), request.getDueDate());
        return ActionItemResponse.from(item);
    }

    @Transactional
    public ActionItemResponse changeActionStatus(Long actionItemId, Long requesterId, ActionItemStatusRequest request) {
        ActionItem item = findActionItem(actionItemId);
        TeamMember requester = permissionEvaluator.requireRole(item.getMinutes().getMeeting().getTeam().getId(), requesterId, TeamRole.VIEWER);
        boolean assignee = item.getAssignee() != null && item.getAssignee().getId().equals(requesterId);
        if (!assignee && !requester.getRole().atLeast(TeamRole.ADMIN)) {
            throw new ForbiddenException("Only assignee or admin can update action item status");
        }
        item.changeStatus(request.getStatus());
        return ActionItemResponse.from(item);
    }

    private String updateRaw(String rawContent, MeetingMinutesUpdateRequest request) {
        try {
            com.fasterxml.jackson.databind.node.ObjectNode root = (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(rawContent);
            root.put("title", request.getTitle().trim());
            root.put("full_summary", request.getFullSummary().trim());
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            return rawContent;
        }
    }

    private MeetingMinutesResponse response(MeetingMinutes minutes) {
        return new MeetingMinutesResponse(
            minutes,
            memberSummaryRepository.findAllByMinutesIdOrderByIdAsc(minutes.getId()).stream()
                .map(MemberMinutesSummaryResponse::from)
                .collect(Collectors.toList()),
            actionItemRepository.findAllByMinutesIdOrderByIdAsc(minutes.getId()).stream()
                .map(ActionItemResponse::from)
                .collect(Collectors.toList()),
            objectMapper
        );
    }

    private MinutesGenerationJob findJob(Long jobId) {
        return jobRepository.findWithMeetingById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Minutes generation job not found"));
    }

    private MeetingMinutes findMinutes(Long meetingId) {
        return minutesRepository.findByMeetingId(meetingId)
            .orElseThrow(() -> new ResourceNotFoundException("Meeting minutes not found"));
    }

    private ActionItem findActionItem(Long actionItemId) {
        return actionItemRepository.findWithMinutesById(actionItemId)
            .orElseThrow(() -> new ResourceNotFoundException("Action item not found"));
    }
}
