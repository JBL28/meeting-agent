package com.ssafy.meeting.auth.repository;

import com.ssafy.meeting.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}
