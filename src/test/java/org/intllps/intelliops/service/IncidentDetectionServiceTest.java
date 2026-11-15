package org.intllps.intelliops.service;

import org.intllps.intelliops.config.IncidentDetectionProperties;
import org.intllps.intelliops.dto.ParsedLogEntry;
import org.intllps.intelliops.entity.Incident;
import org.intllps.intelliops.entity.IncidentSeverity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IncidentDetectionServiceTest {

    private final IncidentDetectionProperties properties = new IncidentDetectionProperties();
    private final IncidentDetectionService incidentDetectionService = new IncidentDetectionService(properties);

    @Test
    void shouldReturnEmptyListForNormalLogs() {
        List<ParsedLogEntry> entries = List.of(
                new ParsedLogEntry("2026-11-14T10:00:00Z INFO api-gateway --- Request processed", OffsetDateTime.parse("2026-11-14T10:00:00Z"), "INFO", "api-gateway", null, "Request processed", null, true),
                new ParsedLogEntry("2026-11-14T10:00:30Z WARN billing-service --- Retry scheduled", OffsetDateTime.parse("2026-11-14T10:00:30Z"), "WARN", "billing-service", null, "Retry scheduled", null, true)
        );

        List<Incident> incidents = incidentDetectionService.detect(entries);

        assertTrue(incidents.isEmpty());
    }

    @Test
    void shouldNotCreateIncidentForIsolatedError() {
        List<ParsedLogEntry> entries = List.of(
                new ParsedLogEntry("2026-11-14T10:00:00Z ERROR payments-service --- Payment request failed", OffsetDateTime.parse("2026-11-14T10:00:00Z"), "ERROR", "payments-service", null, "Payment request failed", "java.lang.IllegalStateException: payment failed", true)
        );

        List<Incident> incidents = incidentDetectionService.detect(entries);

        assertTrue(incidents.isEmpty());
    }

    @Test
    void shouldCreateIncidentForRepeatedErrors() {
        List<ParsedLogEntry> entries = List.of(
                new ParsedLogEntry("2026-11-14T10:00:00Z ERROR payments-service --- Payment request failed", OffsetDateTime.parse("2026-11-14T10:00:00Z"), "ERROR", "payments-service", null, "Payment request failed", "java.lang.IllegalStateException: payment failed", true),
                new ParsedLogEntry("2026-11-14T10:01:00Z ERROR payments-service --- Payment request failed", OffsetDateTime.parse("2026-11-14T10:01:00Z"), "ERROR", "payments-service", null, "Payment request failed", "java.lang.IllegalStateException: payment failed", true),
                new ParsedLogEntry("2026-11-14T10:02:00Z ERROR payments-service --- Payment request failed", OffsetDateTime.parse("2026-11-14T10:02:00Z"), "ERROR", "payments-service", null, "Payment request failed", "java.lang.IllegalStateException: payment failed", true)
        );

        List<Incident> incidents = incidentDetectionService.detect(entries);

        assertEquals(1, incidents.size());
        assertEquals(IncidentSeverity.MEDIUM, incidents.getFirst().getSeverity());
    }

    @Test
    void shouldCreateIncidentForSharedTraceId() {
        List<ParsedLogEntry> entries = List.of(
                new ParsedLogEntry("2026-11-14T10:00:00Z ERROR api-gateway --- Request failed", OffsetDateTime.parse("2026-11-14T10:00:00Z"), "ERROR", "api-gateway", "abc-123", "Request failed", "java.lang.RuntimeException: downstream timeout", true),
                new ParsedLogEntry("2026-11-14T10:00:05Z ERROR billing-service --- Processing failed for request", OffsetDateTime.parse("2026-11-14T10:00:05Z"), "ERROR", "billing-service", "abc-123", "Processing failed for request", "java.lang.RuntimeException: downstream timeout", true),
                new ParsedLogEntry("2026-11-14T10:00:10Z ERROR inventory-service --- Retry triggered", OffsetDateTime.parse("2026-11-14T10:00:10Z"), "ERROR", "inventory-service", "abc-123", "Retry triggered", "java.lang.RuntimeException: downstream timeout", true)
        );

        List<Incident> incidents = incidentDetectionService.detect(entries);

        assertEquals(1, incidents.size());
        assertEquals(IncidentSeverity.HIGH, incidents.getFirst().getSeverity());
    }

    @Test
    void shouldRespectConfigurableThresholds() {
        IncidentDetectionProperties custom = new IncidentDetectionProperties();
        custom.setMinErrorCount(4);
        custom.setTimeWindowMinutes(5);
        custom.setTraceIdThreshold(3);
        custom.setRepeatedExceptionThreshold(4);

        IncidentDetectionService customService = new IncidentDetectionService(custom);

        List<ParsedLogEntry> entries = List.of(
                new ParsedLogEntry("2026-11-14T10:00:00Z ERROR service-a --- First failure", OffsetDateTime.parse("2026-11-14T10:00:00Z"), "ERROR", "service-a", null, "First failure", "java.lang.RuntimeException: timeout", true),
                new ParsedLogEntry("2026-11-14T10:01:00Z ERROR service-a --- Second failure", OffsetDateTime.parse("2026-11-14T10:01:00Z"), "ERROR", "service-a", null, "Second failure", "java.lang.RuntimeException: timeout", true),
                new ParsedLogEntry("2026-11-14T10:02:00Z ERROR service-a --- Third failure", OffsetDateTime.parse("2026-11-14T10:02:00Z"), "ERROR", "service-a", null, "Third failure", "java.lang.RuntimeException: timeout", true)
        );

        List<Incident> incidents = customService.detect(entries);

        assertTrue(incidents.isEmpty());
    }
}
