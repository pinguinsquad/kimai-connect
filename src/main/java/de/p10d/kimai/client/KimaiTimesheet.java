package de.p10d.kimai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Rohformat eines Timesheet-Eintrags der Kimai-API (GET /timesheets?full=1).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record KimaiTimesheet(
    String begin,
    String end,
    Long duration,
    String description,
    Double rate,
    KimaiUser user,
    KimaiProject project,
    KimaiActivity activity) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KimaiUser(long id, String alias, String username) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KimaiProject(long id, String name, String orderNumber, KimaiCustomer customer) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KimaiCustomer(String name, String number) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KimaiActivity(long id, String name) {
    }
}
