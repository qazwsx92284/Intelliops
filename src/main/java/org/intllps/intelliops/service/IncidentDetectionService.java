package org.intllps.intelliops.service;

import org.intllps.intelliops.config.IncidentDetectionProperties;
import org.intllps.intelliops.dto.ParsedLogEntry;
import org.intllps.intelliops.entity.Incident;
import org.intllps.intelliops.entity.IncidentSeverity;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IncidentDetectionService {

    private final IncidentDetectionProperties properties;

    public IncidentDetectionService(IncidentDetectionProperties properties) {
        this.properties = properties;
    }

    public List<Incident> detect(List<ParsedLogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        List<ParsedLogEntry> validEntries = entries.stream()
                .filter(Objects::nonNull)
                .filter(ParsedLogEntry::parsedSuccessfully)
                .filter(entry -> entry.logLevel() != null && isErrorLevel(entry.logLevel()))
                .filter(entry -> entry.timestamp() != null)
                .sorted(Comparator.comparing(ParsedLogEntry::timestamp))
                .toList();

        if (validEntries.isEmpty()) {
            return List.of();
        }

        List<List<ParsedLogEntry>> candidateClusters = new ArrayList<>();

        Map<String, List<ParsedLogEntry>> groupedByTrace = validEntries.stream()
                .filter(entry -> entry.traceId() != null && !entry.traceId().isBlank())
                .collect(Collectors.groupingBy(ParsedLogEntry::traceId, LinkedHashMap::new, Collectors.toList()));

        for (List<ParsedLogEntry> cluster : groupedByTrace.values()) {
            if (cluster.size() >= properties.getTraceIdThreshold()) {
                candidateClusters.add(cluster);
            }
        }

        List<ParsedLogEntry> timeWindowEntries = findEntriesWithinWindow(validEntries, properties.getTimeWindowMinutes());
        if (timeWindowEntries.size() >= properties.getMinErrorCount()) {
            candidateClusters.add(timeWindowEntries);
        }

        Map<String, List<ParsedLogEntry>> repeatedExceptions = validEntries.stream()
                .filter(entry -> entry.exceptionInformation() != null && !entry.exceptionInformation().isBlank())
                .collect(Collectors.groupingBy(ParsedLogEntry::exceptionInformation, LinkedHashMap::new, Collectors.toList()));

        for (List<ParsedLogEntry> cluster : repeatedExceptions.values()) {
            if (cluster.size() >= properties.getRepeatedExceptionThreshold()) {
                candidateClusters.add(cluster);
            }
        }

        List<List<ParsedLogEntry>> mergedClusters = mergeClusters(candidateClusters);
        return mergedClusters.stream()
                .map(this::buildIncident)
                .sorted(Comparator.comparing(Incident::getFirstSeenAt))
                .toList();
    }

    private List<ParsedLogEntry> findEntriesWithinWindow(List<ParsedLogEntry> entries, int timeWindowMinutes) {
        if (entries.isEmpty()) {
            return List.of();
        }

        List<ParsedLogEntry> windowEntries = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            ParsedLogEntry current = entries.get(i);
            OffsetDateTime windowStart = current.timestamp().minusMinutes(timeWindowMinutes);
            List<ParsedLogEntry> matching = entries.stream()
                    .filter(entry -> !entry.timestamp().isBefore(windowStart) && !entry.timestamp().isAfter(current.timestamp()))
                    .toList();

            if (matching.size() >= properties.getMinErrorCount()) {
                windowEntries.addAll(matching);
                break;
            }
        }

        return windowEntries.stream()
                .distinct()
                .sorted(Comparator.comparing(ParsedLogEntry::timestamp))
                .toList();
    }

    private List<List<ParsedLogEntry>> mergeClusters(List<List<ParsedLogEntry>> clusters) {
        List<List<ParsedLogEntry>> merged = new ArrayList<>();

        for (List<ParsedLogEntry> cluster : clusters) {
            boolean mergedWithExisting = false;
            for (int i = 0; i < merged.size(); i++) {
                List<ParsedLogEntry> existing = merged.get(i);
                if (clustersOverlap(existing, cluster)) {
                    Set<ParsedLogEntry> combined = new LinkedHashSet<>();
                    combined.addAll(existing);
                    combined.addAll(cluster);
                    merged.set(i, combined.stream().sorted(Comparator.comparing(ParsedLogEntry::timestamp)).toList());
                    mergedWithExisting = true;
                    break;
                }
            }
            if (!mergedWithExisting) {
                merged.add(new ArrayList<>(cluster));
            }
        }

        return merged;
    }

    private boolean clustersOverlap(List<ParsedLogEntry> left, List<ParsedLogEntry> right) {
        Set<String> leftTraceIds = left.stream().map(ParsedLogEntry::traceId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> rightTraceIds = right.stream().map(ParsedLogEntry::traceId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (leftTraceIds.stream().anyMatch(rightTraceIds::contains)) {
            return true;
        }

        Set<String> leftExceptions = left.stream().map(ParsedLogEntry::exceptionInformation).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> rightExceptions = right.stream().map(ParsedLogEntry::exceptionInformation).filter(Objects::nonNull).collect(Collectors.toSet());
        if (leftExceptions.stream().anyMatch(rightExceptions::contains)) {
            return true;
        }

        Set<String> leftServices = left.stream().map(ParsedLogEntry::serviceName).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> rightServices = right.stream().map(ParsedLogEntry::serviceName).filter(Objects::nonNull).collect(Collectors.toSet());
        if (leftServices.stream().anyMatch(rightServices::contains)) {
            return true;
        }

        OffsetDateTime leftStart = left.stream().map(ParsedLogEntry::timestamp).min(Comparator.naturalOrder()).orElse(OffsetDateTime.MIN);
        OffsetDateTime leftEnd = left.stream().map(ParsedLogEntry::timestamp).max(Comparator.naturalOrder()).orElse(OffsetDateTime.MAX);
        OffsetDateTime rightStart = right.stream().map(ParsedLogEntry::timestamp).min(Comparator.naturalOrder()).orElse(OffsetDateTime.MIN);
        OffsetDateTime rightEnd = right.stream().map(ParsedLogEntry::timestamp).max(Comparator.naturalOrder()).orElse(OffsetDateTime.MAX);

        return !leftEnd.isBefore(rightStart) && !rightEnd.isBefore(leftStart);
    }

    private Incident buildIncident(List<ParsedLogEntry> entries) {
        List<ParsedLogEntry> ordered = entries.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ParsedLogEntry::timestamp))
                .toList();

        if (ordered.isEmpty()) {
            throw new IllegalArgumentException("Incident evidence cannot be empty.");
        }

        OffsetDateTime firstSeen = ordered.getFirst().timestamp();
        OffsetDateTime lastSeen = ordered.getLast().timestamp();
        IncidentSeverity severity = determineSeverity(ordered);

        String title = determineTitle(ordered);
        String summary = buildSummary(ordered);

        Incident incident = new Incident(title, summary, severity, firstSeen, lastSeen);
        incident.setStatus(org.intllps.intelliops.entity.IncidentStatus.OPEN);
        return incident;
    }

    private String determineTitle(List<ParsedLogEntry> entries) {
        long distinctTraceIds = entries.stream().map(ParsedLogEntry::traceId).filter(Objects::nonNull).distinct().count();
        long distinctExceptions = entries.stream().map(ParsedLogEntry::exceptionInformation).filter(Objects::nonNull).distinct().count();

        if (distinctTraceIds > 0) {
            return "Trace-linked errors";
        }
        if (distinctExceptions > 0) {
            return "Repeated exception pattern";
        }
        return "Error burst in application logs";
    }

    private String buildSummary(List<ParsedLogEntry> entries) {
        String service = entries.stream()
                .map(ParsedLogEntry::serviceName)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("unknown-service");

        String message = entries.stream()
                .map(ParsedLogEntry::message)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("Multiple error events detected.");

        return "Detected " + entries.size() + " related error events in service " + service + ". Latest evidence: " + message;
    }

    private IncidentSeverity determineSeverity(List<ParsedLogEntry> entries) {
        long totalErrors = entries.size();
        long uniqueServices = entries.stream().map(ParsedLogEntry::serviceName).filter(Objects::nonNull).distinct().count();
        boolean hasFatal = entries.stream().anyMatch(entry -> entry.exceptionInformation() != null && entry.exceptionInformation().toLowerCase().contains("fatal"));
        long distinctTraceIds = entries.stream().map(ParsedLogEntry::traceId).filter(Objects::nonNull).distinct().count();

        if (hasFatal || totalErrors >= 10) {
            return IncidentSeverity.CRITICAL;
        }
        if (totalErrors >= 5 || distinctTraceIds > 0 || uniqueServices >= 3 && totalErrors >= 4) {
            return IncidentSeverity.HIGH;
        }
        if (totalErrors >= 3) {
            return IncidentSeverity.MEDIUM;
        }
        return IncidentSeverity.LOW;
    }

    private boolean isErrorLevel(String level) {
        if (level == null) {
            return false;
        }
        return switch (level.toUpperCase()) {
            case "ERROR", "FATAL", "WARN" -> true;
            default -> false;
        };
    }
}
