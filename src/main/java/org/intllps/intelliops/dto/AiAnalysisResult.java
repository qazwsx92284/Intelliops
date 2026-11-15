package org.intllps.intelliops.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record AiAnalysisResult(
        @NotBlank(message = "Incident summary is required") String summary,
        @NotBlank(message = "Suspected root cause is required") String suspectedRootCause,
        @NotBlank(message = "Supporting evidence is required") String supportingEvidence,
        @NotBlank(message = "Potential impact is required") String potentialImpact,
        @NotBlank(message = "Recommended troubleshooting steps are required") String recommendedTroubleshootingSteps,
        @NotBlank(message = "Confidence level is required") String confidenceLevel
) {
    public AiAnalysisResult {
        if (recommendationsIsEmpty(recommendedTroubleshootingSteps)) {
            throw new IllegalArgumentException("Recommended troubleshooting steps cannot be blank");
        }
    }

    private static boolean recommendationsIsEmpty(String value) {
        return value == null || value.isBlank();
    }
}

