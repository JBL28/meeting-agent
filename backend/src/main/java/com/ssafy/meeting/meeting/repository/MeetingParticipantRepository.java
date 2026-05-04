package com.ssafy.meeting.meeting.repository;

import com.ssafy.meeting.meeting.domain.MeetingParticipant;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    boolean existsByMeetingIdAndMemberId(Long meetingId, Long memberId);

    @EntityGraph(attributePaths = {"member"})
    List<MeetingParticipant> findAllByMeetingIdOrderByIdAsc(Long meetingId);
}
