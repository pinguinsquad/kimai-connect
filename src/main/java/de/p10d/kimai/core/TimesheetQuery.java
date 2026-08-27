package de.p10d.kimai.core;

import java.time.LocalDate;

/**
 * Abfrage für Timesheet-Einträge (Spec 001 FA-1 bis FA-3).
 * Der Zeitraum ist Pflicht und umfasst beide Tage vollständig.
 */
public record TimesheetQuery(LocalDate start, LocalDate end, Long userId, boolean billableOnly) {

    public TimesheetQuery {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start- und Enddatum müssen beide angegeben werden.");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Das Enddatum darf nicht vor dem Startdatum liegen.");
        }
    }

    public TimesheetQuery(LocalDate start, LocalDate end) {
        this(start, end, null, true);
    }
}
