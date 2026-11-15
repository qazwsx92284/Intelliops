package org.intllps.intelliops.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.intllps.intelliops.dto.ParsedLogEntry;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LogParserService {

    private static final Pattern JSON_START_PATTERN = Pattern.compile("^\\s*\\{", Pattern.DOTALL);
    private static final Pattern JSON_END_PATTERN = Pattern.compile("\\}\\s*$", Pattern.DOTALL);
    private static final Pattern SPRING_BOOT_PATTERN = Pattern.compile(
            "^(?<timestamp>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*?)(?:\\s+\\[(?<level>INFO|WARN|WARNING|ERROR|DEBUG|TRACE|FATAL)\\]|\\s+(?<levelAlt>INFO|WARN|WARNING|ERROR|DEBUG|TRACE|FATAL))(?:\\s+)(?<service>[^\\s]+)(?:\\s+---\\s+)(?<message>.*)$",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern JAVA_TIMESTAMP_PATTERN = Pattern.compile(
            "(?<timestamp>\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[.,]?\\d{0,9}(?:Z|[+-]\\d{2}:?\\d{2})?)"
    );
    private static final Pattern LOG_LEVEL_PATTERN = Pattern.compile(
            "\\b(INFO|WARN|WARNING|ERROR|DEBUG|TRACE|FATAL)\\b",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile(
            "(?:traceId|trace_id|traceid|x-trace-id|X-Trace-Id)[:=]\\s*['\"]?(?<traceId>[A-Fa-f0-9-]{8,})['\"]?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SERVICE_NAME_PATTERN = Pattern.compile(
            "(?:service|app|application|module)[:=]\\s*['\"]?(?<service>[A-Za-z0-9_.-]+)['\"]?",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile(
            "(?:Exception|Error|Caused by:|Suppressed:|\\bat .*\\(|\\.{3} \\d+ more)",
            Pattern.CASE_INSENSITIVE
    );

    private final ObjectMapper objectMapper;

    public LogParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ParsedLogEntry> parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return List.of();
        }

        String trimmed = rawContent.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return List.of(parseJsonLog(trimmed));
        }

        List<String> records = splitIntoLogRecords(rawContent);
        List<ParsedLogEntry> parsedEntries = new ArrayList<>();
        for (String record : records) {
            parsedEntries.add(parsePlainTextRecord(record));
        }
        return parsedEntries;
    }

    private List<String> splitIntoLogRecords(String rawContent) {
        List<String> records = new ArrayList<>();
        String[] lines = rawContent.split("\\r?\\n");
        StringBuilder current = new StringBuilder();

        for (String line : lines) {
            if (line == null) {
                continue;
            }

            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            boolean newRecord = startsNewRecord(trimmed);
            boolean continuation = isContinuationLine(trimmed);

            if (current.length() > 0 && newRecord && !continuation) {
                records.add(current.toString().trim());
                current.setLength(0);
            }

            if (current.length() > 0 && !newRecord && !continuation) {
                records.add(current.toString().trim());
                current.setLength(0);
                current.append(trimmed);
                continue;
            }

            if (current.length() == 0 && !newRecord && !continuation) {
                records.add(trimmed);
                continue;
            }

            if (current.length() > 0) {
                current.append(System.lineSeparator()).append(trimmed);
            } else {
                current.append(trimmed);
            }
        }

        if (current.length() > 0) {
            records.add(current.toString().trim());
        }

        return records;
    }

    private boolean startsNewRecord(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }

        return JAVA_TIMESTAMP_PATTERN.matcher(line).find()
                || line.matches(".*\\[(INFO|WARN|WARNING|ERROR|DEBUG|TRACE|FATAL)\\].*")
                || LOG_LEVEL_PATTERN.matcher(line).find();
    }

    private boolean isContinuationLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }

        String trimmed = line.trim();
        if (trimmed.startsWith("at ") || trimmed.startsWith("Caused by:") || trimmed.startsWith("Suppressed:") || trimmed.startsWith("java.") || trimmed.startsWith("javax.")) {
            return true;
        }

        return trimmed.startsWith("...") || trimmed.startsWith("\t") || trimmed.startsWith("    ") || EXCEPTION_PATTERN.matcher(trimmed).find();
    }

    private ParsedLogEntry parsePlainTextRecord(String rawRecord) {
        if (rawRecord == null || rawRecord.isBlank()) {
            return new ParsedLogEntry("", null, null, null, null, "", null, false);
        }

        String trimmed = rawRecord.trim();

        Matcher springMatcher = SPRING_BOOT_PATTERN.matcher(trimmed);
        if (springMatcher.matches()) {
            String timestampText = springMatcher.group("timestamp");
            String level = firstNonBlank(springMatcher.group("level"), springMatcher.group("levelAlt"));
            String serviceName = springMatcher.group("service");
            String message = springMatcher.group("message");
            String exceptionInfo = extractExceptionInfo(trimmed);
            String traceId = extractTraceId(trimmed);

            return new ParsedLogEntry(trimmed, parseTimestamp(timestampText), level, serviceName, traceId, message, exceptionInfo, true);
        }

        String logLevel = extractLogLevel(trimmed);
        String serviceName = extractServiceName(trimmed);
        String traceId = extractTraceId(trimmed);
        String message = extractMessage(trimmed, logLevel, serviceName);
        OffsetDateTime timestamp = extractTimestamp(trimmed);
        String exceptionInfo = extractExceptionInfo(trimmed);

        return new ParsedLogEntry(
                trimmed,
                timestamp,
                logLevel,
                serviceName,
                traceId,
                message,
                exceptionInfo,
                timestamp != null || logLevel != null || message != null || exceptionInfo != null
        );
    }

    private ParsedLogEntry parseJsonLog(String rawContent) {
        try {
            JsonNode root = objectMapper.readTree(rawContent);
            String message = firstNonBlank(extractJsonString(root, "message"), extractJsonString(root, "msg"));
            String level = extractJsonString(root, "level");
            String serviceName = extractJsonString(root, "service");
            String traceId = extractJsonString(root, "traceId");
            OffsetDateTime timestamp = parseTimestamp(extractJsonString(root, "timestamp"));
            if (timestamp == null) {
                timestamp = parseTimestamp(extractJsonString(root, "time"));
            }
            String exceptionInfo = extractExceptionFromJson(root);

            return new ParsedLogEntry(rawContent, timestamp, level, serviceName, traceId, message, exceptionInfo, true);
        } catch (JsonProcessingException ex) {
            return new ParsedLogEntry(rawContent, null, null, null, null, rawContent, null, false);
        }
    }

    private String extractLogLevel(String content) {
        Matcher matcher = LOG_LEVEL_PATTERN.matcher(content);
        if (matcher.find()) {
            String value = matcher.group(1);
            return value.equalsIgnoreCase("WARNING") ? "WARN" : value.toUpperCase(Locale.ROOT);
        }
        return null;
    }

    private String extractServiceName(String content) {
        Matcher matcher = SERVICE_NAME_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group("service");
        }

        Matcher maybeService = Pattern.compile("(?:\\b[A-Za-z0-9_.-]+-service\\b|\\b[A-Za-z0-9_.-]+-api\\b|\\b[A-Za-z0-9_.-]+-gateway\\b)").matcher(content);
        if (maybeService.find()) {
            return maybeService.group();
        }

        return null;
    }

    private String extractTraceId(String content) {
        Matcher matcher = TRACE_ID_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group("traceId");
        }

        Matcher fallback = Pattern.compile("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b").matcher(content);
        if (fallback.find()) {
            return fallback.group();
        }

        return null;
    }

    private String extractExceptionInfo(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        String[] lines = content.split("\\r?\\n");
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            if (EXCEPTION_PATTERN.matcher(line).find()) {
                if (builder.length() > 0) {
                    builder.append(System.lineSeparator());
                }
                builder.append(line.trim());
            }
        }
        return builder.length() == 0 ? null : builder.toString();
    }

    private String extractJsonString(JsonNode root, String key) {
        if (root == null || root.isNull()) {
            return null;
        }
        JsonNode node = root.get(key);
        return node == null || node.isNull() ? null : node.asText();
    }

    private String extractExceptionFromJson(JsonNode root) {
        if (root == null) {
            return null;
        }

        if (root.has("exception")) {
            return root.get("exception").asText();
        }
        if (root.has("stack_trace")) {
            return root.get("stack_trace").asText();
        }
        return null;
    }

    private String extractMessage(String content, String logLevel, String serviceName) {
        String candidate = content;

        if (logLevel != null) {
            candidate = candidate.replaceFirst("(?i)\\b" + Pattern.quote(logLevel) + "\\b", "").trim();
        }

        if (serviceName != null) {
            candidate = candidate.replaceFirst("(?i)\\b" + Pattern.quote(serviceName) + "\\b", "").trim();
        }

        Matcher matcher = Pattern.compile("(?:---\\s+|:\\s*)(.+)$").matcher(candidate);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        candidate = candidate.replaceFirst("^.*?\\d{4}-\\d{2}-\\d{2}.*?", "").trim();
        return candidate.isBlank() ? null : candidate;
    }

    private OffsetDateTime extractTimestamp(String content) {
        Matcher matcher = JAVA_TIMESTAMP_PATTERN.matcher(content);
        if (matcher.find()) {
            return parseTimestamp(matcher.group("timestamp"));
        }
        return null;
    }

    private OffsetDateTime parseTimestamp(String timestampText) {
        if (timestampText == null || timestampText.isBlank()) {
            return null;
        }

        String sanitized = timestampText.trim();
        if (sanitized.endsWith("Z")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1) + "+00:00";
        }

        try {
            return OffsetDateTime.parse(sanitized, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException ex) {
            try {
                return OffsetDateTime.parse(sanitized.replace(" ", "T"), DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
