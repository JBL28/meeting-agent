package com.ssafy.meeting.minutes.repository;

import com.ssafy.meeting.minutes.domain.ActionItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    @EntityGraph(attributePaths = {"assignee"})
    List<ActionItem> findAllByMinutesIdOrderByIdAsc(Long minutesId);

    List<ActionItem> findByMinutesMeetingTeamIdAndContentContainingIgnoreCase(Long teamId, String keyword);

    @EntityGraph(attributePaths = {"minutes", "minutes.meeting", "minutes.meeting.team", "assignee"})
    Optional<ActionItem> findWithMinutesById(Long id);

    void deleteAllByMinutesId(Long minutesId);
}
