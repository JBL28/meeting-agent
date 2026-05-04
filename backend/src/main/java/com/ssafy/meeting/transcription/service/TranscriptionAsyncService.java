package com.ssafy.meeting.transcription.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meeting.common.exception.ResourceNotFoundException;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.repository.MeetingParticipantRepository;
import com.ssafy.meeting.meeting.repository.MeetingRepository;
import com.ssafy.meeting.storage.StorageService;
import com.ssafy.meeting.transcription.domain.TranscriptSegment;
import com.ssafy.meeting.transcription.domain.TranscriptionJob;
import com.ssafy.meeting.transcription.openai.OpenAiTranscriptionClient;
import com.ssafy.meeting.transcription.openai.OpenAiTranscriptionRequest;
import com.ssafy.meeting.transcription.openai.SpeakerReference;
import com.ssafy.meeting.transcription.repository.TranscriptSegmentRepository;
import com.ssafy.meeting.transcription.repository.TranscriptionJobRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
public class TranscriptionAsyncService {

    private final TranscriptionJobRepository jobRepository;
    private final TranscriptSegmentRepository segmentRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final StorageService storageService;
    private final SpeakerReferenceBuilder speakerReferenceBuilder;
    private final OpenAiTranscriptionClient openAiClient;
    private final TranscriptionRawStorage rawStorage;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public TranscriptionAsyncService(
        TranscriptionJobRepository jobRepository,
        TranscriptSegmentRepository segmentRepository,
        MeetingRepository meetingRepository,
        MeetingParticipantRepository participantRepository,
        StorageService storageService,
        SpeakerReferenceBuilder speakerReferenceBuilder,
        OpenAiTranscriptionClient openAiClient,
        TranscriptionRawStorage rawStorage,
        ObjectMapper objectMapper,
        TransactionTemplate transactionTemplate
    ) {
        this.jobRepository = jobRepository;
        this.segmentRepository = segmentRepository;
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.storageService = storageService;
        this.speakerReferenceBuilder = speakerReferenceBuilder;
        this.openAiClient = openAiClient;
        this.rawStorage = rawStorage;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Async("transcriptionExecutor")
    public CompletableFuture<Void> processTranscription(Long jobId) {
        try {
            JobWork work = markProcessing(jobId);
            Resource audio = storageService.load(work.audioFilePath);
            String rawJson = openAiClient.transcribe(new OpenAiTranscriptionRequest(audio, work.references));
            String rawPath = rawStorage.saveSuccess(jobId, rawJson);
            markCompleted(jobId, rawJson, rawPath);
        } catch (Exception exception) {
            failJob(jobId, exception);
        }
        return CompletableFuture.completedFuture(null);
    }

    public void process(Long jobId) {
        processTranscription(jobId).join();
    }

    private JobWork markProcessing(Long jobId) {
        return transactionTemplate.execute(status -> {
        TranscriptionJob job = findJob(jobId);
        job.markProcessing();
        Meeting meeting = job.getMeeting();
        meeting.markTranscribing();
        log.info("STT processing started jobId={} meetingId={}", jobId, meeting.getId());
        List<SpeakerReference> references = speakerReferenceBuilder.build(
            meeting.getTeam().getId(),
            meeting.getId(),
            participantRepository.findAllByMeetingIdOrderByIdAsc(meeting.getId())
        );
            return new JobWork(job.getAudioFile().getFilePath(), references);
        });
    }

    private void markCompleted(Long jobId, String rawJson, String rawPath) {
        transactionTemplate.executeWithoutResult(status -> {
            TranscriptionJob job = findJob(jobId);
            segmentRepository.saveAll(parseSegments(job, rawJson));
            job.markCompleted(rawPath);
            job.getMeeting().markTranscribed();
            log.info("STT processing completed jobId={} meetingId={} rawPath={}", jobId, job.getMeeting().getId(), rawPath);
        });
    }

    public void failJob(Long jobId, Exception exception) {
        transactionTemplate.executeWithoutResult(status -> {
        TranscriptionJob job = findJob(jobId);
        String rawPath = rawStorage.saveError(jobId, exception.getMessage());
        job.markFailed(exception.getMessage(), rawPath);
        job.getMeeting().markRecorded();
        log.error("STT processing failed jobId={} error={}", jobId, exception.getMessage(), exception);
        });
    }

    private TranscriptionJob findJob(Long jobId) {
        return jobRepository.findWithMeetingAndAudioFileById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Transcription job not found"));
    }

    private List<TranscriptSegment> parseSegments(TranscriptionJob job, String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode segmentsNode = root.path("segments");
            List<TranscriptSegment> segments = new ArrayList<>();
            if (!segmentsNode.isArray()) {
                return segments;
            }
            int sequence = 0;
            for (JsonNode node : segmentsNode) {
                segments.add(new TranscriptSegment(
                    job,
                    node.path("speaker").asText("UNKNOWN"),
                    node.path("start").asDouble(),
                    node.path("end").asDouble(),
                    node.path("text").asText(""),
                    sequence++
                ));
            }
            return segments;
        } catch (Exception exception) {
            throw new IllegalStateException("Could not parse diarized transcription response", exception);
        }
    }

    private static class JobWork {
        private final String audioFilePath;
        private final List<SpeakerReference> references;

        private JobWork(String audioFilePath, List<SpeakerReference> references) {
            this.audioFilePath = audioFilePath;
            this.references = references;
        }
    }
}
