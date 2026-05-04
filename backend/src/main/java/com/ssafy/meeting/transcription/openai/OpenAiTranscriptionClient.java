package com.ssafy.meeting.transcription.openai;

public interface OpenAiTranscriptionClient {
    String transcribe(OpenAiTranscriptionRequest request);
}
