package com.ssafy.meeting.storage;

import java.util.OptionalInt;

public interface AudioDurationProbe {
    OptionalInt probeSeconds(String filePath);
}
