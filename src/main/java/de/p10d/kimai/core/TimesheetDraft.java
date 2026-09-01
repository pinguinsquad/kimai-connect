package de.p10d.kimai.core;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Eingabe zum Erfassen einer Zeit, wie sie von CLI und MCP kommt: Projekt und
 * Tätigkeit als Referenz (Name oder ID), die {@link TimeTrackingService}
 * auflöst.
 *
 * @param begin       Beginn (lokale Zeit des Kimai-Users)
 * @param end         Ende, muss nach dem Beginn liegen
 * @param project     Projekt als Name oder ID
 * @param activity    Tätigkeit als Name oder ID
 * @param description Beschreibung, optional
 * @param userId      Kimai-User, für den erfasst wird; null = der API-User selbst
 * @param tags        Tags, leer wenn keine
 * @param billable    abrechenbar
 */
public record TimesheetDraft(
    LocalDateTime begin,
    LocalDateTime end,
    String project,
    String activity,
    String description,
    Long userId,
    List<String> tags,
    boolean billable) {

    public TimesheetDraft {
        NewTimesheet.requireValidRange(begin, end);
        if (project == null || project.isBlank()) {
            throw new IllegalArgumentException("Ein Projekt muss angegeben werden (Name oder ID).");
        }
        if (activity == null || activity.isBlank()) {
            throw new IllegalArgumentException("Eine Tätigkeit muss angegeben werden (Name oder ID).");
        }
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
