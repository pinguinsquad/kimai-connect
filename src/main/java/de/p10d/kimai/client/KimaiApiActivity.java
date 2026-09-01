package de.p10d.kimai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Rohformat einer Tätigkeit der Kimai-API (GET /activities): project ist die
 * Projekt-ID, null bei globalen Tätigkeiten.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record KimaiApiActivity(long id, String name, Long project) {
}
