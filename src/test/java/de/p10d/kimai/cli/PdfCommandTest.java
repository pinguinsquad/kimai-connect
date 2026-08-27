package de.p10d.kimai.cli;

import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.TimesheetService;
import de.p10d.kimai.core.TimesheetSource;
import de.p10d.kimai.client.KimaiProperties;
import de.p10d.kimai.pdf.TimesheetPdfWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PdfCommandTest {

    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();
    private final RecordingSource source = new RecordingSource();

    @TempDir
    Path tempDir;

    private CommandLine commandLine(String token) {
        var command = new PdfCommand(
            new TimesheetService(source),
            new TimesheetPdfWriter(),
            new KimaiProperties("https://kimai.example/api", token, 100));
        var commandLine = new CommandLine(command);
        commandLine.setOut(new PrintWriter(out));
        commandLine.setErr(new PrintWriter(err));
        return commandLine;
    }

    @Test
    void fehlenderZeitraumFuehrtZuExit2MitHilfe() {
        int exitCode = commandLine("token").execute("--out", tempDir.toString());

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Start- und Enddatum");
        assertThat(source.calls).isZero();
    }

    @Test
    void fehlendesTokenFuehrtZuExit1OhneApiAufruf() {
        int exitCode = commandLine("").execute(
            "--start", "2026-07-01", "--end", "2026-07-31", "--out", tempDir.toString());

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("KIMAI_API_TOKEN");
        assertThat(source.calls).isZero();
    }

    @Test
    void leeresErgebnisGibtHinweisUndKeineDateien() throws Exception {
        int exitCode = commandLine("token").execute(
            "--start", "2026-07-01", "--end", "2026-07-31", "--out", tempDir.toString());

        assertThat(exitCode).isZero();
        assertThat(out.toString()).contains("Keine Einträge");
        try (Stream<Path> files = Files.list(tempDir)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void erzeugtPdfsUndMeldetDieDateien() {
        source.entries = List.of(entry());

        int exitCode = commandLine("token").execute(
            "--start", "2026-07-01", "--end", "2026-07-31", "--out", tempDir.toString());

        assertThat(exitCode).isZero();
        assertThat(tempDir.resolve("Zeitnachweis_ACME_Projekt_X_2026-07-01_2026-07-31.pdf")).exists();
        assertThat(out.toString()).contains("Zeitnachweis_ACME_Projekt_X_2026-07-01_2026-07-31.pdf");
    }

    @Test
    void fehlendesEigenesTemplateFuehrtZuExit1() {
        source.entries = List.of(entry());

        int exitCode = commandLine("token").execute(
            "--start", "2026-07-01", "--end", "2026-07-31",
            "--out", tempDir.toString(), "--template", tempDir.resolve("fehlt.html").toString());

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("Template");
    }

    private static TimesheetEntry entry() {
        return new TimesheetEntry(
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 12, 30),
            12600,
            "Beratung",
            new TimesheetEntry.User(3, "Erika Mustermann"),
            new TimesheetEntry.Customer("ACME", "K-001"),
            new TimesheetEntry.Project(1, "Projekt X", "A-100"),
            new TimesheetEntry.Activity(5, "Entwicklung"),
            null);
    }

    private static class RecordingSource implements TimesheetSource {
        private List<TimesheetEntry> entries = List.of();
        private int calls;

        @Override
        public List<TimesheetEntry> fetch(TimesheetQuery query) {
            calls++;
            return entries;
        }
    }
}
