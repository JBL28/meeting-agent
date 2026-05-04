package com.ssafy.meeting.storage;

import com.ssafy.meeting.common.exception.ValidationException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FilePolicyValidator {

    private static final long MB = 1024L * 1024L;
    private static final Set<String> VOICE_EXTENSIONS = new HashSet<>(Arrays.asList("wav", "mp3", "webm"));
    private static final Set<String> MEETING_AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList("wav", "mp3", "webm", "m4a"));

    public void validateVoiceSample(MultipartFile file) {
        validate(file, VOICE_EXTENSIONS, 10L * MB, "Voice sample");
    }

    public void validateMeetingAudio(MultipartFile file) {
        validate(file, MEETING_AUDIO_EXTENSIONS, 100L * MB, "Meeting audio");
    }

    private void validate(MultipartFile file, Set<String> extensions, long maxBytes, String label) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException(label + " file is required");
        }
        if (file.getSize() > maxBytes) {
            throw new ValidationException(label + " file size exceeds limit");
        }
        String extension = extension(file.getOriginalFilename());
        if (!extensions.contains(extension)) {
            throw new ValidationException(label + " file type is not allowed");
        }
    }

    private String extension(String fileName) {
        String cleanName = StringUtils.cleanPath(fileName == null ? "" : fileName);
        int dot = cleanName.lastIndexOf('.');
        if (dot < 0 || dot == cleanName.length() - 1) {
            return "";
        }
        return cleanName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
