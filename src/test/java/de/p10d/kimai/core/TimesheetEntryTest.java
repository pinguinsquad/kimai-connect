package de.p10d.kimai.core;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimesheetEntryTest {

    @Test
    void ratePerHourWirdAusRateUndDauerBerechnet() {
        var entry = entry(12600, 262.5); // 3,5 Stunden

        assertThat(entry.ratePerHour()).isEqualTo(75.0);
    }

    @Test
    void ratePerHourOhneRateIstNull() {
        var entry = entry(12600, null);

        assertThat(entry.ratePerHour()).isNull();
    }

    @Test
    void ratePerHourBeiDauerNullIstNull() {
        var entry = entry(0, 100.0);

        assertThat(entry.ratePerHour()).isNull();
    }

    private TimesheetEntry entry(long durationSeconds, Double rate) {
        return new TimesheetEntry(
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 12, 30),
            durationSeconds,
            "Beratung",
            new TimesheetEntry.User(2, "Erika"),
            new TimesheetEntry.Customer("ACME GmbH", "K-001"),
            new TimesheetEntry.Project(1, "Projekt X", "A-100"),
            new TimesheetEntry.Activity(5, "Entwicklung"),
            rate);
    }
}
