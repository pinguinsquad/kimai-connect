package de.p10d.kimai.cli;

import de.p10d.kimai.client.KimaiProperties;
import de.p10d.kimai.core.DurationParser;
import de.p10d.kimai.core.TimeTrackingService;
import de.p10d.kimai.core.TimesheetDraft;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(
    name = "add",
    description = "Erfasst einen Timesheet-Eintrag in Kimai.",
    synopsisHeading = "Aufruf: ",
    optionListHeading = "Optionen:%n",
    versionProvider = VersionProvider.class)
public class AddCommand implements Callable<Integer> {

    @Option(names = "--date", paramLabel = "DATUM",
        description = "Datum (yyyy-MM-dd), Standard: heute")
    String date;

    @Option(names = {"-s", "--start"}, paramLabel = "HH:mm",
        description = "Beginn, Pflicht")
    String start;

    @Option(names = {"-e", "--end"}, paramLabel = "HH:mm",
        description = "Ende; alternativ --duration")
    String end;

    @Option(names = "--duration", paramLabel = "DAUER",
        description = "Dauer statt Ende, z. B. 3h30m, 2h, 45m oder 1:30")
    String duration;

    @Option(names = {"-p", "--project"}, paramLabel = "NAME|ID",
        description = "Projekt, Pflicht (Name muss eindeutig sein; kimai projects listet sie)")
    String project;

    @Option(names = {"-a", "--activity"}, paramLabel = "NAME|ID",
        description = "Tätigkeit, Pflicht (Name muss eindeutig sein; kimai activities listet sie)")
    String activity;

    @Option(names = {"-d", "--description"}, paramLabel = "TEXT",
        description = "Beschreibung")
    String description;

    @Option(names = {"-u", "--user"}, paramLabel = "ID",
        description = "Für diesen Kimai-User erfassen (braucht Berechtigung); Standard: API-User")
    Long userId;

    @Option(names = "--tag", paramLabel = "TAG",
        description = "Tag, mehrfach möglich")
    List<String> tags = List.of();

    @Option(names = "--not-billable",
        description = "Als nicht abrechenbar erfassen")
    boolean notBillable;

    @Option(names = "--json",
        description = "Ausgabe als JSON statt Tabelle")
    boolean json;

    @Option(names = {"-h", "--help"}, usageHelp = true,
        description = "Zeigt diese Hilfe an und beendet das Programm")
    boolean helpRequested;

    @Option(names = {"-V", "--version"}, versionHelp = true,
        description = "Zeigt die Version an und beendet das Programm")
    boolean versionRequested;

    @Spec
    CommandSpec spec;

    private final TimeTrackingService service;
    private final TextRenderer textRenderer;
    private final JsonRenderer jsonRenderer;
    private final KimaiProperties kimaiProperties;

    public AddCommand(TimeTrackingService service, TextRenderer textRenderer,
                      JsonRenderer jsonRenderer, KimaiProperties kimaiProperties) {
        this.service = service;
        this.textRenderer = textRenderer;
        this.jsonRenderer = jsonRenderer;
        this.kimaiProperties = kimaiProperties;
    }

    @Override
    public Integer call() {
        var draft = buildDraft();

        if (kimaiProperties.token() == null || kimaiProperties.token().isBlank()) {
            spec.commandLine().getErr().println("❌ KIMAI_API_TOKEN ist nicht gesetzt.");
            return 1;
        }

        var created = service.record(draft);
        spec.commandLine().getOut().println(json ? jsonRenderer.render(created) : textRenderer.render(created));
        return 0;
    }

    private TimesheetDraft buildDraft() {
        if (start == null) {
            throw usageError("Der Beginn muss angegeben werden (--start HH:mm).");
        }
        if ((end == null) == (duration == null)) {
            throw usageError("Entweder --end oder --duration angeben.");
        }
        if (project == null || project.isBlank()) {
            throw usageError("Ein Projekt muss angegeben werden (--project NAME|ID).");
        }
        if (activity == null || activity.isBlank()) {
            throw usageError("Eine Tätigkeit muss angegeben werden (--activity NAME|ID).");
        }
        LocalDate day = date == null ? LocalDate.now() : parseDate(date);
        LocalDateTime begin = day.atTime(parseTime(start));
        LocalDateTime finish;
        try {
            finish = end != null ? day.atTime(parseTime(end)) : begin.plus(DurationParser.parse(duration));
            return new TimesheetDraft(begin, finish, project, activity, description, userId, tags, !notBillable);
        } catch (IllegalArgumentException e) {
            throw usageError(e.getMessage());
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw usageError("Ungültiges Datum „" + value + "“ — erwartet wird yyyy-MM-dd.");
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            throw usageError("Ungültige Uhrzeit „" + value + "“ — erwartet wird HH:mm.");
        }
    }

    private ParameterException usageError(String message) {
        return new ParameterException(spec.commandLine(), "❌ " + message);
    }
}
