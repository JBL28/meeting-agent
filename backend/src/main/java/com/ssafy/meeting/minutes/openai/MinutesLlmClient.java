package com.ssafy.meeting.minutes.openai;

public interface MinutesLlmClient {
    String generate(String transcript);
}
