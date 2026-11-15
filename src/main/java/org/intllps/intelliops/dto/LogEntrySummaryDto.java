package org.intllps.intelliops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record LogEntrySummaryDto(
        Long id,
        @NotBlank(message = "Raw message is required") String rawMessage,
        @NotNull(message = "Timestamp is required") OffsetDateTime timestamp,
        String logLevel,
        String serviceName,
        String traceId,
        String messageSummary
) {
}

