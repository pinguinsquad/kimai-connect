package de.p10d.kimai.cli;

import de.p10d.kimai.client.KimaiProperties;
import de.p10d.kimai.core.ActivityInfo;
import de.p10d.kimai.core.ProjectInfo;
import de.p10d.kimai.core.TimeTrackingService;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Die Nachschlage-Kommandos projects und activities. */
class LookupCommandsTest {

    private static final ProjectInfo ACME = new ProjectInfo(1, "Website", 10L, "ACME GmbH");
    private static final ProjectInfo MUELLER = new ProjectInfo(2, "Relaunch", 11L, "Müller AG");
    private static final ActivityInfo GLOBAL = new ActivityInfo(5, "Entwicklung", null);
    private static final ActivityInfo SPECIFIC = new ActivityInfo(6, "Review", 1L);

    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();
    private Long requestedProjectId;

    private final TimeTrackingService service = new TimeTrackingService(
        () -> List.of(ACME, MUELLER),
        projectId -> {
            requestedProjectId = projectId;
            return projectId == null ? List.of(GLOBAL, SPECIFIC) : List.of(GLOBAL, SPECIFIC);
        },
        entry -> { throw new UnsupportedOperationException(); });

    private CommandLine commandLine(Object command) {
        var commandLine = new CommandLine(command);
        commandLine.setOut(new PrintWriter(out));
        commandLine.setErr(new PrintWriter(err));
        commandLine.setExecutionExceptionHandler((exception, cmd, parseResult) -> {
            cmd.getErr().println("❌ " + exception.getMessage());
            return 1;
        });
        return commandLine;
    }

    private ProjectsCommand projects(String token) {
        return new ProjectsCommand(service, new TextRenderer(), new JsonRenderer(),
            new KimaiProperties("https://kimai.example/api", token, 100));
    }

    private ActivitiesCommand activities(String token) {
        return new ActivitiesCommand(service, new TextRenderer(), new JsonRenderer(),
            new KimaiProperties("https://kimai.example/api", token, 100));
    }

    @Test
    void projectsListetAlleAlsTabelle() {
        int exitCode = commandLine(projects("token")).execute();

        assertThat(exitCode).isZero();
        var lines = out.toString().lines().toList();
        assertThat(lines.getFirst()).contains("ID").contains("Kunde").contains("Projekt");
        assertThat(out.toString()).contains("ACME GmbH").contains("Website").contains("Müller AG").contains("Relaunch");
    }

    @Test
    void projectsFiltertNachKundeUndKannJson() {
        int exitCode = commandLine(projects("token")).execute("--customer", "müller", "--json");

        assertThat(exitCode).isZero();
        assertThat(out.toString().strip()).startsWith("[").endsWith("]")
            .contains("\"Relaunch\"").doesNotContain("Website");
    }

    @Test
    void activitiesOhneProjektListetAlle() {
        int exitCode = commandLine(activities("token")).execute();

        assertThat(exitCode).isZero();
        assertThat(requestedProjectId).isNull();
        assertThat(out.toString()).contains("Entwicklung").contains("(global)").contains("Review");
    }

    @Test
    void activitiesMitProjektLoestDenNamenAuf() {
        int exitCode = commandLine(activities("token")).execute("--project", "Website");

        assertThat(exitCode).isZero();
        assertThat(requestedProjectId).isEqualTo(1L);
    }

    @Test
    void activitiesMitUnbekanntemProjektIstLaufzeitfehler() {
        int exitCode = commandLine(activities("token")).execute("--project", "Nix");

        assertThat(exitCode).isEqualTo(1);
        assertThat(err.toString()).contains("Projekt „Nix“ nicht gefunden");
    }

    @Test
    void fehlendesTokenFuehrtZuExit1() {
        assertThat(commandLine(projects("")).execute()).isEqualTo(1);
        assertThat(commandLine(activities("")).execute()).isEqualTo(1);
        assertThat(err.toString()).contains("KIMAI_API_TOKEN");
    }

    @Test
    void hilfeIstDeutsch() {
        assertThat(commandLine(projects("token")).execute("--help")).isZero();
        assertThat(commandLine(activities("token")).execute("--help")).isZero();
        assertThat(out.toString()).contains("Aufruf:").doesNotContain("Usage:");
    }
}
