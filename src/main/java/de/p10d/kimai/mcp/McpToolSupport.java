package de.p10d.kimai.mcp;

import de.p10d.kimai.cli.JsonRenderer;
import de.p10d.kimai.client.KimaiException;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

/**
 * Gemeinsame Bausteine der MCP-Tools: Ergebnis-/Fehlerformat (deutsche
 * Meldung ohne Framework-Präfix, Plan 002 B-3) und Datums-Validierung.
 * Öffentlich, damit aufbauende Projekte (z. B. kimai2lexware) sie nutzen.
 */
public final class McpToolSupport {

    private McpToolSupport() {
    }

    public static CallToolResult execute(JsonRenderer jsonRenderer, Supplier<Object> action) {
        try {
            Object value = action.get();
            String text = value instanceof String s ? s : jsonRenderer.render(value);
            return ok(text);
        } catch (IllegalArgumentException | KimaiException e) {
            return error(e.getMessage());
        }
    }

    public static CallToolResult ok(String text) {
        return CallToolResult.builder().addTextContent(text).isError(false).build();
    }

    public static CallToolResult error(String message) {
        return CallToolResult.builder().addTextContent(message).isError(true).build();
    }

    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            // gleiche Meldung wie die Query-Validierung des Kerns
            throw new IllegalArgumentException("Start- und Enddatum müssen beide angegeben werden.");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ungültiges Datum „" + value + "“ — erwartet wird yyyy-MM-dd.");
        }
    }
}
