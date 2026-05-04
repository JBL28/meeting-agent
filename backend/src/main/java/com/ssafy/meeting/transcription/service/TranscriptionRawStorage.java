package com.ssafy.meeting.transcription.service;

import com.ssafy.meeting.common.exception.ValidationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TranscriptionRawStorage {

    private final Path basePath;

    public TranscriptionRawStorage(@Value("${storage.base-path:./data}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    public String saveSuccess(Long jobId, String json) {
        return save("stt-raw/" + jobId + ".json", json);
    }

    public String saveError(Long jobId, String message) {
        String escaped = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        return save("stt-raw/" + jobId + ".error.json", "{\"error\":\"" + escaped + "\"}");
    }

    private String save(String relativePath, String content) {
        Path destination = basePath.resolve(relativePath).normalize();
        if (!destination.startsWith(basePath)) {
            throw new ValidationException("Invalid raw transcription storage path");
        }
        try {
            Files.createDirectories(destination.getParent());
            Files.write(destination, content.getBytes(StandardCharsets.UTF_8));
            return relativePath.replace('\\', '/');
        } catch (IOException exception) {
            throw new ValidationException("Could not store raw transcription response");
        }
    }
}
