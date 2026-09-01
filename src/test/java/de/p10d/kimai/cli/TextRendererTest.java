package de.p10d.kimai.cli;

import de.p10d.kimai.core.ActivityInfo;
import de.p10d.kimai.core.CreatedTimesheet;
import de.p10d.kimai.core.ProjectInfo;
import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.TimesheetReport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextRendererTest {

    private static final TimesheetQuery QUERY = new TimesheetQuery(
        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

    private final TextRenderer renderer = new TextRenderer();

    @Test
    void tabelleMitKopfzeileDeutschenFormatenUndSummenzeile() {
        var report = TimesheetReport.of(QUERY, List.of(
            entry(LocalDateTime.of(2026, 7, 1, 9, 0), 12600, "Prüfung Übergabe"),
            entry(LocalDateTime.of(2026, 7, 15, 14, 0), 5400, "Abstimmung")));

        var output = renderer.render(report);
        var lines = output.lines().toList();

        // Kopfzeile mit allen Spalten (FA-5)
        assertThat(lines.getFirst())
            .contains("Datum").contains("User").contains("Kunde").contains("Projekt")
            .contains("Aktivität").contains("Beschreibung").contains("Dauer");
        // deutsche Formate und Umlaute
        assertThat(output).contains("01.07.2026").contains("15.07.2026");
        assertThat(output).contains("Prüfung Übergabe").contains("Müller GmbH");
        // Dauer h:mm
        assertThat(output).contains("3:30").contains("1:30");
        // Summenzeile: 12600 + 5400 = 18000 Sekunden = 5:00
        assertThat(lines.getLast()).contains("Gesamt").contains("5:00");
    }

    @Test
    void ohneEintraegeErscheinenKopfUndSummenzeile() {
        var report = TimesheetReport.of(QUERY, List.of());

        var output = renderer.render(report);
        var lines = output.lines().toList();

        // Kopfzeile, Trennlinie, Summenzeile
        assertThat(lines).hasSize(3);
        assertThat(lines.getFirst()).contains("Datum").contains("Dauer");
        assertThat(lines.getLast()).contains("Gesamt").contains("0:00");
    }

    @Test
    void mehrzeiligeBeschreibungBleibtEineTabellenzeile() {
        var report = TimesheetReport.of(QUERY, List.of(
            entry(LocalDateTime.of(2026, 7, 6, 9, 0), 3600, "ordner-struktur\ndocker image bauen")));

        var output = renderer.render(report);

        assertThat(output).contains("ordner-struktur; docker image bauen");
        // Kopf, ein Eintrag, Trennlinie, Summenzeile
        assertThat(output.lines()).hasSize(4);
    }

    @Test
    void fehlendeBeschreibungBrichtNicht() {
        var report = TimesheetReport.of(QUERY, List.of(
            entry(LocalDateTime.of(2026, 7, 1, 9, 0), 3600, null)));

        assertThat(renderer.render(report)).contains("01.07.2026").contains("1:00");
    }

    @Test
    void angelegterEintragAlsEinzeiligeTabelle() {
        var begin = LocalDateTime.of(2026, 8, 28, 9, 0);
        var created = new CreatedTimesheet(4711, begin, begin.plusMinutes(210), 12600, "Beratung\nvor Ort",
            new ProjectInfo(1, "Projekt X", 7L, "ACME GmbH"), new ActivityInfo(5, "Entwicklung", null),
            3L, List.of("a", "b"), false);

        var output = renderer.render(created);
        var lines = output.lines().toList();

        assertThat(lines.getFirst()).contains("ID").contains("Datum").contains("Von").contains("Bis")
            .contains("Dauer").contains("Projekt").contains("Tätigkeit").contains("Beschreibung").contains("Tags");
        assertThat(lines.get(1)).contains("4711").contains("28.08.2026").contains("09:00").contains("12:30")
            .contains("3:30").contains("Projekt X").contains("Entwicklung").contains("Beratung; vor Ort")
            .contains("a, b");
        assertThat(lines.getLast()).contains("Nicht abrechenbar");
    }

    @Test
    void projektUndTaetigkeitslisten() {
        var projects = renderer.renderProjects(List.of(
            new ProjectInfo(1, "Website", 10L, "ACME GmbH"), new ProjectInfo(2, "Intern", null, null)));
        assertThat(projects.lines().toList().getFirst()).contains("ID").contains("Kunde").contains("Projekt");
        assertThat(projects).contains("ACME GmbH").contains("Website").contains("Intern");

        var activities = renderer.renderActivities(List.of(
            new ActivityInfo(5, "Entwicklung", null), new ActivityInfo(6, "Review", 1L)));
        assertThat(activities.lines().toList().getFirst()).contains("ID").contains("Tätigkeit").contains("Projekt");
        assertThat(activities).contains("Entwicklung").contains("(global)").contains("Review");
    }

    private static TimesheetEntry entry(LocalDateTime begin, long durationSeconds, String description) {
        return new TimesheetEntry(
            begin,
            begin.plusSeconds(durationSeconds),
            durationSeconds,
            description,
            new TimesheetEntry.User(2, "Erika"),
            new TimesheetEntry.Customer("Müller GmbH", "K-001"),
            new TimesheetEntry.Project(1, "Projekt X", "A-100"),
            new TimesheetEntry.Activity(5, "Entwicklung"),
            null);
    }
}
