package org.intllps.intelliops.dto;

import java.time.OffsetDateTime;
import java.util.Objects;

public record ParsedLogEntry(
        String originalContent,
        OffsetDateTime timestamp,
        String logLevel,
        String serviceName,
        String traceId,
        String message,
        String exceptionInformation,
        boolean parsedSuccessfully
) {

    public ParsedLogEntry {
        originalContent = Objects.requireNonNullElse(originalContent, "");
        logLevel = logLevel == null ? null : logLevel.toUpperCase();
    }

    public ParsedLogEntry withExceptionInformation(String additionalException) {
        String combined = exceptionInformation == null || exceptionInformation.isBlank()
                ? additionalException
                : exceptionInformation + System.lineSeparator() + additionalException;
        return new ParsedLogEntry(originalContent, timestamp, logLevel, serviceName, traceId, message, combined, parsedSuccessfully);
    }
}

