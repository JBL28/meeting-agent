package com.ssafy.meeting.meeting.repository;

import com.ssafy.meeting.meeting.domain.Meeting;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    @EntityGraph(attributePaths = {"team", "createdBy"})
    List<Meeting> findAllByTeamIdOrderByCreatedAtDesc(Long teamId);
}
