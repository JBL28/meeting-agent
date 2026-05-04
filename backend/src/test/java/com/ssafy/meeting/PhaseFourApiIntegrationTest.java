package com.ssafy.meeting;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meeting.meeting.domain.Meeting;
import com.ssafy.meeting.meeting.repository.MeetingRepository;
import com.ssafy.meeting.minutes.domain.MeetingMinutes;
import com.ssafy.meeting.minutes.domain.MinutesGenerationJob;
import com.ssafy.meeting.minutes.repository.MeetingMinutesRepository;
import com.ssafy.meeting.minutes.repository.MinutesGenerationJobRepository;
import com.ssafy.meeting.minutes.service.MinutesGenerationAsyncService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhaseFourApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private MinutesGenerationJobRepository jobRepository;

    @Autowired
    private MeetingMinutesRepository minutesRepository;

    @MockBean
    private MinutesGenerationAsyncService asyncService;

    @Test
    void minutesGenerateRequiresTranscribedAndViewerCannotEdit() throws Exception {
        doReturn(CompletableFuture.completedFuture(null)).when(asyncService).processGeneration(anyLong());
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "phase4-owner-" + suffix + "@example.com";
        String viewerEmail = "phase4-viewer-" + suffix + "@example.com";
        register(ownerEmail, "Owner");
        register(viewerEmail, "Viewer");
        String ownerToken = login(ownerEmail);
        String viewerToken = login(viewerEmail);
        Long teamId = createTeam(ownerToken, "Phase 4 Team");
        invite(ownerToken, teamId, viewerEmail, "VIEWER");
        Long meetingId = createMeeting(ownerToken, teamId, "Phase 4 Meeting");

        mockMvc.perform(post("/api/meetings/" + meetingId + "/minutes/generate").header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isBadRequest());

        Meeting meeting = meetingRepository.findById(meetingId).orElseThrow(IllegalStateException::new);
        MinutesGenerationJob job = jobRepository.save(new MinutesGenerationJob(meeting));
        minutesRepository.save(new MeetingMinutes(meeting, job, "Minutes", null, "Summary", "{\"decisions\":[]}"));
        Map<String, Object> update = new HashMap<>();
        update.put("title", "Edited");
        update.put("fullSummary", "Edited summary");

        mockMvc.perform(put("/api/meetings/" + meetingId + "/minutes")
                .header("Authorization", "Bearer " + viewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
            .andExpect(status().isForbidden());
    }

    private void register(String email, String name) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", "password123");
        request.put("name", name);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
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

    private void invite(String accessToken, Long teamId, String email, String role) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("role", role);
        mockMvc.perform(post("/api/teams/" + teamId + "/members")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());
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
