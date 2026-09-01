package de.p10d.kimai.mcp;

import de.p10d.kimai.cli.JsonRenderer;
import de.p10d.kimai.core.TimeTrackingService;
import de.p10d.kimai.core.TimesheetService;
import de.p10d.kimai.pdf.TimesheetPdfWriter;
import io.modelcontextprotocol.server.McpServerFeatures;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.provider.tool.SyncMcpToolProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prüft, was der MCP-Server nach außen anbietet (Spec 002/005 AK), ohne
 * einen stdio-Server zu starten: derselbe Provider-Mechanismus, den die
 * Auto-Konfiguration für @McpTool-Beans verwendet.
 */
class ToolRegistrationTest {

    private final JsonRenderer jsonRenderer = new JsonRenderer();
    private final TimesheetService service = new TimesheetService(query -> List.of());
    private final TimesheetTools tools = new TimesheetTools(service, List::of, jsonRenderer);
    private final PdfTools pdfTools = new PdfTools(service, new TimesheetPdfWriter(), jsonRenderer);
    private final TimeTrackingTools timeTrackingTools = new TimeTrackingTools(
        new TimeTrackingService(List::of, projectId -> List.of(), entry -> null), jsonRenderer);

    private List<McpServerFeatures.SyncToolSpecification> allSpecs() {
        return new SyncMcpToolProvider(List.of(tools, pdfTools, timeTrackingTools)).getToolSpecifications();
    }

    @Test
    void alleKimaiToolsSindMitDeutschenBeschreibungenRegistriert() {
        List<McpServerFeatures.SyncToolSpecification> specs = allSpecs();

        assertThat(specs).extracting(spec -> spec.tool().name())
            .containsExactlyInAnyOrder(
                "kimai_list_timesheets", "kimai_list_users", "kimai_generate_timesheet_pdfs",
                "kimai_list_projects", "kimai_list_activities", "kimai_create_timesheet");

        var timesheets = byName(specs, "kimai_list_timesheets");
        assertThat(timesheets.tool().description())
            .contains("Kimai").contains("Zeitraum").contains("abrechenbare");

        var users = byName(specs, "kimai_list_users");
        assertThat(users.tool().description())
            .contains("Kimai-User").contains("kimai_list_timesheets");

        var pdfs = byName(specs, "kimai_generate_timesheet_pdfs");
        assertThat(pdfs.tool().description()).contains("PDF-Zeitnachweis").contains("absoluten Pfade");

        var create = byName(specs, "kimai_create_timesheet");
        assertThat(create.tool().description()).contains("schreibend").contains("Name oder ID");
        assertThat(byName(specs, "kimai_list_projects").tool().description()).contains("kimai_create_timesheet");
        assertThat(byName(specs, "kimai_list_activities").tool().description()).contains("global");
    }

    @Test
    @SuppressWarnings("unchecked")
    void createTimesheetParameterSindDokumentiert() {
        var create = byName(allSpecs(), "kimai_create_timesheet");

        Map<String, Object> schema = create.tool().inputSchema();
        assertThat((List<String>) schema.get("required"))
            .containsExactlyInAnyOrder("date", "start", "project", "activity");

        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThat(properties.keySet()).containsExactlyInAnyOrder(
            "date", "start", "end", "duration", "project", "activity",
            "description", "userId", "tags", "billable");
        assertThat(properties.get("duration").toString()).contains("3h30m");
        assertThat(properties.get("userId").toString()).contains("kimai_list_users");
        assertThat(properties.get("tags").toString()).contains("array");
    }

    @Test
    @SuppressWarnings("unchecked")
    void zeitraumParameterSindPflichtUndDokumentiert() {
        var timesheets = byName(allSpecs(), "kimai_list_timesheets");

        Map<String, Object> schema = timesheets.tool().inputSchema();
        assertThat((List<String>) schema.get("required")).containsExactlyInAnyOrder("start", "end");

        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertThat(properties.keySet())
            .containsExactlyInAnyOrder("start", "end", "userId", "includeNonBillable");
        assertThat(properties.get("start").toString()).contains("yyyy-MM-dd");
        assertThat(properties.get("userId").toString()).contains("kimai_list_users");
    }

    private static McpServerFeatures.SyncToolSpecification byName(
        List<McpServerFeatures.SyncToolSpecification> specs, String name) {
        return specs.stream().filter(spec -> spec.tool().name().equals(name)).findFirst().orElseThrow();
    }
}
