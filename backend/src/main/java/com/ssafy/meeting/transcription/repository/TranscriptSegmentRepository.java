package com.ssafy.meeting.transcription.repository;

import com.ssafy.meeting.transcription.domain.TranscriptSegment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TranscriptSegmentRepository extends JpaRepository<TranscriptSegment, Long> {
    @EntityGraph(attributePaths = {"member"})
    List<TranscriptSegment> findAllByTranscriptionJobIdOrderBySequenceAsc(Long transcriptionJobId);

    @EntityGraph(attributePaths = {"member", "transcriptionJob", "transcriptionJob.meeting"})
    List<TranscriptSegment> findByTranscriptionJobMeetingTeamIdAndTextContainingIgnoreCase(Long teamId, String keyword);
}
