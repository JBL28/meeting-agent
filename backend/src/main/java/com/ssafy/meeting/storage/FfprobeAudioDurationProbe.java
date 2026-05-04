package com.ssafy.meeting.storage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FfprobeAudioDurationProbe implements AudioDurationProbe {

    private final Path basePath;

    public FfprobeAudioDurationProbe(@Value("${storage.base-path:./data}") String basePath) {
        this.basePath = Paths.get(basePath).toAbsolutePath().normalize();
    }

    @Override
    public OptionalInt probeSeconds(String filePath) {
        Path target = basePath.resolve(filePath).normalize();
        try {
            Process process = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                target.toString()
            ).redirectErrorStream(true).start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("ffprobe timed out filePath={}", filePath);
                return OptionalInt.empty();
            }
            if (process.exitValue() != 0) {
                log.warn("ffprobe failed filePath={} exit={}", filePath, process.exitValue());
                return OptionalInt.empty();
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String output = reader.readLine();
                if (output == null || output.trim().isEmpty()) {
                    return OptionalInt.empty();
                }
                int seconds = (int) Math.ceil(Double.parseDouble(output.trim()));
                log.info("ffprobe duration measured filePath={} seconds={}", filePath, seconds);
                return OptionalInt.of(seconds);
            }
        } catch (Exception exception) {
            log.warn("ffprobe unavailable or failed filePath={} reason={}", filePath, exception.getMessage());
            return OptionalInt.empty();
        }
    }
}
