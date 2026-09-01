package de.p10d.kimai.mcp;

import de.p10d.kimai.cli.JsonRenderer;
import de.p10d.kimai.core.DurationParser;
import de.p10d.kimai.core.TimeTrackingService;
import de.p10d.kimai.core.TimesheetDraft;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * MCP-Tools zum Erfassen von Zeiten: Projekte und Tätigkeiten nachschlagen,
 * Eintrag anlegen. Fehler kommen wie bei den lesenden Tools als isError mit
 * deutscher Meldung beim Client an.
 */
@Component
public class TimeTrackingTools {

    private final TimeTrackingService service;
    private final JsonRenderer jsonRenderer;

    public TimeTrackingTools(TimeTrackingService service, JsonRenderer jsonRenderer) {
        this.service = service;
        this.jsonRenderer = jsonRenderer;
    }

    @McpTool(
        name = "kimai_list_projects",
        description = "Listet die sichtbaren Kimai-Projekte mit ID, Name und Kunde. Dient dazu, "
            + "das Projekt für kimai_create_timesheet zu finden; dort kann Name oder ID angegeben werden.")
    public CallToolResult listProjects(
        @McpToolParam(required = false,
            description = "Nur Projekte dieses Kunden: Kunden-ID oder Namensbestandteil")
        String customer) {
        return McpToolSupport.execute(jsonRenderer, () -> service.listProjects(customer));
    }

    @McpTool(
        name = "kimai_list_activities",
        description = "Listet Kimai-Tätigkeiten mit ID und Name. Mit Projekt die Tätigkeiten dieses "
            + "Projekts einschließlich der globalen (projectId null), ohne Projekt alle. Dient dazu, "
            + "die Tätigkeit für kimai_create_timesheet zu finden.")
    public CallToolResult listActivities(
        @McpToolParam(required = false,
            description = "Projekt als Name oder ID, dessen Tätigkeiten gelistet werden sollen")
        String project) {
        return McpToolSupport.execute(jsonRenderer, () -> service.listActivities(project));
    }

    @McpTool(
        name = "kimai_create_timesheet",
        description = "Legt einen Timesheet-Eintrag in Kimai an (schreibend). Projekt und Tätigkeit "
            + "als Name oder ID; ein Name muss eindeutig sein, sonst nennt der Fehler die Kandidaten. "
            + "Entweder end oder duration angeben. Liefert den angelegten Eintrag mit ID, Beginn, Ende "
            + "und Dauer, wie Kimai ihn gespeichert hat (ggf. gerundet).")
    public CallToolResult createTimesheet(
        @McpToolParam(required = true,
            description = "Datum im Format yyyy-MM-dd, z. B. 2026-08-28")
        String date,
        @McpToolParam(required = true,
            description = "Beginn als Uhrzeit HH:mm, z. B. 09:00")
        String start,
        @McpToolParam(required = false,
            description = "Ende als Uhrzeit HH:mm, z. B. 12:30; alternativ duration")
        String end,
        @McpToolParam(required = false,
            description = "Dauer statt end, z. B. 3h30m, 2h, 45m oder 1:30")
        String duration,
        @McpToolParam(required = true,
            description = "Projekt als Name oder ID; Kandidaten liefert kimai_list_projects")
        String project,
        @McpToolParam(required = true,
            description = "Tätigkeit als Name oder ID; Kandidaten liefert kimai_list_activities")
        String activity,
        @McpToolParam(required = false,
            description = "Beschreibung des Eintrags")
        String description,
        @McpToolParam(required = false,
            description = "Kimai-User-ID, für die erfasst wird (braucht Berechtigung); "
                + "ohne Angabe der API-User selbst. IDs liefert kimai_list_users.")
        Long userId,
        @McpToolParam(required = false,
            description = "Tags für den Eintrag")
        List<String> tags,
        @McpToolParam(required = false,
            description = "false, um als nicht abrechenbar zu erfassen; Standard true")
        Boolean billable) {

        return McpToolSupport.execute(jsonRenderer, () -> {
            LocalDate day = parseDate(date);
            LocalDateTime begin = day.atTime(parseTime(start, "Beginn"));
            boolean hasEnd = end != null && !end.isBlank();
            boolean hasDuration = duration != null && !duration.isBlank();
            if (hasEnd == hasDuration) {
                throw new IllegalArgumentException("Entweder end oder duration angeben.");
            }
            LocalDateTime finish = hasEnd
                ? day.atTime(parseTime(end, "Ende"))
                : begin.plus(DurationParser.parse(duration));
            var draft = new TimesheetDraft(begin, finish, project, activity, description, userId, tags,
                !Boolean.FALSE.equals(billable));
            return service.record(draft);
        });
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Das Datum muss angegeben werden (yyyy-MM-dd).");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ungültiges Datum „" + value + "“ — erwartet wird yyyy-MM-dd.");
        }
    }

    private static LocalTime parseTime(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " muss angegeben werden (HH:mm).");
        }
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ungültige Uhrzeit „" + value + "“ — erwartet wird HH:mm.");
        }
    }
}
