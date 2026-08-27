package de.p10d.kimai.core;

import org.springframework.stereotype.Service;

/**
 * Fachlicher Einstiegspunkt für den Timesheet-Abruf (Spec 001);
 * wird von allen Oberflächen (CLI, später MCP) gemeinsam genutzt.
 */
@Service
public class TimesheetService {

    private final TimesheetSource source;

    public TimesheetService(TimesheetSource source) {
        this.source = source;
    }

    public TimesheetReport fetch(TimesheetQuery query) {
        return TimesheetReport.of(query, source.fetch(query));
    }
}
