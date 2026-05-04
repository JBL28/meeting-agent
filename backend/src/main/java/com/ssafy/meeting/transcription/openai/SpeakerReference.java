package com.ssafy.meeting.transcription.openai;

public class SpeakerReference {
    private final String name;
    private final String dataUrl;

    public SpeakerReference(String name, String dataUrl) {
        this.name = name;
        this.dataUrl = dataUrl;
    }

    public String getName() { return name; }
    public String getDataUrl() { return dataUrl; }
}
