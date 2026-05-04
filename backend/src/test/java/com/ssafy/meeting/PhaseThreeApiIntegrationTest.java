package com.ssafy.meeting;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meeting.audio.domain.AudioFile;
import com.ssafy.meeting.audio.repository.AudioFileRepository;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.repository.MeetingRepository;
import com.ssafy.meeting.transcription.service.TranscriptionAsyncService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhaseThreeApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private AudioFileRepository audioFileRepository;

    @MockBean
    private TranscriptionAsyncService asyncService;

    @Test
    void transcriptionStartRejectsInvalidStatesAndActiveJobs() throws Exception {
        doReturn(CompletableFuture.completedFuture(null)).when(asyncService).processTranscription(anyLong());
        String suffix = String.valueOf(System.nanoTime());
        String email = "phase3-owner-" + suffix + "@example.com";
        register(email, "Owner");
        String token = login(email);
        Long teamId = createTeam(token, "Phase 3 Team");
        Long draftMeetingId = createMeeting(token, teamId, "Draft Meeting");
        Meeting draftMeeting = meetingRepository.findById(draftMeetingId).orElseThrow(IllegalStateException::new);
        audioFileRepository.save(new AudioFile(draftMeeting, "audio/draft.webm", "draft.webm", 3L, 1));

        mockMvc.perform(post("/api/meetings/" + draftMeetingId + "/transcription").header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());

        Long recordedMeetingId = createMeeting(token, teamId, "Recorded Meeting");
        MockMultipartFile audio = new MockMultipartFile("file", "meeting.webm", "audio/webm", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/api/meetings/" + recordedMeetingId + "/audio")
                .file(audio)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());

        MvcResult startResult = mockMvc.perform(post("/api/meetings/" + recordedMeetingId + "/transcription")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.data.jobId").isNumber())
            .andReturn();
        Long jobId = objectMapper.readTree(startResult.getResponse().getContentAsString()).at("/data/jobId").asLong();

        mockMvc.perform(post("/api/meetings/" + recordedMeetingId + "/transcription").header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/transcription-jobs/" + jobId + "/retry").header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict());
    }

    private Long register(String email, String name) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", "password123");
        request.put("name", name);
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asLong();
    }

    private String login(String email) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return json.at("/data/accessToken").asText();
    }

    private Long createTeam(String accessToken, String name) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        MvcResult result = mockMvc.perform(post("/api/teams")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asLong();
    }

    private Long createMeeting(String accessToken, Long teamId, String title) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("title", title);
        MvcResult result = mockMvc.perform(post("/api/teams/" + teamId + "/meetings")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asLong();
    }
}
