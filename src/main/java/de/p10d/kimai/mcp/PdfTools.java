package de.p10d.kimai.mcp;

import de.p10d.kimai.cli.JsonRenderer;
import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.TimesheetService;
import de.p10d.kimai.pdf.TimesheetPdfWriter;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * MCP-Tool für Zeitnachweis-PDFs (Spec 005 FA-1, fachlich Spec 003).
 */
@Component
public class PdfTools {

    private static final Path DEFAULT_OUTPUT_DIR = Path.of("pdf");

    private final TimesheetService service;
    private final TimesheetPdfWriter writer;
    private final JsonRenderer jsonRenderer;

    public PdfTools(TimesheetService service, TimesheetPdfWriter writer, JsonRenderer jsonRenderer) {
        this.service = service;
        this.writer = writer;
        this.jsonRenderer = jsonRenderer;
    }

    @McpTool(
        name = "kimai_generate_timesheet_pdfs",
        description = "Erzeugt pro Projekt einen PDF-Zeitnachweis aus den Kimai-Timesheets eines "
            + "Zeitraums (Kopfdaten, Stundensummen je Tätigkeit, alle Einträge chronologisch). "
            + "Liefert die absoluten Pfade der erzeugten Dateien.")
    public CallToolResult generateTimesheetPdfs(
        @McpToolParam(required = true,
            description = "Startdatum des Zeitraums im Format yyyy-MM-dd, z. B. 2026-07-01")
        String start,
        @McpToolParam(required = true,
            description = "Enddatum des Zeitraums im Format yyyy-MM-dd (einschließlich), z. B. 2026-07-31")
        String end,
        @McpToolParam(required = false,
            description = "Kimai-User-ID, um nur die Einträge eines Users aufzunehmen; "
                + "ohne Angabe alle User. IDs liefert kimai_list_users.")
        Long userId,
        @McpToolParam(required = false,
            description = "true, um auch nicht abrechenbare Einträge aufzunehmen; Standard false")
        Boolean includeNonBillable,
        @McpToolParam(required = false,
            description = "Ausgabeverzeichnis für die PDFs; Standard „pdf/“ im Arbeitsverzeichnis des Servers")
        String outputDir) {

        return McpToolSupport.execute(jsonRenderer, () -> {
            var query = new TimesheetQuery(
                McpToolSupport.parseDate(start), McpToolSupport.parseDate(end),
                userId, !Boolean.TRUE.equals(includeNonBillable));
            var report = service.fetch(query);
            if (report.entries().isEmpty()) {
                return "Keine Einträge im Zeitraum gefunden — keine PDFs erzeugt.";
            }
            Path dir = outputDir == null || outputDir.isBlank()
                ? DEFAULT_OUTPUT_DIR : Path.of(outputDir);
            List<String> files = writer.write(report, dir, null).stream()
                .map(file -> file.toAbsolutePath().toString())
                .toList();
            return Map.of("files", files);
        });
    }
}
