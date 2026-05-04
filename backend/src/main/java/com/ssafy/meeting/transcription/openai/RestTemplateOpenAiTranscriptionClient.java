package com.ssafy.meeting.transcription.openai;

import com.ssafy.meeting.common.exception.ValidationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class RestTemplateOpenAiTranscriptionClient implements OpenAiTranscriptionClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;
    private final String model;

    public RestTemplateOpenAiTranscriptionClient(
        @Value("${openai.api-key:}") String apiKey,
        @Value("${openai.transcribe-model:gpt-4o-transcribe-diarize}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String transcribe(OpenAiTranscriptionRequest request) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ValidationException("OPENAI_API_KEY is required for transcription");
        }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", request.getAudioFile());
        body.add("model", model);
        body.add("response_format", "diarized_json");
        body.add("chunking_strategy", "auto");
        addSpeakerReferences(body, request.getSpeakerReferences());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        log.info("OpenAI transcription request model={} knownSpeakers={}", model, request.getSpeakerReferences().size());
        try {
            return restTemplate.postForObject(
                "https://api.openai.com/v1/audio/transcriptions",
                new HttpEntity<>(body, headers),
                String.class
            );
        } catch (RestClientException exception) {
            throw new ValidationException("OpenAI transcription request failed: " + exception.getMessage());
        }
    }

    private void addSpeakerReferences(MultiValueMap<String, Object> body, List<SpeakerReference> references) {
        for (SpeakerReference reference : references) {
            body.add("known_speaker_names[]", reference.getName());
            body.add("known_speaker_references[]", reference.getDataUrl());
        }
    }
}
