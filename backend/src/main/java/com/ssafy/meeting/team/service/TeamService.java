package com.ssafy.meeting.team.service;

import com.ssafy.meeting.common.exception.ForbiddenException;
import com.ssafy.meeting.common.exception.ResourceNotFoundException;
import com.ssafy.meeting.common.exception.ValidationException;
import com.ssafy.meeting.member.domain.Member;
import com.ssafy.meeting.member.repository.MemberRepository;
import com.ssafy.meeting.team.domain.Team;
import com.ssafy.meeting.team.domain.TeamMember;
import com.ssafy.meeting.team.domain.TeamRole;
import com.ssafy.meeting.team.dto.TeamCreateRequest;
import com.ssafy.meeting.team.dto.TeamMemberInviteRequest;
import com.ssafy.meeting.team.dto.TeamMemberResponse;
import com.ssafy.meeting.team.dto.TeamMemberRoleRequest;
import com.ssafy.meeting.team.dto.TeamResponse;
import com.ssafy.meeting.team.dto.TeamUpdateRequest;
import com.ssafy.meeting.team.repository.TeamMemberRepository;
import com.ssafy.meeting.team.repository.TeamRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MemberRepository memberRepository;
    private final TeamPermissionEvaluator permissionEvaluator;

    public TeamService(
        TeamRepository teamRepository,
        TeamMemberRepository teamMemberRepository,
        MemberRepository memberRepository,
        TeamPermissionEvaluator permissionEvaluator
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.memberRepository = memberRepository;
        this.permissionEvaluator = permissionEvaluator;
    }

    @Transactional
    public TeamResponse createTeam(Long requesterId, TeamCreateRequest request) {
        Member owner = findMember(requesterId);
        Team team = teamRepository.save(new Team(request.getName().trim(), owner));
        TeamMember ownerMembership = teamMemberRepository.save(new TeamMember(team, owner, TeamRole.OWNER));
        log.info("Team created teamId={} ownerId={}", team.getId(), owner.getId());
        return TeamResponse.of(team, ownerMembership.getRole());
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listMyTeams(Long requesterId) {
        return teamMemberRepository.findAllByMemberIdOrderByJoinedAtDesc(requesterId).stream()
            .map(teamMember -> TeamResponse.of(teamMember.getTeam(), teamMember.getRole()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeam(Long teamId, Long requesterId) {
        TeamMember membership = permissionEvaluator.requireRole(teamId, requesterId, TeamRole.VIEWER);
        return TeamResponse.of(membership.getTeam(), membership.getRole());
    }

    @Transactional
    public TeamResponse updateTeam(Long teamId, Long requesterId, TeamUpdateRequest request) {
        TeamMember membership = permissionEvaluator.requireRole(teamId, requesterId, TeamRole.OWNER);
        Team team = membership.getTeam();
        team.rename(request.getName().trim());
        log.info("Team updated teamId={} requesterId={}", teamId, requesterId);
        return TeamResponse.of(team, membership.getRole());
    }

    @Transactional
    public void deleteTeam(Long teamId, Long requesterId) {
        permissionEvaluator.requireRole(teamId, requesterId, TeamRole.OWNER);
        List<TeamMember> members = teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId);
        teamMemberRepository.deleteAll(members);
        teamRepository.delete(findTeam(teamId));
        log.info("Team deleted teamId={} requesterId={}", teamId, requesterId);
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> listMembers(Long teamId, Long requesterId) {
        permissionEvaluator.requireRole(teamId, requesterId, TeamRole.VIEWER);
        return teamMemberRepository.findAllByTeamIdOrderByJoinedAtAsc(teamId).stream()
            .map(TeamMemberResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional
    public TeamMemberResponse inviteMember(Long teamId, Long requesterId, TeamMemberInviteRequest request) {
        permissionEvaluator.requireRole(teamId, requesterId, TeamRole.ADMIN);
        Team team = findTeam(teamId);
        Member invited = memberRepository.findByEmail(request.getEmail().toLowerCase().trim())
            .orElseThrow(() -> new ResourceNotFoundException("Member with email not found"));
        if (teamMemberRepository.existsByTeamIdAndMemberId(teamId, invited.getId())) {
            throw new ValidationException("Member is already in the team");
        }
        TeamRole role = request.getRole() == null ? TeamRole.MEMBER : request.getRole();
        if (role == TeamRole.OWNER) {
            throw new ValidationException("Use role change to transfer owner role");
        }
        TeamMember teamMember = teamMemberRepository.save(new TeamMember(team, invited, role));
        log.info("Team member invited teamId={} requesterId={} invitedMemberId={} role={}", teamId, requesterId, invited.getId(), role);
        return TeamMemberResponse.from(teamMember);
    }

    @Transactional
    public TeamMemberResponse changeRole(Long teamId, Long memberId, Long requesterId, TeamMemberRoleRequest request) {
        permissionEvaluator.requireRole(teamId, requesterId, TeamRole.OWNER);
        TeamMember target = teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Team membership not found"));
        if (target.getRole() == TeamRole.OWNER) {
            throw new ForbiddenException("Owner role cannot be changed in Phase 1");
        }
        if (request.getRole() == TeamRole.OWNER) {
            throw new ValidationException("Owner transfer is not supported in Phase 1");
        }
        target.changeRole(request.getRole());
        log.info("Team member role changed teamId={} requesterId={} targetMemberId={} role={}", teamId, requesterId, memberId, request.getRole());
        return TeamMemberResponse.from(target);
    }

    @Transactional
    public void removeMember(Long teamId, Long memberId, Long requesterId) {
        permissionEvaluator.requireRole(teamId, requesterId, TeamRole.ADMIN);
        TeamMember target = teamMemberRepository.findByTeamIdAndMemberId(teamId, memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Team membership not found"));
        if (target.getRole() == TeamRole.OWNER) {
            throw new ForbiddenException("Owner cannot be removed");
        }
        teamMemberRepository.delete(target);
        log.info("Team member removed teamId={} requesterId={} targetMemberId={}", teamId, requesterId, memberId);
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }

    private Team findTeam(Long teamId) {
        return teamRepository.findById(teamId)
            .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
    }
}
