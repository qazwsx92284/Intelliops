package org.intllps.intelliops.controller;

import org.intllps.intelliops.dto.AiAnalysisRequest;
import org.intllps.intelliops.dto.DashboardStats;
import org.intllps.intelliops.dto.IncidentDetailDto;
import org.intllps.intelliops.dto.IncidentSummaryDto;
import org.intllps.intelliops.dto.LogEntrySummaryDto;
import org.intllps.intelliops.dto.ParsedLogEntry;
import org.intllps.intelliops.entity.AiAnalysis;
import org.intllps.intelliops.entity.Incident;
import org.intllps.intelliops.entity.LogEntry;
import org.intllps.intelliops.repository.AiAnalysisRepository;
import org.intllps.intelliops.repository.IncidentRepository;
import org.intllps.intelliops.service.IncidentAiAnalysisService;
import org.intllps.intelliops.service.IncidentDetectionService;
import org.intllps.intelliops.service.IncidentService;
import org.intllps.intelliops.service.LogParserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final IncidentService incidentService;
    private final LogParserService logParserService;
    private final IncidentDetectionService incidentDetectionService;
    private final IncidentRepository incidentRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final IncidentAiAnalysisService aiAnalysisService;

    public DashboardController(
            IncidentService incidentService,
            LogParserService logParserService,
            IncidentDetectionService incidentDetectionService,
            IncidentRepository incidentRepository,
            AiAnalysisRepository aiAnalysisRepository,
            IncidentAiAnalysisService aiAnalysisService
    ) {
        this.incidentService = incidentService;
        this.logParserService = logParserService;
        this.incidentDetectionService = incidentDetectionService;
        this.incidentRepository = incidentRepository;
        this.aiAnalysisRepository = aiAnalysisRepository;
        this.aiAnalysisService = aiAnalysisService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        try {
            List<IncidentSummaryDto> incidents = incidentService.getAllIncidents();
            model.addAttribute("incidents", incidents);
            model.addAttribute("stats", buildDashboardStats(incidents));
        } catch (Exception ex) {
            model.addAttribute("incidents", List.of());
            model.addAttribute("stats", new DashboardStats(0, 0, 0, 0, 0));
            model.addAttribute("errorMessage", "Unable to load incident data: " + ex.getMessage());
        }
        return "dashboard";
    }

    @GetMapping("/incidents/history")
    public String history(Model model) {
        try {
            List<IncidentSummaryDto> incidents = incidentService.getAllIncidents();
            List<IncidentSummaryDto> ordered = incidents.stream()
                    .sorted(Comparator.comparing(IncidentSummaryDto::createdAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .toList();
            model.addAttribute("incidents", ordered);
            model.addAttribute("stats", buildDashboardStats(ordered));
        } catch (Exception ex) {
            model.addAttribute("incidents", List.of());
            model.addAttribute("stats", new DashboardStats(0, 0, 0, 0, 0));
            model.addAttribute("errorMessage", "Unable to load incident history: " + ex.getMessage());
        }
        return "incident-history";
    }

    @GetMapping("/incidents/{id}")
    public String incidentDetail(@PathVariable Long id, Model model) {
        try {
            IncidentDetailDto incident = incidentService.getIncidentById(id);
            AiAnalysis aiAnalysis = aiAnalysisRepository.findByIncidentId(id).stream()
                    .sorted(Comparator.comparing(AiAnalysis::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .findFirst()
                    .orElse(null);

            model.addAttribute("incident", incident);
            model.addAttribute("aiAnalysis", aiAnalysis);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Unable to load the selected incident: " + ex.getMessage());
        }
        return "incident-detail";
    }

    @PostMapping("/dashboard/logs")
    public String ingestLogData(
            @RequestParam(value = "logText", required = false) String logText,
            @RequestParam(value = "logFile", required = false) MultipartFile logFile,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String rawLog = resolveLogInput(logFile, logText);
            if (rawLog == null || rawLog.isBlank()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Please upload a log file or paste log text before submitting.");
                return "redirect:/dashboard";
            }

            List<Incident> createdIncidents = persistDetectedIncidents(rawLog);
            if (createdIncidents.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "No incidents were detected from the supplied log content.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Detected " + createdIncidents.size() + " incident(s) from the uploaded log stream.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/incidents/{id}/analyze")
    public String analyzeIncident(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            IncidentDetailDto incident = incidentService.getIncidentById(id);
            String context = buildIncidentContext(incident);
            List<String> evidence = incident.logEntries() == null ? List.of() : incident.logEntries().stream()
                    .map(LogEntrySummaryDto::rawMessage)
                    .filter(message -> message != null && !message.isBlank())
                    .limit(12)
                    .toList();

            aiAnalysisService.saveAnalysis(id, new AiAnalysisRequest(String.valueOf(id), context, evidence));
            redirectAttributes.addFlashAttribute("successMessage", "AI analysis completed successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/incidents/" + id;
    }

    private DashboardStats buildDashboardStats(List<IncidentSummaryDto> incidents) {
        if (incidents == null) {
            return new DashboardStats(0, 0, 0, 0, 0);
        }

        Map<String, Long> counts = incidents.stream()
                .collect(Collectors.groupingBy(item -> item.severity() == null ? "LOW" : item.severity().name(), Collectors.counting()));

        long low = counts.getOrDefault("LOW", 0L);
        long medium = counts.getOrDefault("MEDIUM", 0L);
        long high = counts.getOrDefault("HIGH", 0L);
        long critical = counts.getOrDefault("CRITICAL", 0L);
        return new DashboardStats(incidents.size(), low, medium, high, critical);
    }

    private List<Incident> persistDetectedIncidents(String rawLog) {
        List<ParsedLogEntry> parsedEntries = logParserService.parse(rawLog);
        List<Incident> detectedIncidents = incidentDetectionService.detect(parsedEntries);
        if (detectedIncidents.isEmpty()) {
            return List.of();
        }

        List<Incident> saved = new ArrayList<>();
        for (Incident incident : detectedIncidents) {
            List<ParsedLogEntry> matching = parsedEntries.stream()
                    .filter(entry -> entry.timestamp() != null)
                    .filter(entry -> !entry.timestamp().isBefore(incident.getFirstSeenAt()) && !entry.timestamp().isAfter(incident.getLastSeenAt()))
                    .sorted(Comparator.comparing(ParsedLogEntry::timestamp))
                    .toList();

            if (matching.isEmpty()) {
                matching = parsedEntries.stream()
                        .filter(entry -> entry.timestamp() != null)
                        .sorted(Comparator.comparing(ParsedLogEntry::timestamp))
                        .limit(5)
                        .toList();
            }

            for (ParsedLogEntry entry : matching) {
                LogEntry logEntry = new LogEntry(
                        entry.originalContent() == null ? "" : entry.originalContent(),
                        entry.timestamp(),
                        entry.logLevel(),
                        entry.serviceName(),
                        entry.traceId(),
                        entry.message() == null ? entry.exceptionInformation() : entry.message()
                );
                incident.addLogEntry(logEntry);
            }

            saved.add(incidentRepository.save(incident));
        }
        return saved;
    }

    private String resolveLogInput(MultipartFile logFile, String logText) {
        if (logFile != null && !logFile.isEmpty()) {
            try {
                return sanitizeLogInput(new String(logFile.getBytes()));
            } catch (Exception ex) {
                throw new IllegalStateException("Unable to read uploaded file: " + ex.getMessage(), ex);
            }
        }
        return sanitizeLogInput(logText);
    }

    private String sanitizeLogInput(String raw) {
        if (raw == null) {
            return null;
        }
        String cleaned = raw.replace("\r", "");
        cleaned = cleaned.replaceAll("(?m)^```.*$", "");
        cleaned = cleaned.replaceAll("(?m)^```$", "");
        cleaned = cleaned.replaceAll("(?m)^[\\s]*text[\\s]*$", "");
        cleaned = cleaned.replaceAll("(?m)^[\\s]*json[\\s]*$", "");
        cleaned = cleaned.replaceAll("(?m)^[\\s]*log[\\s]*$", "");
        return cleaned.trim();
    }

    private String buildIncidentContext(IncidentDetailDto incident) {
        StringBuilder context = new StringBuilder();
        context.append("Incident: ").append(incident.title()).append(System.lineSeparator());
        if (incident.summary() != null && !incident.summary().isBlank()) {
            context.append("Summary: ").append(incident.summary()).append(System.lineSeparator());
        }
        context.append("Severity: ").append(incident.severity()).append(System.lineSeparator());
        context.append("Status: ").append(incident.status()).append(System.lineSeparator());
        context.append("Observed from ").append(incident.firstSeenAt()).append(" to ").append(incident.lastSeenAt()).append(System.lineSeparator());

        if (incident.logEntries() != null && !incident.logEntries().isEmpty()) {
            context.append("Relevant evidence:").append(System.lineSeparator());
            for (LogEntrySummaryDto entry : incident.logEntries()) {
                if (entry.rawMessage() != null && !entry.rawMessage().isBlank()) {
                    context.append("- ").append(entry.rawMessage()).append(System.lineSeparator());
                }
            }
        }
        return context.toString();
    }
}

