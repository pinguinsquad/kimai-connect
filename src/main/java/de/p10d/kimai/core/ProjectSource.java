package de.p10d.kimai.core;

import java.util.List;

/**
 * Vom Kern definierte Schnittstelle zur Projektliste;
 * implementiert vom Kimai-Adapter.
 */
public interface ProjectSource {

    /** Alle sichtbaren Projekte, auf die der API-User Zugriff hat. */
    List<ProjectInfo> listProjects();
}
