package de.p10d.kimai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Rohformat eines Projekts der Kimai-API (GET /projects): customer ist die
 * Kunden-ID, parentTitle der Kundenname.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record KimaiApiProject(long id, String name, Long customer, String parentTitle) {
}
