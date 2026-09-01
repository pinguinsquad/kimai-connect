package de.p10d.kimai.core;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Eine Kimai-Tätigkeit für die Auswahl beim Erfassen von Zeiten.
 *
 * @param id        Kimai-Tätigkeits-ID
 * @param name      Name der Tätigkeit
 * @param projectId Projekt, zu dem die Tätigkeit gehört; null bei globalen Tätigkeiten
 */
public record ActivityInfo(long id, String name, Long projectId) {

    /** true, wenn die Tätigkeit keinem Projekt zugeordnet (global) ist. */
    @JsonProperty
    public boolean global() {
        return projectId == null;
    }
}
