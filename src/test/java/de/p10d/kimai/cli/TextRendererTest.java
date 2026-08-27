package de.p10d.kimai.cli;

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
