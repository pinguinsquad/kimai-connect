package de.p10d.kimai.core;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Ein Timesheet-Eintrag aus Kimai (Spec 001 FA-6).
 */
public record TimesheetEntry(
    LocalDateTime begin,
    LocalDateTime end,
    long durationSeconds,
    String description,
    User user,
    Customer customer,
    Project project,
    Activity activity,
    Double rate) {

    /** Stundensatz; null, wenn kein Satz vorliegt oder die Dauer 0 ist. */
    @JsonProperty
    public Double ratePerHour() {
        if (rate == null || durationSeconds <= 0) {
            return null;
        }
        return rate / (durationSeconds / 3600.0);
    }

    public record User(long id, String name) {
    }

    public record Customer(String name, String number) {
    }

    public record Project(long id, String name, String orderNumber) {
    }

    public record Activity(long id, String name) {
    }
}
