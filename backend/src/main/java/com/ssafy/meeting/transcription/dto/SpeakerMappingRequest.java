package com.ssafy.meeting.transcription.dto;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class SpeakerMappingRequest {
    @Valid
    @NotEmpty
    private List<Mapping> mappings = new ArrayList<>();

    public List<Mapping> getMappings() {
        return mappings;
    }

    public void setMappings(List<Mapping> mappings) {
        this.mappings = mappings;
    }

    public static class Mapping {
        @NotBlank
        private String speaker;

        @NotNull
        private Long memberId;

        public String getSpeaker() { return speaker; }
        public void setSpeaker(String speaker) { this.speaker = speaker; }
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
    }
}
