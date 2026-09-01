package de.p10d.kimai.mcp;

import de.p10d.kimai.cli.JsonRenderer;
import de.p10d.kimai.client.KimaiException;
import de.p10d.kimai.core.ActivityInfo;
import de.p10d.kimai.core.CreatedTimesheet;
import de.p10d.kimai.core.NewTimesheet;
import de.p10d.kimai.core.ProjectInfo;
import de.p10d.kimai.core.TimeTrackingService;
import de.p10d.kimai.core.TimesheetWriter;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimeTrackingToolsTest {

    private static final ProjectInfo PROJECT = new ProjectInfo(1, "Projekt X", 7L, "ACME GmbH");
    private static final ActivityInfo GLOBAL = new ActivityInfo(5, "Entwicklung", null);
    private static final ActivityInfo SPECIFIC = new ActivityInfo(6, "Review", 1L);

    private final RecordingWriter writer = new RecordingWriter();
    private final TimeTrackingTools tools = new TimeTrackingTools(
        new TimeTrackingService(() -> List.of(PROJECT), projectId -> List.of(GLOBAL, SPECIFIC), writer),
        new JsonRenderer());
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void createLegtAnUndLiefertDenEintragAlsJson() {
        var result = tools.createTimesheet("2026-08-28", "09:00", "12:30", null,
            "Projekt X", "Entwicklung", "Beratung", 3L, List.of("a"), null);

        assertThat(result.isError()).isFalse();
        var json = mapper.readTree(text(result));
        assertThat(json.path("id").asLong()).isEqualTo(4711);
        assertThat(json.path("begin").asText()).isEqualTo("2026-08-28T09:00:00");
        assertThat(json.path("durationSeconds").asLong()).isEqualTo(12600);
        assertThat(json.path("project").path("name").asText()).isEqualTo("Projekt X");
        assertThat(json.path("activity").path("global").asBoolean()).isTrue();
        assertThat(json.path("billable").asBoolean()).isTrue();
        assertThat(writer.last.userId()).isEqualTo(3L);
        assertThat(writer.last.tags()).containsExactly("a");
    }

    @Test
    void dauerStattEndeUndBillableFalse() {
        var result = tools.createTimesheet("2026-08-28", "09:00", null, "45m", "1", "6", null, null, null, false);

        assertThat(result.isError()).isFalse();
        assertThat(writer.last.end()).isEqualTo(LocalDateTime.of(2026, 8, 28, 9, 45));
        assertThat(writer.last.activity()).isEqualTo(SPECIFIC);
        assertThat(writer.last.billable()).isFalse();
    }

    @Test
    void endeUndDauerZugleichOderKeinsVonBeidenIstFehler() {
        var both = tools.createTimesheet("2026-08-28", "09:00", "10:00", "1h", "1", "5", null, null, null, null);
        var none = tools.createTimesheet("2026-08-28", "09:00", "", " ", "1", "5", null, null, null, null);

        assertThat(both.isError()).isTrue();
        assertThat(text(both)).contains("Entweder end oder duration");
        assertThat(none.isError()).isTrue();
        assertThat(writer.last).isNull();
    }

    @Test
    void ungueltigeEingabenErgebenDeutscheToolFehler() {
        assertThat(text(tools.createTimesheet("28.08.2026", "09:00", "10:00", null, "1", "5", null, null, null, null)))
            .contains("Ungültiges Datum");
        assertThat(text(tools.createTimesheet(null, "09:00", "10:00", null, "1", "5", null, null, null, null)))
            .contains("Datum muss angegeben");
        assertThat(text(tools.createTimesheet("2026-08-28", "9", "10:00", null, "1", "5", null, null, null, null)))
            .contains("Ungültige Uhrzeit");
        assertThat(text(tools.createTimesheet("2026-08-28", "10:00", "09:00", null, "1", "5", null, null, null, null)))
            .contains("Ende muss nach dem Beginn");
        assertThat(text(tools.createTimesheet("2026-08-28", "09:00", "10:00", null, "Nix", "5", null, null, null, null)))
            .contains("Projekt „Nix“ nicht gefunden").contains("Projekt X");
        assertThat(writer.last).isNull();
    }

    @Test
    void kimaiFehlerErgibtToolFehlerOhneFrameworkPraefix() {
        writer.failure = new KimaiException("Kimai-API-Fehler: HTTP 400 – Validation Failed");

        var result = tools.createTimesheet("2026-08-28", "09:00", "10:00", null, "1", "5", null, null, null, null);

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("Kimai-API-Fehler: HTTP 400 – Validation Failed");
    }

    @Test
    void listProjectsUndActivitiesLiefernJson() {
        var projects = mapper.readTree(text(tools.listProjects(null)));
        assertThat(projects).hasSize(1);
        assertThat(projects.get(0).path("customerName").asText()).isEqualTo("ACME GmbH");

        var activities = mapper.readTree(text(tools.listActivities("Projekt X")));
        assertThat(activities).hasSize(2);
        assertThat(activities.get(0).path("global").asBoolean()).isTrue();
        assertThat(activities.get(1).path("projectId").asLong()).isEqualTo(1);

        assertThat(tools.listProjects("Niemand").isError()).isFalse();
        assertThat(text(tools.listProjects("Niemand"))).isEqualTo("[]");
        assertThat(tools.listActivities("Nix").isError()).isTrue();
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }

    private static class RecordingWriter implements TimesheetWriter {
        private NewTimesheet last;
        private RuntimeException failure;

        @Override
        public CreatedTimesheet create(NewTimesheet entry) {
            if (failure != null) {
                throw failure;
            }
            last = entry;
            return new CreatedTimesheet(4711, entry.begin(), entry.end(), entry.durationSeconds(),
                entry.description(), entry.project(), entry.activity(), entry.userId(), entry.tags(),
                entry.billable());
        }
    }
}
