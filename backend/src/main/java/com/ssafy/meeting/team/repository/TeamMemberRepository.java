package com.ssafy.meeting.team.repository;

import com.ssafy.meeting.team.domain.TeamMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {
    @EntityGraph(attributePaths = {"team", "member"})
    Optional<TeamMember> findByTeamIdAndMemberId(Long teamId, Long memberId);

    @EntityGraph(attributePaths = {"team", "member"})
    List<TeamMember> findAllByMemberIdOrderByJoinedAtDesc(Long memberId);

    @EntityGraph(attributePaths = {"team", "member"})
    List<TeamMember> findAllByTeamIdOrderByJoinedAtAsc(Long teamId);

    boolean existsByTeamIdAndMemberId(Long teamId, Long memberId);
}
