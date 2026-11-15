package org.intllps.intelliops.service;

import org.intllps.intelliops.dto.IncidentDetailDto;
import org.intllps.intelliops.dto.IncidentSummaryDto;
import org.intllps.intelliops.dto.LogEntrySummaryDto;
import org.intllps.intelliops.entity.Incident;
import org.intllps.intelliops.entity.LogEntry;
import org.intllps.intelliops.exception.ApiException;
import org.intllps.intelliops.repository.IncidentRepository;
import org.intllps.intelliops.repository.LogEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final LogEntryRepository logEntryRepository;

    public IncidentService(IncidentRepository incidentRepository, LogEntryRepository logEntryRepository) {
        this.incidentRepository = incidentRepository;
        this.logEntryRepository = logEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<IncidentSummaryDto> getAllIncidents() {
        return incidentRepository.findAll().stream()
                .map(this::toSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IncidentDetailDto getIncidentById(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ApiException("Incident not found with id: " + id));

        List<LogEntrySummaryDto> logs = logEntryRepository.findByIncidentId(id).stream()
                .map(this::toLogEntryDto)
                .collect(Collectors.toList());

        return new IncidentDetailDto(
                incident.getId(),
                incident.getTitle(),
                incident.getSummary(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getFirstSeenAt(),
                incident.getLastSeenAt(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                logs
        );
    }

    private IncidentSummaryDto toSummaryDto(Incident incident) {
        return new IncidentSummaryDto(
                incident.getId(),
                incident.getTitle(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getFirstSeenAt(),
                incident.getLastSeenAt(),
                incident.getCreatedAt()
        );
    }

    private LogEntrySummaryDto toLogEntryDto(LogEntry logEntry) {
        return new LogEntrySummaryDto(
                logEntry.getId(),
                logEntry.getRawMessage(),
                logEntry.getTimestamp(),
                logEntry.getLogLevel(),
                logEntry.getServiceName(),
                logEntry.getTraceId(),
                logEntry.getMessageSummary()
        );
    }
}

