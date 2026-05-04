package com.ssafy.meeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhaseTwoAApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void voiceSampleAndMeetingAudioFlowMatchesPhaseTwoAAcceptance() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String ownerEmail = "phase2a-owner-" + suffix + "@example.com";
        String viewerEmail = "phase2a-viewer-" + suffix + "@example.com";
        Long ownerId = register(ownerEmail, "Owner");
        register(viewerEmail, "Viewer");
        String ownerToken = login(ownerEmail);
        String viewerToken = login(viewerEmail);
        Long teamId = createTeam(ownerToken, "Phase 2A Team");
        invite(ownerToken, teamId, viewerEmail, "VIEWER");

        MockMultipartFile voice = new MockMultipartFile("file", "sample.mp3", "audio/mpeg", new byte[] {1, 2, 3});
        mockMvc.perform(multipart("/api/teams/" + teamId + "/members/" + ownerId + "/voice-samples")
                .file(voice)
                .param("consent", "false")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isBadRequest());

        MvcResult sampleResult = mockMvc.perform(multipart("/api/teams/" + teamId + "/members/" + ownerId + "/voice-samples")
                .file(voice)
                .param("consent", "true")
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andReturn();
        Long sampleId = objectMapper.readTree(sampleResult.getResponse().getContentAsString()).at("/data/id").asLong();

        mockMvc.perform(delete("/api/voice-samples/" + sampleId).header("Authorization", "Bearer " + viewerToken))
            .andExpect(status().isForbidden());

        Long meetingId = createMeeting(ownerToken, teamId, "Phase 2A Meeting");
        MockMultipartFile audio = new MockMultipartFile("file", "meeting.mp3", "audio/mpeg", new byte[] {4, 5, 6});
        MvcResult audioResult = mockMvc.perform(multipart("/api/meetings/" + meetingId + "/audio")
                .file(audio)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.meetingId").value(meetingId))
            .andReturn();
        Long audioFileId = objectMapper.readTree(audioResult.getResponse().getContentAsString()).at("/data/id").asLong();
        assertThat(audioFileId).isPositive();

        mockMvc.perform(get("/api/meetings/" + meetingId).header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("RECORDED"));

        mockMvc.perform(get("/api/audio/" + audioFileId + "/stream"))
            .andExpect(status().isUnauthorized());
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
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).at("/data/id").asLong();
    }
}
