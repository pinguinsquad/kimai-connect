package de.p10d.kimai.cli;

import de.p10d.kimai.core.TimesheetService;
import de.p10d.kimai.client.KimaiProperties;
import de.p10d.kimai.pdf.TimesheetPdfWriter;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(
    name = "pdf",
    description = "Erzeugt PDF-Zeitnachweise pro Projekt aus Kimai-Timesheets.",
    synopsisHeading = "Aufruf: ",
    optionListHeading = "Optionen:%n",
    versionProvider = VersionProvider.class)
public class PdfCommand implements Callable<Integer> {

    @Mixin
    TimesheetQueryMixin query;

    @Option(names = {"-o", "--out"}, paramLabel = "VERZEICHNIS",
        description = "Ausgabeverzeichnis für die PDFs (Standard: ${DEFAULT-VALUE})")
    Path outputDir = Path.of("pdf");

    @Option(names = {"-t", "--template"}, paramLabel = "DATEI",
        description = "Eigenes HTML-Template statt des eingebauten")
    Path template;

    @Option(names = {"-h", "--help"}, usageHelp = true,
        description = "Zeigt diese Hilfe an und beendet das Programm")
    boolean helpRequested;

    @Option(names = {"-V", "--version"}, versionHelp = true,
        description = "Zeigt die Version an und beendet das Programm")
    boolean versionRequested;

    @Spec
    CommandSpec spec;

    private final TimesheetService service;
    private final TimesheetPdfWriter writer;
    private final KimaiProperties kimaiProperties;

    public PdfCommand(TimesheetService service, TimesheetPdfWriter writer, KimaiProperties kimaiProperties) {
        this.service = service;
        this.writer = writer;
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
        if (report.entries().isEmpty()) {
            spec.commandLine().getOut().println("Keine Einträge im Zeitraum gefunden — keine PDFs erzeugt.");
            return 0;
        }

        List<Path> files = writer.write(report, outputDir, template);
        var out = spec.commandLine().getOut();
        out.println("Erzeugte Zeitnachweise:");
        files.forEach(file -> out.println("  " + file));
        return 0;
    }
}
