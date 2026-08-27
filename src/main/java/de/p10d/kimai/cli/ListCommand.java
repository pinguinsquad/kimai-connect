package de.p10d.kimai.cli;

import de.p10d.kimai.core.TimesheetService;
import de.p10d.kimai.client.KimaiProperties;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Component
@Command(
    name = "list",
    description = "Listet abrechenbare Timesheet-Einträge aus Kimai.",
    synopsisHeading = "Aufruf: ",
    optionListHeading = "Optionen:%n",
    versionProvider = VersionProvider.class)
public class ListCommand implements Callable<Integer> {

    @Mixin
    TimesheetQueryMixin query;

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

    private final TimesheetService service;
    private final TextRenderer textRenderer;
    private final JsonRenderer jsonRenderer;
    private final KimaiProperties kimaiProperties;

    public ListCommand(TimesheetService service, TextRenderer textRenderer,
                       JsonRenderer jsonRenderer, KimaiProperties kimaiProperties) {
        this.service = service;
        this.textRenderer = textRenderer;
        this.jsonRenderer = jsonRenderer;
        this.kimaiProperties = kimaiProperties;
    }

    @Override
    public Integer call() {
        var timesheetQuery = query.buildQuery(spec);

        if (kimaiProperties.token() == null || kimaiProperties.token().isBlank()) {
            spec.commandLine().getErr().println("❌ KIMAI_API_TOKEN ist nicht gesetzt.");
            return 1;
        }

        var report = service.fetch(timesheetQuery);
        spec.commandLine().getOut().println(json ? jsonRenderer.render(report) : textRenderer.render(report));
        return 0;
    }
}
