package com.ssafy.meeting.minutes.repository;

import com.ssafy.meeting.minutes.domain.MemberMinutesSummary;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberMinutesSummaryRepository extends JpaRepository<MemberMinutesSummary, Long> {
    @EntityGraph(attributePaths = {"member"})
    List<MemberMinutesSummary> findAllByMinutesIdOrderByIdAsc(Long minutesId);

    void deleteAllByMinutesId(Long minutesId);
}
