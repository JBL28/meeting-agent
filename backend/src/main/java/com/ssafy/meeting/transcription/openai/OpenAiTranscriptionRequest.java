package com.ssafy.meeting.transcription.openai;

import java.util.List;
import org.springframework.core.io.Resource;

public class OpenAiTranscriptionRequest {
    private final Resource audioFile;
    private final List<SpeakerReference> speakerReferences;

    public OpenAiTranscriptionRequest(Resource audioFile, List<SpeakerReference> speakerReferences) {
        this.audioFile = audioFile;
        this.speakerReferences = speakerReferences;
    }

    public Resource getAudioFile() { return audioFile; }
    public List<SpeakerReference> getSpeakerReferences() { return speakerReferences; }
}
