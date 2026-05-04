package com.ssafy.meeting.transcription.repository;

import com.ssafy.meeting.transcription.domain.TranscriptionJob;
import com.ssafy.meeting.transcription.domain.TranscriptionJobStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptionJobRepository extends JpaRepository<TranscriptionJob, Long> {
    boolean existsByMeetingIdAndStatusIn(Long meetingId, Collection<TranscriptionJobStatus> statuses);

    @EntityGraph(attributePaths = {"meeting", "meeting.team", "meeting.createdBy", "audioFile"})
    Optional<TranscriptionJob> findWithMeetingAndAudioFileById(Long id);

    List<TranscriptionJob> findAllByMeetingIdOrderByCreatedAtDesc(Long meetingId);

    Optional<TranscriptionJob> findFirstByMeetingIdAndStatusOrderByCreatedAtDesc(Long meetingId, TranscriptionJobStatus status);
}
