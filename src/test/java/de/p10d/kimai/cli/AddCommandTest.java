package de.p10d.kimai.cli;

import de.p10d.kimai.client.KimaiException;
import de.p10d.kimai.client.KimaiProperties;
import de.p10d.kimai.core.ActivityInfo;
import de.p10d.kimai.core.CreatedTimesheet;
import de.p10d.kimai.core.NewTimesheet;
import de.p10d.kimai.core.ProjectInfo;
import de.p10d.kimai.core.TimeTrackingService;
import de.p10d.kimai.core.TimesheetWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AddCommandTest {

    private static final ProjectInfo PROJECT = new ProjectInfo(1, "Projekt X", 7L, "ACME GmbH");
    private static final ActivityInfo ACTIVITY = new ActivityInfo(5, "Entwicklung", null);

    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();
    private final RecordingWriter writer = new RecordingWriter();

    private CommandLine commandLine(String token) {
        var service = new TimeTrackingService(() -> List.of(PROJECT), projectId -> List.of(ACTIVITY), writer);
        var command = new AddCommand(service, new TextRenderer(), new JsonRenderer(),
            new KimaiProperties("https://kimai.example/api", token, 100));
        var commandLine = new CommandLine(command);
        commandLine.setOut(new PrintWriter(out));
        commandLine.setErr(new PrintWriter(err));
        commandLine.setExecutionExceptionHandler((exception, cmd, parseResult) -> {
            cmd.getErr().println("❌ " + exception.getMessage());
            return 1;
        });
        return commandLine;
    }

    @Test
    void legtEintragAnUndZeigtIhnAlsTabelle() {
        int exitCode = commandLine("token").execute(
            "--date", "2026-08-28", "--start", "09:00", "--end", "12:30",
            "--project", "Projekt X", "--activity", "Entwicklung",
            "--description", "Beratung", "--tag", "a", "--tag", "b", "--user", "3");

        assertThat(exitCode).isZero();
        assertThat(writer.last.begin()).isEqualTo(LocalDateTime.of(2026, 8, 28, 9, 0));
        assertThat(writer.last.end()).isEqualTo(LocalDateTime.of(2026, 8, 28, 12, 30));
        assertThat(writer.last.description()).isEqualTo("Beratung");
        assertThat(writer.last.tags()).containsExactly("a", "b");
        assertThat(writer.last.userId()).isEqualTo(3L);
        assertThat(writer.last.billable()).isTrue();
        assertThat(out.toString())
            .contains("ID").contains("4711")
            .contains("28.08.2026").contains("09:00").contains("12:30").contains("3:30")
            .contains("Projekt X").contains("Entwicklung").contains("a, b");
    }

    @Test
    void dauerStattEndeUndNichtAbrechenbar() {
        int exitCode = commandLine("token").execute(
            "--date", "2026-08-28", "--start", "09:00", "--duration", "1h15m",
            "--project", "1", "--activity", "5", "--not-billable");

        assertThat(exitCode).isZero();
        assertThat(writer.last.end()).isEqualTo(LocalDateTime.of(2026, 8, 28, 10, 15));
        assertThat(writer.last.billable()).isFalse();
        assertThat(out.toString()).contains("Nicht abrechenbar");
    }

    @Test
    void ohneDatumGiltHeute() {
        int exitCode = commandLine("token").execute(
            "--start", "09:00", "--end", "10:00", "--project", "1", "--activity", "5");

        assertThat(exitCode).isZero();
        assertThat(writer.last.begin().toLocalDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void jsonOptionSchreibtNurJsonAufStdout() {
        int exitCode = commandLine("token").execute(
            "--date", "2026-08-28", "--start", "09:00", "--end", "10:00",
            "--project", "1", "--activity", "5", "--json");

        assertThat(exitCode).isZero();
        assertThat(out.toString().strip()).startsWith("{").endsWith("}").contains("\"id\":4711");
    }

    @Test
    void endeUndDauerZugleichIstBedienfehler() {
        int exitCode = commandLine("token").execute(
            "--start", "09:00", "--end", "10:00", "--duration", "1h", "--project", "1", "--activity", "5");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Entweder --end oder --duration");
        assertThat(writer.last).isNull();
    }

    @Test
    void wederEndeNochDauerIstBedienfehler() {
        int exitCode = commandLine("token").execute("--start", "09:00", "--project", "1", "--activity", "5");

        assertThat(exitCode).isEqualTo(2);
        assertThat(err.toString()).contains("Entweder --end oder --duration");
    }

    @Test
    void fehlendeOptionenSindBedienfehlerMitDeutscherMeldung() {
        assertThat(commandLine("token").execute("--end", "10:00", "--project", "1", "--activity", "5")).isEqualTo(2);
        assertThat(err.toString()).contains("Beginn").contains("--start");

        assertThat(commandLine("token").execute("--start", "09:00", "--end", "10:00", "--activity", "5")).isEqualTo(2);
        assertThat(err.toString()).contains("--project");

        assertThat(commandLine("token").execute("--start", "09:00", "--end", "10:00", "--project", "1")).isEqualTo(2);
        assertThat(err.toString()).contains("--activity");
    }

    @Test
    void ungueltigeZeitenSindBedienfehler() {
        assertThat(commandLine("token").execute(
            "--date", "28.08.2026", "--start", "09:00", "--end", "10:00", "--project", "1", "--activity", "5"))
            .isEqualTo(2);
        assertThat(err.toString()).contains("Ungültiges Datum").contains("yyyy-MM-dd");

        assertThat(commandLine("token").execute(
            "--start", "9 Uhr", "--end", "10:00", "--project", "1", "--activity", "5")).isEqualTo(2);
        assertThat(err.toString()).contains("Ungültige Uhrzeit").contains("HH:mm");

        assertThat(commandLine("token").execute(
            "--start", "10:00", "--end", "09:00", "--project", "1", "--activity", "5")).isEqualTo(2);
        assertThat(err.toString()).contains("Ende muss nach dem Beginn");

        assertThat(commandLine("token").execute(
            "--start", "09:00", "--duration", "viel", "--project", "1", "--activity", "5")).isEqualTo(2);
        assertThat(err.toString()).contains("Ungültige Dauer");
        assertThat(writer.last).isNull();
    }

    @Test
    void unbekanntesProjektIstLaufzeitfehlerMitVorschlaegen() {
        int exitCode = commandLine("token").execute(
            "--start", "09:00", "--end", "10:00", "--project", "Nix", "--activity", "5");

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("❌").contains("Projekt „Nix“ nicht gefunden").contains("Projekt X");
        assertThat(writer.last).isNull();
    }

    @Test
    void kimaiFehlerIstLaufzeitfehler() {
        writer.failure = new KimaiException("Kimai-API-Fehler: HTTP 403");

        int exitCode = commandLine("token").execute(
            "--start", "09:00", "--end", "10:00", "--project", "1", "--activity", "5");

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("HTTP 403");
    }

    @Test
    void fehlendesTokenFuehrtZuExit1OhneApiAufruf() {
        int exitCode = commandLine("").execute(
            "--start", "09:00", "--end", "10:00", "--project", "1", "--activity", "5");

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("KIMAI_API_TOKEN");
        assertThat(writer.last).isNull();
    }

    @Test
    void hilfeIstVollstaendigDeutsch() {
        int exitCode = commandLine("token").execute("--help");

        assertThat(exitCode).isZero();
        assertThat(out.toString())
            .contains("Aufruf:").contains("Optionen:").contains("--duration").contains("Zeigt diese Hilfe")
            .doesNotContain("Usage:");
    }

    private static class RecordingWriter implements TimesheetWriter {
        private NewTimesheet last;
        private RuntimeException failure;

        @Override
        public CreatedTimesheet create(NewTimesheet entry) {
            if (failure != null) {
                throw failure;
            }
            last = entry;
            return new CreatedTimesheet(4711, entry.begin(), entry.end(), entry.durationSeconds(),
                entry.description(), entry.project(), entry.activity(), entry.userId(), entry.tags(),
                entry.billable());
        }
    }
}
