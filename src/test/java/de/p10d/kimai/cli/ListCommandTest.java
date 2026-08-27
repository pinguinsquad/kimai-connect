package de.p10d.kimai.cli;

import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.TimesheetService;
import de.p10d.kimai.core.TimesheetSource;
import de.p10d.kimai.client.KimaiProperties;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListCommandTest {

    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();
    private final RecordingSource source = new RecordingSource();

    private CommandLine commandLine(String token) {
        var command = new ListCommand(
            new TimesheetService(source),
            new TextRenderer(),
            new JsonRenderer(),
            new KimaiProperties("https://kimai.example/api", token, 100));
        var commandLine = new CommandLine(command);
        commandLine.setOut(new PrintWriter(out));
        commandLine.setErr(new PrintWriter(err));
        return commandLine;
    }

    @Test
    void fehlenderZeitraumFuehrtZuExit2MitHilfe() {
        int exitCode = commandLine("token").execute();

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Start- und Enddatum").contains("--start");
        assertThat(source.calls).isZero();
    }

    @Test
    void halberZeitraumFuehrtZuExit2() {
        int exitCode = commandLine("token").execute("--start", "2026-07-01");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Start- und Enddatum");
        assertThat(source.calls).isZero();
    }

    @Test
    void ungueltigesDatumFuehrtZuExit2MitDeutscherMeldung() {
        int exitCode = commandLine("token").execute("--start", "01.07.2026", "--end", "2026-07-31");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Ungültiges Datum").contains("yyyy-MM-dd");
        assertThat(source.calls).isZero();
    }

    @Test
    void endeVorStartFuehrtZuExit2() {
        int exitCode = commandLine("token").execute("--start", "2026-07-31", "--end", "2026-07-01");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Enddatum");
        assertThat(source.calls).isZero();
    }

    @Test
    void fehlendesTokenFuehrtZuExit1OhneApiAufruf() {
        int exitCode = commandLine("").execute("--start", "2026-07-01", "--end", "2026-07-31");

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("KIMAI_API_TOKEN");
        assertThat(source.calls).isZero();
    }

    @Test
    void standardAusgabeIstTabelleMitBillableOnly() {
        source.entries = List.of(entry());

        int exitCode = commandLine("token").execute("--start", "2026-07-01", "--end", "2026-07-31");

        assertThat(exitCode).isZero();
        assertThat(source.lastQuery)
            .isEqualTo(new TimesheetQuery(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), null, true));
        assertThat(out.toString()).contains("Datum").contains("01.07.2026").contains("Gesamt");
    }

    @Test
    void jsonOptionSchreibtNurJsonAufStdout() {
        source.entries = List.of(entry());

        int exitCode = commandLine("token")
            .execute("--start", "2026-07-01", "--end", "2026-07-31", "--json");

        assertThat(exitCode).isZero();
        assertThat(out.toString().strip())
            .startsWith("{")
            .endsWith("}")
            .contains("\"totalDurationSeconds\"");
    }

    @Test
    void hilfeIstVollstaendigDeutsch() {
        int exitCode = commandLine("token").execute("--help");

        assertThat(exitCode).isZero();
        assertThat(out.toString())
            .contains("Aufruf:")
            .contains("Optionen:")
            .contains("Zeigt diese Hilfe")
            .doesNotContain("Show this help")
            .doesNotContain("Usage:");
    }

    @Test
    void userUndAllWerdenInDieQueryUebernommen() {
        int exitCode = commandLine("token")
            .execute("--start", "2026-07-01", "--end", "2026-07-31", "--user", "2", "--all");

        assertThat(exitCode).isZero();
        assertThat(source.lastQuery)
            .isEqualTo(new TimesheetQuery(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), 2L, false));
    }

    private static TimesheetEntry entry() {
        return new TimesheetEntry(
            LocalDateTime.of(2026, 7, 1, 9, 0),
            LocalDateTime.of(2026, 7, 1, 12, 30),
            12600,
            "Beratung",
            new TimesheetEntry.User(2, "Erika"),
            new TimesheetEntry.Customer("ACME GmbH", "K-001"),
            new TimesheetEntry.Project(1, "Projekt X", "A-100"),
            new TimesheetEntry.Activity(5, "Entwicklung"),
            null);
    }

    private static class RecordingSource implements TimesheetSource {
        private List<TimesheetEntry> entries = List.of();
        private TimesheetQuery lastQuery;
        private int calls;

        @Override
        public List<TimesheetEntry> fetch(TimesheetQuery query) {
            calls++;
            lastQuery = query;
            return entries;
        }
    }
}
