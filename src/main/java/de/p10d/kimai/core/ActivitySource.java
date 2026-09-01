package de.p10d.kimai.core;

import java.util.List;

/**
 * Vom Kern definierte Schnittstelle zur Tätigkeitsliste;
 * implementiert vom Kimai-Adapter.
 */
public interface ActivitySource {

    /**
     * Sichtbare Tätigkeiten. Mit Projekt-ID die Tätigkeiten dieses Projekts
     * einschließlich der globalen, ohne Projekt-ID alle.
     */
    List<ActivityInfo> listActivities(Long projectId);
}
