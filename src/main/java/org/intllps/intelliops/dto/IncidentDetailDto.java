package org.intllps.intelliops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.intllps.intelliops.entity.IncidentSeverity;
import org.intllps.intelliops.entity.IncidentStatus;

import java.time.OffsetDateTime;
import java.util.List;

public record IncidentDetailDto(
        Long id,
        @NotBlank(message = "Title is required") String title,
        String summary,
        @NotNull(message = "Severity is required") IncidentSeverity severity,
        @NotNull(message = "Status is required") IncidentStatus status,
        OffsetDateTime firstSeenAt,
        OffsetDateTime lastSeenAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<LogEntrySummaryDto> logEntries
) {
}

