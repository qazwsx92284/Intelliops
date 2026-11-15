package org.intllps.intelliops.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AiAnalysisRequest(
        @NotBlank(message = "Incident identifier is required") String incidentId,
        @NotBlank(message = "Incident context is required") String incidentContext,
        List<String> relevantEvidence
) {
}

