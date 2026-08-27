package de.p10d.kimai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Rohformat eines Users der Kimai-API (GET /users).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record KimaiApiUser(long id, String alias, String username) {

    String name() {
        return alias != null && !alias.isBlank() ? alias : username;
    }
}
