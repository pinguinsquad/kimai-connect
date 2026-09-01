package de.p10d.kimai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Rohformat der Antwort auf POST /timesheets: Beginn/Ende mit Offset wie bei
 * GET, user als ID, tags als Liste.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record KimaiApiTimesheet(
    long id,
    String begin,
    String end,
    Long duration,
    String description,
    Long user,
    List<String> tags,
    Boolean billable) {
}
