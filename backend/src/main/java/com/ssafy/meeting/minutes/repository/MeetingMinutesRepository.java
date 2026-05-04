package com.ssafy.meeting.minutes.repository;

import com.ssafy.meeting.minutes.domain.MeetingMinutes;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingMinutesRepository extends JpaRepository<MeetingMinutes, Long> {
    @EntityGraph(attributePaths = {"meeting", "meeting.team", "generationJob"})
    Optional<MeetingMinutes> findByMeetingId(Long meetingId);

    List<MeetingMinutes> findByMeetingTeamIdAndFullSummaryContainingIgnoreCase(Long teamId, String keyword);
}
