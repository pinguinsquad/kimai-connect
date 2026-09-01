package de.p10d.kimai.cli;

import de.p10d.kimai.client.KimaiProperties;
import de.p10d.kimai.core.TimeTrackingService;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Component
@Command(
    name = "projects",
    description = "Listet Projekte aus Kimai, um Namen und IDs für „add“ nachzuschlagen.",
    synopsisHeading = "Aufruf: ",
    optionListHeading = "Optionen:%n",
    versionProvider = VersionProvider.class)
public class ProjectsCommand implements Callable<Integer> {

    @Option(names = {"-c", "--customer"}, paramLabel = "NAME|ID",
        description = "Nur Projekte dieses Kunden (Namensbestandteil oder Kunden-ID)")
    String customer;

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

    public ProjectsCommand(TimeTrackingService service, TextRenderer textRenderer,
                           JsonRenderer jsonRenderer, KimaiProperties kimaiProperties) {
        this.service = service;
        this.textRenderer = textRenderer;
        this.jsonRenderer = jsonRenderer;
        this.kimaiProperties = kimaiProperties;
    }

    @Override
    public Integer call() {
        if (kimaiProperties.token() == null || kimaiProperties.token().isBlank()) {
            spec.commandLine().getErr().println("❌ KIMAI_API_TOKEN ist nicht gesetzt.");
            return 1;
        }

        var projects = service.listProjects(customer);
        spec.commandLine().getOut()
            .println(json ? jsonRenderer.render(projects) : textRenderer.renderProjects(projects));
        return 0;
    }
}
