package de.p10d.kimai.core;

import java.util.List;

/**
 * Vom Kern definierte Schnittstelle zur Datenquelle (Plan E-4);
 * implementiert vom Kimai-Adapter.
 */
public interface TimesheetSource {

    List<TimesheetEntry> fetch(TimesheetQuery query);
}
