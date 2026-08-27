package de.p10d.kimai.core;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimesheetServiceTest {

    private static final TimesheetQuery QUERY = new TimesheetQuery(
        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

    @Test
    void delegiertAnDieQuelleUndBautDenReport() {
        var entries = List.of(entry(3600), entry(9000));
        var source = new StubSource(entries);
        var service = new TimesheetService(source);

        var report = service.fetch(QUERY);

        assertThat(source.lastQuery).isSameAs(QUERY);
        assertThat(source.calls).isEqualTo(1);
        assertThat(report.query()).isSameAs(QUERY);
        assertThat(report.entries()).containsExactlyElementsOf(entries);
        assertThat(report.totalDurationSeconds()).isEqualTo(12600);
    }

    @Test
    void leereQuelleErgibtLeerenReportMitDauerNull() {
        var service = new TimesheetService(new StubSource(List.of()));

        var report = service.fetch(QUERY);

        assertThat(report.entries()).isEmpty();
        assertThat(report.totalDurationSeconds()).isZero();
    }

    private static TimesheetEntry entry(long durationSeconds) {
        return new TimesheetEntry(
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 12, 30),
            durationSeconds,
            "Beratung",
            new TimesheetEntry.User(2, "Erika"),
            new TimesheetEntry.Customer("ACME GmbH", "K-001"),
            new TimesheetEntry.Project(1, "Projekt X", "A-100"),
            new TimesheetEntry.Activity(5, "Entwicklung"),
            null);
    }

    private static class StubSource implements TimesheetSource {
        private final List<TimesheetEntry> entries;
        private TimesheetQuery lastQuery;
        private int calls;

        private StubSource(List<TimesheetEntry> entries) {
            this.entries = entries;
        }

        @Override
        public List<TimesheetEntry> fetch(TimesheetQuery query) {
            calls++;
            lastQuery = query;
            return entries;
        }
    }
}
