package de.p10d.kimai.mcp;

import de.p10d.kimai.cli.JsonRenderer;
import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.TimesheetService;
import de.p10d.kimai.core.TimesheetSource;
import de.p10d.kimai.core.UserInfo;
import de.p10d.kimai.client.KimaiException;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimesheetToolsTest {

    private final RecordingSource source = new RecordingSource();
    private final TimesheetTools tools = new TimesheetTools(
        new TimesheetService(source),
        () -> List.of(new UserInfo(3, "Erika Mustermann"), new UserInfo(2, "Max Mustermann")),
        new JsonRenderer());
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void gueltigerAufrufLiefertDenReportAlsJson() {
        source.entries = List.of(entry());

        var result = tools.listTimesheets("2026-07-01", "2026-07-31", null, null);

        assertThat(result.isError()).isFalse();
        var report = mapper.readTree(text(result));
        assertThat(report.path("query").path("start").asText()).isEqualTo("2026-07-01");
        assertThat(report.path("query").path("billableOnly").asBoolean()).isTrue();
        assertThat(report.path("entries")).hasSize(1);
        assertThat(report.path("totalDurationSeconds").asLong()).isEqualTo(12600);
    }

    @Test
    void userIdUndIncludeNonBillableWirkenAufDieQuery() {
        tools.listTimesheets("2026-07-01", "2026-07-31", 2L, true);

        assertThat(source.lastQuery)
            .isEqualTo(new TimesheetQuery(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 2L, false));
    }

    @Test
    void fehlenderZeitraumErgibtDeutschenToolFehler() {
        var result = tools.listTimesheets(null, null, null, null);

        assertThat(result.isError()).isTrue();
        assertThat(text(result))
            .contains("Start- und Enddatum")
            .doesNotContain("Error invoking");
        assertThat(source.calls).isZero();
    }

    @Test
    void halberZeitraumErgibtDeutschenToolFehler() {
        var result = tools.listTimesheets("2026-07-01", null, null, null);

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("Start- und Enddatum");
        assertThat(source.calls).isZero();
    }

    @Test
    void ungueltigesDatumErgibtDeutschenToolFehler() {
        var result = tools.listTimesheets("01.07.2026", "2026-07-31", null, null);

        assertThat(result.isError()).isTrue();
        assertThat(text(result))
            .contains("Ungültiges Datum")
            .contains("yyyy-MM-dd")
            .doesNotContain("Error invoking");
        assertThat(source.calls).isZero();
    }

    @Test
    void kimaiFehlerErgibtToolFehlerOhneFrameworkPraefix() {
        source.failure = new KimaiException("Kimai-API-Fehler: HTTP 500");

        var result = tools.listTimesheets("2026-07-01", "2026-07-31", null, null);

        assertThat(result.isError()).isTrue();
        assertThat(text(result))
            .isEqualTo("Kimai-API-Fehler: HTTP 500");
    }

    @Test
    void listUsersLiefertAlleUserAlsJson() {
        var result = tools.listUsers();

        assertThat(result.isError()).isFalse();
        var users = mapper.readTree(text(result));
        assertThat(users).hasSize(2);
        assertThat(users.get(0).path("id").asLong()).isEqualTo(3);
        assertThat(users.get(0).path("name").asText()).isEqualTo("Erika Mustermann");
    }

    @Test
    void kimaiFehlerBeimUserAbrufErgibtToolFehler() {
        var failingTools = new TimesheetTools(
            new TimesheetService(source),
            () -> { throw new KimaiException("KIMAI_API_TOKEN ist nicht gesetzt."); },
            new JsonRenderer());

        var result = failingTools.listUsers();

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).isEqualTo("KIMAI_API_TOKEN ist nicht gesetzt.");
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }

    private static TimesheetEntry entry() {
        return new TimesheetEntry(
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 12, 30),
            12600,
            "Beratung",
            new TimesheetEntry.User(3, "Erika Mustermann"),
            new TimesheetEntry.Customer("ACME GmbH", "K-001"),
            new TimesheetEntry.Project(1, "Projekt X", "A-100"),
            new TimesheetEntry.Activity(5, "Entwicklung"),
            null);
    }

    private static class RecordingSource implements TimesheetSource {
        private List<TimesheetEntry> entries = List.of();
        private TimesheetQuery lastQuery;
        private int calls;
        private RuntimeException failure;

        @Override
        public List<TimesheetEntry> fetch(TimesheetQuery query) {
            calls++;
            lastQuery = query;
            if (failure != null) {
                throw failure;
            }
            return entries;
        }
    }
}
