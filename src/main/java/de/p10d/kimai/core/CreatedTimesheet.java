package de.p10d.kimai.core;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Ein in Kimai angelegter Timesheet-Eintrag. Beginn, Ende und Dauer sind die
 * von Kimai gespeicherten Werte – sie können durch Rundungsregeln der
 * Kimai-Konfiguration von der Eingabe abweichen.
 */
public record CreatedTimesheet(
    long id,
    LocalDateTime begin,
    LocalDateTime end,
    long durationSeconds,
    String description,
    ProjectInfo project,
    ActivityInfo activity,
    Long userId,
    List<String> tags,
    boolean billable) {

    public CreatedTimesheet {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
