package com.ssafy.meeting.audio.repository;

import com.ssafy.meeting.audio.domain.AudioFile;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AudioFileRepository extends JpaRepository<AudioFile, Long> {
    @EntityGraph(attributePaths = {"meeting", "meeting.team"})
    Optional<AudioFile> findWithMeetingById(Long id);

    @EntityGraph(attributePaths = {"meeting", "meeting.team"})
    Optional<AudioFile> findFirstByMeetingIdOrderByUploadedAtDesc(Long meetingId);
}
