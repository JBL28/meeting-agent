package com.ssafy.meeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhaseOneApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginMeCreateTeamAndViewerDeleteIsForbidden() throws Exception {
        register("owner1@example.com", "Owner");
        register("viewer1@example.com", "Viewer");

        String ownerToken = login("owner1@example.com");
        String viewerToken = login("viewer1@example.com");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.email").value("owner1@example.com"));

        Long teamId = createTeam(ownerToken, "Phase 1 Team");
        invite(ownerToken, teamId, "viewer1@example.com", "VIEWER");

        mockMvc.perform(get("/api/teams/" + teamId + "/members").header("Authorization", "Bearer " + viewerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[?(@.email == 'viewer1@example.com')].role").value("VIEWER"));

        mockMvc.perform(delete("/api/teams/" + teamId).header("Authorization", "Bearer " + viewerToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false));
    }

    private void register(String email, String name) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", "password123");
        request.put("name", name);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value(email));
    }

    private String login(String email) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("password", "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").isString())
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
            .andExpect(jsonPath("$.data.myRole").value("OWNER"))
            .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        Long teamId = json.at("/data/id").asLong();
        assertThat(teamId).isPositive();
        return teamId;
    }

    private void invite(String accessToken, Long teamId, String email, String role) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("email", email);
        request.put("role", role);
        mockMvc.perform(post("/api/teams/" + teamId + "/members")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.email").value(email))
            .andExpect(jsonPath("$.data.role").value(role));
    }
}
