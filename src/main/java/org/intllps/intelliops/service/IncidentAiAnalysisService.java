package org.intllps.intelliops.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intllps.intelliops.config.AiProperties;
import org.intllps.intelliops.dto.AiAnalysisRequest;
import org.intllps.intelliops.dto.AiAnalysisResult;
import org.intllps.intelliops.entity.AiAnalysis;
import org.intllps.intelliops.entity.Incident;
import org.intllps.intelliops.repository.AiAnalysisRepository;
import org.intllps.intelliops.repository.IncidentRepository;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class IncidentAiAnalysisService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final AiProperties aiProperties;
    private final PromptTemplate promptTemplate;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final IncidentRepository incidentRepository;

    public IncidentAiAnalysisService(
            @Qualifier("chatModel") ChatModel chatModel,
            ObjectMapper objectMapper,
            AiProperties aiProperties,
            PromptTemplate promptTemplate
    ) {
        this(chatModel, objectMapper, aiProperties, promptTemplate, null, null);
    }

    @Autowired
    public IncidentAiAnalysisService(
            @Qualifier("chatModel") ChatModel chatModel,
            ObjectMapper objectMapper,
            AiProperties aiProperties,
            PromptTemplate promptTemplate,
            AiAnalysisRepository aiAnalysisRepository,
            IncidentRepository incidentRepository
    ) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.aiProperties = aiProperties;
        this.promptTemplate = promptTemplate;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.incidentRepository = incidentRepository;
    }

    public AiAnalysis saveAnalysis(Long incidentId, AiAnalysisRequest request) {
        if (incidentRepository == null || aiAnalysisRepository == null) {
            throw new IllegalStateException("AI analysis persistence is not configured");
        }
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found with id: " + incidentId));

        AiAnalysisResult result = analyzeIncident(request);
        AiAnalysis analysis = new AiAnalysis(
                incident,
                result.summary(),
                result.suspectedRootCause(),
                result.supportingEvidence(),
                result.potentialImpact(),
                result.recommendedTroubleshootingSteps(),
                result.confidenceLevel(),
                aiProperties.getModel(),
                ""
        );
        return aiAnalysisRepository.save(analysis);
    }

    public AiAnalysisResult analyzeIncident(AiAnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Analysis request cannot be null");
        }
        if (request.incidentContext() == null || request.incidentContext().isBlank()) {
            throw new IllegalArgumentException("Incident context is required");
        }
        if (aiProperties.getApiKey() == null || aiProperties.getApiKey().isBlank()) {
            throw new IllegalStateException("AI API key is not configured. Set INTELLIOPS_AI_API_KEY or OPENAI_API_KEY.");
        }

        String promptText = buildPrompt(request);
        try {
            ChatResponse response = chatModel.call(new Prompt(promptText));
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                throw new IllegalStateException("AI response was empty");
            }
            String content = response.getResult().getOutput().getText();
            return parseResponse(content);
        } catch (RestClientException ex) {
            throw new IllegalStateException("AI request timed out or the provider was unreachable: " + ex.getMessage(), ex);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to analyze incident with AI model: " + ex.getMessage(), ex);
        }
    }

    public String buildPrompt(AiAnalysisRequest request) {
        String evidence = buildRelevantEvidence(request);

        Map<String, Object> variables = Map.of(
                "incidentId", request.incidentId(),
                "incidentContext", request.incidentContext(),
                "evidence", evidence,
                "modelName", aiProperties.getModel()
        );

        return promptTemplate.render(variables);
    }

    public AiAnalysisResult parseResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new IllegalStateException("AI response is empty");
        }

        try {
            String normalized = extractJson(rawResponse);
            AiAnalysisResult result = objectMapper.readValue(normalized, AiAnalysisResult.class);
            validateResult(result);
            return result;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("AI response was not valid JSON: " + ex.getMessage(), ex);
        }
    }

    private String buildRelevantEvidence(AiAnalysisRequest request) {
        if (request.relevantEvidence() == null || request.relevantEvidence().isEmpty()) {
            return "No specific evidence was selected for analysis.";
        }

        List<String> sanitized = new ArrayList<>();
        for (String evidence : request.relevantEvidence()) {
            if (evidence == null || evidence.isBlank()) {
                continue;
            }
            sanitized.add(evidence.trim());
        }

        return sanitized.isEmpty() ? "No specific evidence was selected for analysis." : String.join(System.lineSeparator(), sanitized);
    }

    private String extractJson(String rawResponse) {
        String trimmed = rawResponse.trim();
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private void validateResult(AiAnalysisResult result) {
        if (result == null) {
            throw new IllegalStateException("AI response parsing produced no result");
        }

        if (result.summary() == null || result.summary().isBlank()) {
            throw new IllegalStateException("AI response missing summary");
        }
        if (result.suspectedRootCause() == null || result.suspectedRootCause().isBlank()) {
            throw new IllegalStateException("AI response missing suspected root cause");
        }
        if (matchesConfirmedDiagnosis(result.suspectedRootCause())) {
            throw new IllegalStateException("AI response must describe a suspected root cause, not a confirmed diagnosis");
        }
        if (result.supportingEvidence() == null || result.supportingEvidence().isBlank()) {
            throw new IllegalStateException("AI response missing supporting evidence");
        }
        if (result.potentialImpact() == null || result.potentialImpact().isBlank()) {
            throw new IllegalStateException("AI response missing impact");
        }
        if (result.recommendedTroubleshootingSteps() == null || result.recommendedTroubleshootingSteps().isBlank()) {
            throw new IllegalStateException("AI response missing troubleshooting steps");
        }
        if (result.confidenceLevel() == null || result.confidenceLevel().isBlank()) {
            throw new IllegalStateException("AI response missing confidence");
        }
    }

    private boolean matchesConfirmedDiagnosis(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase();
        return lower.contains("confirmed")
                || lower.contains("is the root cause")
                || lower.contains("definitively caused")
                || lower.contains("was caused by")
                || lower.contains("diagnosis is")
                || lower.contains("the issue is definitely");
    }
}
