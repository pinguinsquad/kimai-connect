package de.p10d.kimai.cli;

import de.p10d.kimai.core.TimesheetQuery;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Gemeinsame Abruf-Optionen der Subkommandos (Spec 001 FA-2/FA-3).
 * Datumswerte als String, damit alle Fehlermeldungen deutsch sind (FA-8).
 */
public class TimesheetQueryMixin {

    @Option(names = {"-s", "--start"}, paramLabel = "DATUM",
        description = "Startdatum (yyyy-MM-dd), Pflicht")
    String start;

    @Option(names = {"-e", "--end"}, paramLabel = "DATUM",
        description = "Enddatum (yyyy-MM-dd), Pflicht")
    String end;

    @Option(names = {"-u", "--user"}, paramLabel = "ID",
        description = "Nur Einträge dieses Users (Kimai-User-ID)")
    Long userId;

    @Option(names = "--all",
        description = "Auch nicht abrechenbare Einträge abrufen")
    boolean all;

    public TimesheetQuery buildQuery(CommandSpec spec) {
        return buildQuery(spec, !all);
    }

    public TimesheetQuery buildQuery(CommandSpec spec, boolean billableOnly) {
        if (start == null || end == null) {
            throw usageError(spec, "Start- und Enddatum müssen beide angegeben werden (--start, --end).");
        }
        try {
            return new TimesheetQuery(parseDate(spec, start), parseDate(spec, end), userId, billableOnly);
        } catch (IllegalArgumentException e) {
            throw usageError(spec, e.getMessage());
        }
    }

    private static LocalDate parseDate(CommandSpec spec, String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw usageError(spec, "Ungültiges Datum „" + value + "“ — erwartet wird yyyy-MM-dd.");
        }
    }

    private static ParameterException usageError(CommandSpec spec, String message) {
        return new ParameterException(spec.commandLine(), "❌ " + message);
    }
}
