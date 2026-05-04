package com.ssafy.meeting.transcription.service;

import com.ssafy.meeting.audio.domain.AudioFile;
import com.ssafy.meeting.audio.repository.AudioFileRepository;
import com.ssafy.meeting.common.exception.ConflictException;
import com.ssafy.meeting.common.exception.ForbiddenException;
import com.ssafy.meeting.common.exception.ResourceNotFoundException;
import com.ssafy.meeting.common.exception.ValidationException;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.domain.MeetingStatus;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.repository.MemberRepository;
import com.ssafy.meeting.team.domain.TeamMember;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.service.TeamPermissionEvaluator;
import com.ssafy.meeting.transcription.domain.SpeakerMapping;
import com.ssafy.meeting.transcription.domain.TranscriptSegment;
import com.ssafy.meeting.transcription.domain.TranscriptionJob;
import com.ssafy.meeting.transcription.domain.TranscriptionJobStatus;
import com.ssafy.meeting.transcription.dto.SpeakerMappingRequest;
import com.ssafy.meeting.transcription.dto.SpeakerMappingResponse;
import com.ssafy.meeting.transcription.dto.TranscriptSegmentResponse;
import com.ssafy.meeting.transcription.dto.TranscriptionJobResponse;
import com.ssafy.meeting.transcription.dto.TranscriptionJobStartResponse;
import com.ssafy.meeting.transcription.repository.SpeakerMappingRepository;
import com.ssafy.meeting.transcription.repository.TranscriptSegmentRepository;
import com.ssafy.meeting.transcription.repository.TranscriptionJobRepository;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TranscriptionService {

    private static final List<TranscriptionJobStatus> ACTIVE_STATUSES = Arrays.asList(
        TranscriptionJobStatus.CREATED,
        TranscriptionJobStatus.PROCESSING
    );

    private final TranscriptionJobRepository jobRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final SpeakerMappingRepository mappingRepository;
    private final AudioFileRepository audioFileRepository;
    private final MemberRepository memberRepository;
    private final TeamPermissionEvaluator permissionEvaluator;
    private final TranscriptionAsyncService asyncService;

    public TranscriptionService(
        TranscriptionJobRepository jobRepository,
        TranscriptSegmentRepository segmentRepository,
        SpeakerMappingRepository mappingRepository,
        AudioFileRepository audioFileRepository,
        MemberRepository memberRepository,
        TeamPermissionEvaluator permissionEvaluator,
        TranscriptionAsyncService asyncService
    ) {
        this.jobRepository = jobRepository;
        this.segmentRepository = segmentRepository;
        this.mappingRepository = mappingRepository;
        this.audioFileRepository = audioFileRepository;
        this.memberRepository = memberRepository;
        this.permissionEvaluator = permissionEvaluator;
        this.asyncService = asyncService;
    }

    public TranscriptionJobStartResponse start(Long meetingId, Long requesterId) {
        AudioFile audioFile = audioFileRepository.findFirstByMeetingIdOrderByUploadedAtDesc(meetingId)
            .orElseThrow(() -> new ValidationException("Meeting audio is required before transcription"));
        Meeting meeting = audioFile.getMeeting();
        permissionEvaluator.requireRole(meeting.getTeam().getId(), requesterId, TeamRole.MEMBER);
        if (meeting.getStatus() != MeetingStatus.RECORDED) {
            throw new ValidationException("Meeting must be RECORDED before transcription");
        }
        if (jobRepository.existsByMeetingIdAndStatusIn(meetingId, ACTIVE_STATUSES)) {
            throw new ConflictException("Active transcription job already exists");
        }
        TranscriptionJob job = jobRepository.saveAndFlush(new TranscriptionJob(meeting, audioFile));
        asyncService.processTranscription(job.getId());
        return new TranscriptionJobStartResponse(job.getId());
    }

    @Transactional(readOnly = true)
    public TranscriptionJobResponse get(Long jobId, Long requesterId) {
        TranscriptionJob job = findJob(jobId);
        permissionEvaluator.requireRole(job.getMeeting().getTeam().getId(), requesterId, TeamRole.VIEWER);
        return TranscriptionJobResponse.from(job);
    }

    @Transactional(readOnly = true)
    public List<TranscriptSegmentResponse> segments(Long jobId, Long requesterId) {
        TranscriptionJob job = findJob(jobId);
        permissionEvaluator.requireRole(job.getMeeting().getTeam().getId(), requesterId, TeamRole.VIEWER);
        return segmentRepository.findAllByTranscriptionJobIdOrderBySequenceAsc(jobId).stream()
            .map(TranscriptSegmentResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public TranscriptionJobResponse cancel(Long jobId, Long requesterId) {
        TranscriptionJob job = findJob(jobId);
        Meeting meeting = job.getMeeting();
        if (!meeting.getCreatedBy().getId().equals(requesterId)) {
            throw new ForbiddenException("Only meeting creator can cancel transcription");
        }
        if (job.getStatus() != TranscriptionJobStatus.PROCESSING && job.getStatus() != TranscriptionJobStatus.CREATED) {
            throw new ConflictException("Only active transcription jobs can be canceled");
        }
        job.markCanceled();
        meeting.markRecorded();
        return TranscriptionJobResponse.from(job);
    }

    public TranscriptionJobStartResponse retry(Long jobId, Long requesterId) {
        TranscriptionJob previous = findJob(jobId);
        Meeting meeting = previous.getMeeting();
        permissionEvaluator.requireRole(meeting.getTeam().getId(), requesterId, TeamRole.MEMBER);
        if (previous.getStatus() != TranscriptionJobStatus.FAILED) {
            throw new ConflictException("Only failed transcription jobs can be retried");
        }
        if (jobRepository.existsByMeetingIdAndStatusIn(meeting.getId(), ACTIVE_STATUSES)) {
            throw new ConflictException("Active transcription job already exists");
        }
        TranscriptionJob next = jobRepository.saveAndFlush(new TranscriptionJob(meeting, previous.getAudioFile()));
        asyncService.processTranscription(next.getId());
        return new TranscriptionJobStartResponse(next.getId());
    }

    @Transactional
    public List<SpeakerMappingResponse> saveMappings(Long jobId, Long requesterId, SpeakerMappingRequest request) {
        TranscriptionJob job = findJob(jobId);
        Meeting meeting = job.getMeeting();
        permissionEvaluator.requireRole(meeting.getTeam().getId(), requesterId, TeamRole.MEMBER);
        Member confirmer = memberRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        for (SpeakerMappingRequest.Mapping item : request.getMappings()) {
            TeamMember mappedMembership = permissionEvaluator.requireRole(meeting.getTeam().getId(), item.getMemberId(), TeamRole.VIEWER);
            Member mappedMember = mappedMembership.getMember();
            SpeakerMapping mapping = mappingRepository.findByTranscriptionJobIdAndSpeaker(jobId, item.getSpeaker())
                .orElse(new SpeakerMapping(job, item.getSpeaker(), mappedMember, false, confirmer));
            mapping.update(mappedMember, false, confirmer);
            mappingRepository.save(mapping);
            for (TranscriptSegment segment : segmentRepository.findAllByTranscriptionJobIdOrderBySequenceAsc(jobId)) {
                if (segment.getSpeaker().equals(item.getSpeaker())) {
                    segment.mapMember(mappedMember);
                }
            }
        }
        return mappingRepository.findAllByTranscriptionJobIdOrderBySpeakerAsc(jobId).stream()
            .map(SpeakerMappingResponse::from)
            .collect(Collectors.toList());
    }

    private TranscriptionJob findJob(Long jobId) {
        return jobRepository.findWithMeetingAndAudioFileById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Transcription job not found"));
    }
}
