package de.p10d.kimai.mcp;

import de.p10d.kimai.cli.JsonRenderer;
import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.TimesheetService;
import de.p10d.kimai.core.UserSource;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * MCP-Tools für KI-Clients (Spec 002). Nur lesende Operationen. Die Tools
 * liefern CallToolResult direkt, damit Fehler ohne Framework-Präfix als
 * deutsche Meldung beim Client ankommen (Plan B-3); der Server läuft weiter.
 */
@Component
public class TimesheetTools {

    private final TimesheetService service;
    private final UserSource userSource;
    private final JsonRenderer jsonRenderer;

    public TimesheetTools(TimesheetService service, UserSource userSource, JsonRenderer jsonRenderer) {
        this.service = service;
        this.userSource = userSource;
        this.jsonRenderer = jsonRenderer;
    }

    @McpTool(
        name = "kimai_list_timesheets",
        description = "Ruft Timesheet-Einträge aus der Kimai-Zeiterfassung für einen Zeitraum ab. "
            + "Standardmäßig nur abrechenbare Einträge aller User. Liefert pro Eintrag Beginn, Ende, "
            + "Dauer in Sekunden, Beschreibung, User, Kunde, Projekt, Aktivität sowie Satz und "
            + "Stundensatz, dazu die Gesamtdauer (totalDurationSeconds).")
    public CallToolResult listTimesheets(
        @McpToolParam(required = true,
            description = "Startdatum des Zeitraums im Format yyyy-MM-dd, z. B. 2026-07-01")
        String start,
        @McpToolParam(required = true,
            description = "Enddatum des Zeitraums im Format yyyy-MM-dd (einschließlich), z. B. 2026-07-31")
        String end,
        @McpToolParam(required = false,
            description = "Kimai-User-ID, um nur die Einträge eines Users abzurufen; "
                + "ohne Angabe alle User. IDs liefert kimai_list_users.")
        Long userId,
        @McpToolParam(required = false,
            description = "true, um auch nicht abrechenbare Einträge einzubeziehen; Standard false (nur abrechenbare)")
        Boolean includeNonBillable) {

        return McpToolSupport.execute(jsonRenderer, () -> {
            var query = new TimesheetQuery(
                McpToolSupport.parseDate(start), McpToolSupport.parseDate(end),
                userId, !Boolean.TRUE.equals(includeNonBillable));
            return service.fetch(query);
        });
    }

    @McpTool(
        name = "kimai_list_users",
        description = "Listet alle Kimai-User mit ID und Name. Die ID dient als userId-Parameter "
            + "für kimai_list_timesheets.")
    public CallToolResult listUsers() {
        return McpToolSupport.execute(jsonRenderer, userSource::listUsers);
    }
}
