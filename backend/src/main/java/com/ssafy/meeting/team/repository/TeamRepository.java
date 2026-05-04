package com.ssafy.meeting.team.repository;

import com.ssafy.meeting.team.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, Long> {
}
