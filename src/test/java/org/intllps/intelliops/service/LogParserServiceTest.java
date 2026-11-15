package org.intllps.intelliops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.intllps.intelliops.dto.ParsedLogEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogParserServiceTest {

    private final LogParserService logParserService = new LogParserService(new ObjectMapper());

    @Test
    void shouldParseSpringBootLogEntry() {
        String log = "2026-11-14T10:33:12.123Z  INFO  api-gateway --- Request processed in 42ms";

        List<ParsedLogEntry> entries = logParserService.parse(log);

        assertEquals(1, entries.size());
        ParsedLogEntry entry = entries.getFirst();
        assertTrue(entry.parsedSuccessfully());
        assertEquals("INFO", entry.logLevel());
        assertEquals("api-gateway", entry.serviceName());
        assertEquals("Request processed in 42ms", entry.message());
        assertNotNull(entry.timestamp());
    }

    @Test
    void shouldParseJsonLogEntry() {
        String log = "{\n" +
                "  \"timestamp\": \"2026-11-14T10:35:12.123Z\",\n" +
                "  \"level\": \"ERROR\",\n" +
                "  \"service\": \"billing-service\",\n" +
                "  \"traceId\": \"b3af8e9a-50b1-4a9a-b6b9-bba5eb3ff807\",\n" +
                "  \"message\": \"Database connection failed\"\n" +
                "}";

        List<ParsedLogEntry> entries = logParserService.parse(log);

        assertEquals(1, entries.size());
        ParsedLogEntry entry = entries.getFirst();
        assertTrue(entry.parsedSuccessfully());
        assertEquals("ERROR", entry.logLevel());
        assertEquals("billing-service", entry.serviceName());
        assertEquals("b3af8e9a-50b1-4a9a-b6b9-bba5eb3ff807", entry.traceId());
        assertEquals("Database connection failed", entry.message());
    }

    @Test
    void shouldHandleMultilineJavaStackTrace() {
        String log = String.join(System.lineSeparator(),
                "2026-11-14T10:40:00Z ERROR inventory-service --- Failed to persist order",
                "java.lang.IllegalStateException: duplicate key",
                "    at com.example.OrderService.persist(OrderService.java:88)",
                "    at com.example.OrderController.handle(OrderController.java:44)",
                "Caused by: java.sql.SQLException: connection timeout"
        );

        List<ParsedLogEntry> entries = logParserService.parse(log);

        assertEquals(1, entries.size());
        ParsedLogEntry entry = entries.getFirst();
        assertEquals("ERROR", entry.logLevel());
        assertEquals("inventory-service", entry.serviceName());
        assertTrue(entry.exceptionInformation().contains("IllegalStateException"));
        assertTrue(entry.exceptionInformation().contains("Caused by: java.sql.SQLException"));
    }

    @Test
    void shouldKeepMalformedLinesWithoutFailingEntireUpload() {
        String log = String.join(System.lineSeparator(),
                "2026-11-14T10:45:00Z ERROR payments-service --- Payment request failed",
                "NOT A VALID LOG ENTRY",
                "2026-11-14T10:46:00Z WARN payments-service --- Retrying request"
        );

        List<ParsedLogEntry> entries = logParserService.parse(log);

        assertEquals(3, entries.size());
        assertTrue(entries.get(1).parsedSuccessfully() || entries.get(1).message() != null || entries.get(1).originalContent().contains("NOT A VALID LOG ENTRY"));
    }
}

