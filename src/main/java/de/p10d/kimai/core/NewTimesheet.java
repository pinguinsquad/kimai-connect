package de.p10d.kimai.core;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ein anzulegender Timesheet-Eintrag mit aufgelöstem Projekt und Tätigkeit;
 * Eingabe für {@link TimesheetWriter}.
 *
 * @param begin       Beginn (lokale Zeit des Kimai-Users)
 * @param end         Ende, muss nach dem Beginn liegen
 * @param project     Projekt; mindestens die ID muss gesetzt sein
 * @param activity    Tätigkeit; mindestens die ID muss gesetzt sein
 * @param description Beschreibung, optional
 * @param userId      Kimai-User, für den erfasst wird; null = der API-User selbst
 * @param tags        Tags, leer wenn keine
 * @param billable    abrechenbar
 */
public record NewTimesheet(
    LocalDateTime begin,
    LocalDateTime end,
    ProjectInfo project,
    ActivityInfo activity,
    String description,
    Long userId,
    List<String> tags,
    boolean billable) {

    public NewTimesheet {
        requireValidRange(begin, end);
        if (project == null) {
            throw new IllegalArgumentException("Ein Projekt muss angegeben werden.");
        }
        if (activity == null) {
            throw new IllegalArgumentException("Eine Tätigkeit muss angegeben werden.");
        }
        tags = tags == null ? List.of() : List.copyOf(tags);
    }

    public long durationSeconds() {
        return Duration.between(begin, end).getSeconds();
    }

    static void requireValidRange(LocalDateTime begin, LocalDateTime end) {
        if (begin == null || end == null) {
            throw new IllegalArgumentException("Beginn und Ende müssen beide angegeben werden.");
        }
        if (!end.isAfter(begin)) {
            throw new IllegalArgumentException("Das Ende muss nach dem Beginn liegen.");
        }
    }
}
