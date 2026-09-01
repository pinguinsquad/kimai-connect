package de.p10d.kimai.core;

/**
 * Vom Kern definierte Schnittstelle zum Anlegen von Timesheet-Einträgen;
 * implementiert vom Kimai-Adapter. Einzige schreibende Operation.
 */
public interface TimesheetWriter {

    /** Legt den Eintrag an und liefert ihn mit den von Kimai gespeicherten Werten zurück. */
    CreatedTimesheet create(NewTimesheet entry);
}
