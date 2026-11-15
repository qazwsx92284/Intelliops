package org.intllps.intelliops.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "log_entries", indexes = {
        @Index(name = "idx_log_entry_incident", columnList = "incident_id"),
        @Index(name = "idx_log_entry_timestamp", columnList = "timestamp"),
        @Index(name = "idx_log_entry_service", columnList = "service_name")
})
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @NotBlank
    @Column(nullable = false, length = 4096)
    private String rawMessage;

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    @Size(max = 255)
    @Column(name = "log_level")
    private String logLevel;

    @Size(max = 255)
    @Column(name = "service_name")
    private String serviceName;

    @Size(max = 255)
    @Column(name = "trace_id")
    private String traceId;

    @Size(max = 2048)
    @Column(name = "message_summary", length = 2048)
    private String messageSummary;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LogEntry() {
    }

    public LogEntry(String rawMessage, OffsetDateTime timestamp, String logLevel, String serviceName, String traceId, String messageSummary) {
        this.rawMessage = rawMessage;
        this.timestamp = timestamp;
        this.logLevel = logLevel;
        this.serviceName = serviceName;
        this.traceId = traceId;
        this.messageSummary = messageSummary;
    }

    public Long getId() {
        return id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getMessageSummary() {
        return messageSummary;
    }

    public void setMessageSummary(String messageSummary) {
        this.messageSummary = messageSummary;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

