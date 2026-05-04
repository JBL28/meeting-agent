package com.ssafy.meeting.transcription.repository;

import com.ssafy.meeting.transcription.domain.SpeakerMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeakerMappingRepository extends JpaRepository<SpeakerMapping, Long> {
    @EntityGraph(attributePaths = {"member"})
    List<SpeakerMapping> findAllByTranscriptionJobIdOrderBySpeakerAsc(Long transcriptionJobId);

    Optional<SpeakerMapping> findByTranscriptionJobIdAndSpeaker(Long transcriptionJobId, String speaker);
}
