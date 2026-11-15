package org.intllps.intelliops.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.intllps.intelliops.entity.IncidentSeverity;
import org.intllps.intelliops.entity.IncidentStatus;

import java.time.OffsetDateTime;

public record IncidentSummaryDto(
        Long id,
        @NotBlank(message = "Title is required") String title,
        @NotNull(message = "Severity is required") IncidentSeverity severity,
        @NotNull(message = "Status is required") IncidentStatus status,
        OffsetDateTime firstSeenAt,
        OffsetDateTime lastSeenAt,
        OffsetDateTime createdAt
) {
}

