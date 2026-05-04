package com.ssafy.meeting.voice.repository;

import com.ssafy.meeting.voice.domain.VoiceSample;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoiceSampleRepository extends JpaRepository<VoiceSample, Long> {
    @EntityGraph(attributePaths = {"member", "team"})
    List<VoiceSample> findAllByTeamIdAndMemberIdOrderByCreatedAtDesc(Long teamId, Long memberId);

    @EntityGraph(attributePaths = {"member", "team"})
    Optional<VoiceSample> findWithMemberAndTeamById(Long id);
}
