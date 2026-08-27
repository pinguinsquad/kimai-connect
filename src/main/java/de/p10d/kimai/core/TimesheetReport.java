package de.p10d.kimai.core;

import java.util.List;

/**
 * Ergebnis eines Abrufs: Query, Einträge und Gesamtdauer (Spec 001 FA-5/FA-6).
 */
public record TimesheetReport(TimesheetQuery query, List<TimesheetEntry> entries, long totalDurationSeconds) {

    public static TimesheetReport of(TimesheetQuery query, List<TimesheetEntry> entries) {
        long total = entries.stream().mapToLong(TimesheetEntry::durationSeconds).sum();
        return new TimesheetReport(query, List.copyOf(entries), total);
    }
}
