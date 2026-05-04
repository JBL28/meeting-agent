package com.ssafy.meeting.meeting.service;

import com.ssafy.meeting.common.exception.ForbiddenException;
import com.ssafy.meeting.common.exception.ResourceNotFoundException;
import com.ssafy.meeting.common.exception.ValidationException;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.domain.MeetingParticipant;
import com.ssafy.meeting.meeting.dto.MeetingCreateRequest;
import com.ssafy.meeting.meeting.dto.MeetingParticipantRequest;
import com.ssafy.meeting.meeting.dto.MeetingParticipantResponse;
import com.ssafy.meeting.meeting.dto.MeetingResponse;
import com.ssafy.meeting.meeting.repository.MeetingParticipantRepository;
import com.ssafy.meeting.meeting.repository.MeetingRepository;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.repository.MemberRepository;
import com.ssafy.meeting.team.domain.Team;
import com.ssafy.meeting.team.domain.TeamMember;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.repository.TeamRepository;
import com.ssafy.meeting.team.service.TeamPermissionEvaluator;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final TeamPermissionEvaluator permissionEvaluator;

    public MeetingService(
        MeetingRepository meetingRepository,
        MeetingParticipantRepository participantRepository,
        TeamRepository teamRepository,
        MemberRepository memberRepository,
        TeamPermissionEvaluator permissionEvaluator
    ) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.permissionEvaluator = permissionEvaluator;
    }

    @Transactional
    public MeetingResponse create(Long teamId, Long requesterId, MeetingCreateRequest request) {
        permissionEvaluator.requireRole(teamId, requesterId, TeamRole.MEMBER);
        Team team = teamRepository.findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        Member creator = memberRepository.findById(requesterId)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        Meeting meeting = meetingRepository.save(new Meeting(team, request.getTitle().trim(), request.getScheduledAt(), creator));
        participantRepository.save(new MeetingParticipant(meeting, creator));
        log.info("Meeting created meetingId={} teamId={} creatorId={}", meeting.getId(), teamId, requesterId);
        return MeetingResponse.of(meeting, Collections.singletonList(new MeetingParticipantResponse(creator.getId(), creator.getEmail(), creator.getName())));
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> list(Long teamId, Long requesterId) {
        permissionEvaluator.requireRole(teamId, requesterId, TeamRole.VIEWER);
        return meetingRepository.findAllByTeamIdOrderByCreatedAtDesc(teamId).stream()
            .map(meeting -> MeetingResponse.of(meeting, Collections.emptyList()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeetingResponse get(Long meetingId, Long requesterId) {
        Meeting meeting = findMeeting(meetingId);
        permissionEvaluator.requireRole(meeting.getTeam().getId(), requesterId, TeamRole.VIEWER);
        return MeetingResponse.of(meeting, participants(meetingId));
    }

    @Transactional
    public MeetingParticipantResponse addParticipant(Long meetingId, Long requesterId, MeetingParticipantRequest request) {
        Meeting meeting = findMeeting(meetingId);
        TeamMember requesterMembership = permissionEvaluator.requireRole(meeting.getTeam().getId(), requesterId, TeamRole.MEMBER);
        boolean creator = meeting.getCreatedBy().getId().equals(requesterId);
        if (!creator && !requesterMembership.getRole().atLeast(TeamRole.ADMIN)) {
            throw new ForbiddenException("Only meeting creator or admin can add participants");
        }
        permissionEvaluator.requireRole(meeting.getTeam().getId(), request.getMemberId(), TeamRole.VIEWER);
        if (participantRepository.existsByMeetingIdAndMemberId(meetingId, request.getMemberId())) {
            throw new ValidationException("Member is already a participant");
        }
        Member member = memberRepository.findById(request.getMemberId())
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        MeetingParticipant participant = participantRepository.save(new MeetingParticipant(meeting, member));
        log.info("Meeting participant added meetingId={} requesterId={} participantId={}", meetingId, requesterId, member.getId());
        return MeetingParticipantResponse.from(participant);
    }

    private Meeting findMeeting(Long meetingId) {
        return meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ResourceNotFoundException("Meeting not found"));
    }

    private List<MeetingParticipantResponse> participants(Long meetingId) {
        return participantRepository.findAllByMeetingIdOrderByIdAsc(meetingId).stream()
            .map(MeetingParticipantResponse::from)
            .collect(Collectors.toList());
    }
}
