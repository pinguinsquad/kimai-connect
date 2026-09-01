package de.p10d.kimai.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Component
@Command(
    name = "kimai-connect",
    description = "Kimai-Timesheets abrufen und erfassen, Zeitnachweise erzeugen, MCP-Server für KI-Clients.",
    synopsisHeading = "Aufruf: ",
    optionListHeading = "Optionen:%n",
    commandListHeading = "Befehle:%n",
    versionProvider = VersionProvider.class,
    subcommands = {ListCommand.class, AddCommand.class, ProjectsCommand.class, ActivitiesCommand.class,
        PdfCommand.class, McpCommand.class})
public class RootCommand implements Callable<Integer> {

    @Option(names = {"-h", "--help"}, usageHelp = true,
        description = "Zeigt diese Hilfe an und beendet das Programm")
    boolean helpRequested;

    @Option(names = {"-V", "--version"}, versionHelp = true,
        description = "Zeigt die Version an und beendet das Programm")
    boolean versionRequested;

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() {
        // ohne Subkommando: Hilfe zeigen, Bedienfehler
        spec.commandLine().usage(spec.commandLine().getErr());
        return 2;
    }
}
