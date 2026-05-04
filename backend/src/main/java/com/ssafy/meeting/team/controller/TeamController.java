package com.ssafy.meeting.team.controller;

import com.ssafy.meeting.auth.security.UserPrincipal;
import com.ssafy.meeting.common.api.ApiResponse;
import com.ssafy.meeting.team.dto.TeamCreateRequest;
import com.ssafy.meeting.team.dto.TeamMemberInviteRequest;
import com.ssafy.meeting.team.dto.TeamMemberResponse;
import com.ssafy.meeting.team.dto.TeamMemberRoleRequest;
import com.ssafy.meeting.team.dto.TeamResponse;
import com.ssafy.meeting.team.dto.TeamUpdateRequest;
import com.ssafy.meeting.team.service.TeamService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ApiResponse<TeamResponse> createTeam(@AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody TeamCreateRequest request) {
        return ApiResponse.success(teamService.createTeam(principal.getId(), request));
    }

    @GetMapping
    public ApiResponse<List<TeamResponse>> listMyTeams(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(teamService.listMyTeams(principal.getId()));
    }

    @GetMapping("/{teamId}")
    @PreAuthorize("@teamPermissionEvaluator.hasTeamRole(authentication.principal.id, #teamId, 'VIEWER')")
    public ApiResponse<TeamResponse> getTeam(@PathVariable Long teamId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(teamService.getTeam(teamId, principal.getId()));
    }

    @PutMapping("/{teamId}")
    @PreAuthorize("@teamPermissionEvaluator.hasTeamRole(authentication.principal.id, #teamId, 'OWNER')")
    public ApiResponse<TeamResponse> updateTeam(@PathVariable Long teamId, @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody TeamUpdateRequest request) {
        return ApiResponse.success(teamService.updateTeam(teamId, principal.getId(), request));
    }

    @DeleteMapping("/{teamId}")
    @PreAuthorize("@teamPermissionEvaluator.hasTeamRole(authentication.principal.id, #teamId, 'OWNER')")
    public ApiResponse<Void> deleteTeam(@PathVariable Long teamId, @AuthenticationPrincipal UserPrincipal principal) {
        teamService.deleteTeam(teamId, principal.getId());
        return ApiResponse.success(null);
    }

    @GetMapping("/{teamId}/members")
    @PreAuthorize("@teamPermissionEvaluator.hasTeamRole(authentication.principal.id, #teamId, 'VIEWER')")
    public ApiResponse<List<TeamMemberResponse>> listMembers(@PathVariable Long teamId, @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(teamService.listMembers(teamId, principal.getId()));
    }

    @PostMapping("/{teamId}/members")
    @PreAuthorize("@teamPermissionEvaluator.hasTeamRole(authentication.principal.id, #teamId, 'ADMIN')")
    public ApiResponse<TeamMemberResponse> inviteMember(@PathVariable Long teamId, @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody TeamMemberInviteRequest request) {
        return ApiResponse.success(teamService.inviteMember(teamId, principal.getId(), request));
    }

    @PutMapping("/{teamId}/members/{memberId}/role")
    @PreAuthorize("@teamPermissionEvaluator.hasTeamRole(authentication.principal.id, #teamId, 'OWNER')")
    public ApiResponse<TeamMemberResponse> changeRole(@PathVariable Long teamId, @PathVariable Long memberId, @AuthenticationPrincipal UserPrincipal principal, @Valid @RequestBody TeamMemberRoleRequest request) {
        return ApiResponse.success(teamService.changeRole(teamId, memberId, principal.getId(), request));
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    @PreAuthorize("@teamPermissionEvaluator.hasTeamRole(authentication.principal.id, #teamId, 'ADMIN')")
    public ApiResponse<Void> removeMember(@PathVariable Long teamId, @PathVariable Long memberId, @AuthenticationPrincipal UserPrincipal principal) {
        teamService.removeMember(teamId, memberId, principal.getId());
        return ApiResponse.success(null);
    }
}
