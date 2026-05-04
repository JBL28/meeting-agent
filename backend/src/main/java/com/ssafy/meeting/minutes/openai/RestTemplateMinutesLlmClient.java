package com.ssafy.meeting.minutes.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.meeting.common.exception.ValidationException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class RestTemplateMinutesLlmClient implements MinutesLlmClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public RestTemplateMinutesLlmClient(
        ObjectMapper objectMapper,
        @Value("${openai.api-key:}") String apiKey,
        @Value("${openai.minutes-model:gpt-4.1}") String model
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public String generate(String transcript) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ValidationException("OPENAI_API_KEY is required for minutes generation");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);
        request.put("input", Arrays.asList(
            message("system", "Generate Korean meeting minutes as strict JSON matching the schema. Use null for unknown due dates."),
            message("user", transcript)
        ));
        request.put("text", Collections.singletonMap("format", schemaFormat()));
        try {
            log.info("OpenAI minutes generation request model={} transcriptChars={}", model, transcript.length());
            String response = restTemplate.postForObject("https://api.openai.com/v1/responses", new HttpEntity<>(request, headers), String.class);
            return extractOutputText(response);
        } catch (RestClientException exception) {
            throw new ValidationException("OpenAI minutes generation failed: " + exception.getMessage());
        }
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private Map<String, Object> schemaFormat() {
        Map<String, Object> format = new HashMap<>();
        format.put("type", "json_schema");
        format.put("name", "meeting_minutes");
        format.put("strict", true);
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", Arrays.asList("title", "full_summary", "member_summaries", "decisions", "action_items"));
        Map<String, Object> properties = new HashMap<>();
        properties.put("title", stringSchema());
        properties.put("full_summary", stringSchema());
        properties.put("member_summaries", arrayOf(objectSchema(
            Arrays.asList("member_name", "progress", "issues", "next_tasks"),
            propMap("member_name", stringSchema(), "progress", stringSchema(), "issues", stringSchema(), "next_tasks", stringSchema())
        )));
        properties.put("decisions", arrayOf(stringSchema()));
        properties.put("action_items", arrayOf(objectSchema(
            Arrays.asList("assignee", "content", "due_date"),
            propMap("assignee", stringSchema(), "content", stringSchema(), "due_date", nullableStringSchema())
        )));
        schema.put("properties", properties);
        format.put("schema", schema);
        return format;
    }

    private String extractOutputText(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            if (root.hasNonNull("output_text")) {
                return root.path("output_text").asText();
            }
            for (JsonNode output : root.path("output")) {
                for (JsonNode content : output.path("content")) {
                    if (content.hasNonNull("text")) {
                        return content.path("text").asText();
                    }
                }
            }
            throw new ValidationException("OpenAI response did not include output_text");
        } catch (Exception exception) {
            if (exception instanceof ValidationException) {
                throw (ValidationException) exception;
            }
            throw new ValidationException("Could not parse OpenAI minutes response");
        }
    }

    private Map<String, Object> stringSchema() {
        return Collections.singletonMap("type", "string");
    }

    private Map<String, Object> nullableStringSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", Arrays.asList("string", "null"));
        return schema;
    }

    private Map<String, Object> arrayOf(Map<String, Object> itemSchema) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "array");
        schema.put("items", itemSchema);
        return schema;
    }

    private Map<String, Object> objectSchema(java.util.List<String> required, Map<String, Object> properties) {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", required);
        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> propMap(Object... values) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            map.put((String) values[i], values[i + 1]);
        }
        return map;
    }
}
