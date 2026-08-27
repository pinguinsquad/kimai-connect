package de.p10d.kimai.cli;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.TimesheetReport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonRendererTest {

    private final JsonRenderer renderer = new JsonRenderer();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void strukturEntsprichtDemPlanSchema() throws Exception {
        var query = new TimesheetQuery(
            LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 2L, true);
        var report = TimesheetReport.of(query, List.of(entry(262.5)));

        JsonNode root = mapper.readTree(renderer.render(report));

        assertThat(root.path("query").path("start").asText()).isEqualTo("2026-07-01");
        assertThat(root.path("query").path("end").asText()).isEqualTo("2026-07-31");
        assertThat(root.path("query").path("userId").asLong()).isEqualTo(2L);
        assertThat(root.path("query").path("billableOnly").asBoolean()).isTrue();
        assertThat(root.path("totalDurationSeconds").asLong()).isEqualTo(12600);

        JsonNode entry = root.path("entries").get(0);
        assertThat(entry.path("begin").asText()).isEqualTo("2026-07-01T09:00:00");
        assertThat(entry.path("end").asText()).isEqualTo("2026-07-01T12:30:00");
        assertThat(entry.path("durationSeconds").asLong()).isEqualTo(12600);
        assertThat(entry.path("description").asText()).isEqualTo("Beratung");
        assertThat(entry.path("user").path("id").asLong()).isEqualTo(2);
        assertThat(entry.path("user").path("name").asText()).isEqualTo("Erika");
        assertThat(entry.path("customer").path("name").asText()).isEqualTo("ACME GmbH");
        assertThat(entry.path("customer").path("number").asText()).isEqualTo("K-001");
        assertThat(entry.path("project").path("orderNumber").asText()).isEqualTo("A-100");
        assertThat(entry.path("activity").path("name").asText()).isEqualTo("Entwicklung");
        assertThat(entry.path("rate").asDouble()).isEqualTo(262.5);
        assertThat(entry.path("ratePerHour").asDouble()).isEqualTo(75.0);
    }

    @Test
    void felderOhneWertErscheinenExplizitAlsNull() throws Exception {
        var query = new TimesheetQuery(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        var report = TimesheetReport.of(query, List.of(entry(null)));

        JsonNode root = mapper.readTree(renderer.render(report));

        assertThat(root.path("query").has("userId")).isTrue();
        assertThat(root.path("query").path("userId").isNull()).isTrue();
        assertThat(root.path("entries").get(0).has("rate")).isTrue();
        assertThat(root.path("entries").get(0).path("rate").isNull()).isTrue();
        assertThat(root.path("entries").get(0).path("ratePerHour").isNull()).isTrue();
    }

    @Test
    void ausgabeIstEineEinzelneZeile() {
        var query = new TimesheetQuery(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));
        var report = TimesheetReport.of(query, List.of(entry(262.5)));

        assertThat(renderer.render(report)).doesNotContain("\n");
    }

    private static TimesheetEntry entry(Double rate) {
        return new TimesheetEntry(
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 12, 30),
            12600,
            "Beratung",
            new TimesheetEntry.User(2, "Erika"),
            new TimesheetEntry.Customer("ACME GmbH", "K-001"),
            new TimesheetEntry.Project(1, "Projekt X", "A-100"),
            new TimesheetEntry.Activity(5, "Entwicklung"),
            rate);
    }
}
