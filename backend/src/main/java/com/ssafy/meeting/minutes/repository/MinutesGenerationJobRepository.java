package com.ssafy.meeting.minutes.repository;

import com.ssafy.meeting.minutes.domain.MinutesGenerationJob;
import com.ssafy.meeting.minutes.domain.MinutesGenerationJobStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MinutesGenerationJobRepository extends JpaRepository<MinutesGenerationJob, Long> {
    boolean existsByMeetingIdAndStatusIn(Long meetingId, Collection<MinutesGenerationJobStatus> statuses);

    @EntityGraph(attributePaths = {"meeting", "meeting.team", "meeting.createdBy"})
    Optional<MinutesGenerationJob> findWithMeetingById(Long id);
}
