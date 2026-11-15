package org.intllps.intelliops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intllps.intelliops.config.AiProperties;
import org.intllps.intelliops.dto.AiAnalysisRequest;
import org.intllps.intelliops.dto.AiAnalysisResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IncidentAiAnalysisServiceTest {

    @Test
    void shouldParseStructuredAiResponse() {
        ChatModel chatModel = mock(ChatModel.class);
        AiProperties properties = new AiProperties();
        properties.setApiKey("test-key");
        properties.setModel("gpt-4o-mini");

        var promptTemplate = new org.springframework.ai.chat.prompt.PromptTemplate(
                "Incident ID: {incidentId}\nContext: {incidentContext}\nEvidence: {evidence}\n"
        );

        IncidentAiAnalysisService service = new IncidentAiAnalysisService(chatModel, new ObjectMapper(), properties, promptTemplate);

        String json = "{\n" +
                "  \"summary\": \"Database timeout appears to be the primary issue\",\n" +
                "  \"suspectedRootCause\": \"The payments service is timing out while talking to Postgres\",\n" +
                "  \"supportingEvidence\": \"Multiple ERROR logs show connection timeouts and retries\",\n" +
                "  \"potentialImpact\": \"Orders may fail for some customers\",\n" +
                "  \"recommendedTroubleshootingSteps\": \"Check DB connectivity, examine recent migrations and pool settings\",\n" +
                "  \"confidenceLevel\": \"MEDIUM\"\n" +
                "}";

        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(json)))));

        AiAnalysisResult result = service.analyzeIncident(new AiAnalysisRequest(
                "INC-1",
                "Repeated payment failures in payments-service",
                List.of("2026-11-14T10:00:00Z ERROR payments-service --- Connection timeout", "2026-11-14T10:00:05Z ERROR payments-service --- Retrying request")
        ));

        assertEquals("Database timeout appears to be the primary issue", result.summary());
        assertEquals("MEDIUM", result.confidenceLevel());
    }

    @Test
    void shouldFailWhenApiKeyMissing() {
        ChatModel chatModel = mock(ChatModel.class);
        AiProperties properties = new AiProperties();
        properties.setApiKey(null);

        var promptTemplate = new org.springframework.ai.chat.prompt.PromptTemplate("Context: {incidentContext}");
        IncidentAiAnalysisService service = new IncidentAiAnalysisService(chatModel, new ObjectMapper(), properties, promptTemplate);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.analyzeIncident(new AiAnalysisRequest("INC-1", "Incident context", List.of())));

        assertTrue(ex.getMessage().contains("API key"));
    }

    @Test
    void shouldFailOnInvalidAiResponse() {
        ChatModel chatModel = mock(ChatModel.class);
        AiProperties properties = new AiProperties();
        properties.setApiKey("test-key");

        var promptTemplate = new org.springframework.ai.chat.prompt.PromptTemplate("Context: {incidentContext}");
        IncidentAiAnalysisService service = new IncidentAiAnalysisService(chatModel, new ObjectMapper(), properties, promptTemplate);

        when(chatModel.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("not-json")))));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.analyzeIncident(new AiAnalysisRequest("INC-1", "Incident context", List.of("evidence"))));

        assertTrue(ex.getMessage().contains("valid JSON"));
    }
}
