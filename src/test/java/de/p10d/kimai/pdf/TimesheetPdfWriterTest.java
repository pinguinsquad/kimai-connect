package de.p10d.kimai.pdf;

import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.TimesheetReport;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimesheetPdfWriterTest {

    private static final TimesheetQuery QUERY = new TimesheetQuery(
        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

    private final TimesheetPdfWriter writer = new TimesheetPdfWriter();

    @TempDir
    Path outputDir;

    @Test
    void proProjektEineDateiMitKundeProjektZeitraumUndUmlauten() {
        var report = TimesheetReport.of(QUERY, List.of(
            entry("Müller GmbH", 1, "K1 Migration", "Tech Lead", day(1), 7200),
            entry("Müller GmbH", 2, "Prüf-Projekt", "Review", day(2), 3600)));

        List<Path> files = writer.write(report, outputDir, null);

        assertThat(files).hasSize(2);
        assertThat(files).allSatisfy(file -> assertThat(file).exists());
        assertThat(files.getFirst().getFileName().toString())
            .isEqualTo("Zeitnachweis_Müller_GmbH_K1_Migration_2026-07-01_2026-07-31.pdf");
        assertThat(files.getLast().getFileName().toString())
            .isEqualTo("Zeitnachweis_Müller_GmbH_Prüf-Projekt_2026-07-01_2026-07-31.pdf");
    }

    @Test
    void pdfEnthaeltKopfdatenSummenUndChronologischeZeilen() throws IOException {
        var report = TimesheetReport.of(QUERY, List.of(
            entry("Müller GmbH", 1, "K1 Migration", "Review", day(15), 5400),
            entry("Müller GmbH", 1, "K1 Migration", "Tech Lead", day(1), 7200),
            entry("Müller GmbH", 1, "K1 Migration", "Tech Lead", day(2), 3600)));

        List<Path> files = writer.write(report, outputDir, null);

        assertThat(files).hasSize(1);
        String text = pdfText(files.getFirst());
        // Kopfdaten
        assertThat(text)
            .contains("Zeitnachweis")
            .contains("Müller GmbH").contains("K1 Migration")
            .contains("01.07.2026 - 31.07.2026")
            .contains("04:30"); // Gesamt: 7200+3600+5400 s
        // Zusammenfassung je Tätigkeit
        assertThat(text).contains("Review").contains("01:30");
        assertThat(text).contains("Tech Lead").contains("03:00");
        // Detailzeilen chronologisch: 01.07. vor 02.07. vor 15.07.
        // „Erstellt am <heute>" vorher entfernen — das Tagesdatum könnte
        // sonst mit einem Eintragsdatum kollidieren (Fund vom 15.07.)
        String rows = text.replaceFirst("Erstellt am \\d{2}\\.\\d{2}\\.\\d{4}", "");
        assertThat(rows.indexOf("01.07.2026"))
            .isLessThan(rows.indexOf("02.07.2026"));
        assertThat(rows.indexOf("02.07.2026"))
            .isLessThan(rows.indexOf("15.07.2026"));
    }

    @Test
    void eigenesTemplateWirdVerwendet() throws IOException {
        Path template = outputDir.resolve("eigenes.html");
        Files.writeString(template, """
            <html xmlns:th="http://www.thymeleaf.org">
            <body><p th:text="'EIGENES TEMPLATE für ' + ${projectName}"/></body></html>
            """);
        var report = TimesheetReport.of(QUERY, List.of(
            entry("ACME", 1, "Projekt X", "Dev", day(1), 3600)));

        List<Path> files = writer.write(report, outputDir, template);

        assertThat(pdfText(files.getFirst())).contains("EIGENES TEMPLATE für Projekt X");
    }

    @Test
    void fehlendesEigenesTemplateIstEinFehler() {
        var report = TimesheetReport.of(QUERY, List.of(
            entry("ACME", 1, "Projekt X", "Dev", day(1), 3600)));

        assertThatThrownBy(() -> writer.write(report, outputDir, outputDir.resolve("fehlt.html")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Template");
    }

    private String pdfText(Path file) throws IOException {
        try (var document = Loader.loadPDF(file.toFile())) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    private static LocalDateTime day(int dayOfMonth) {
        return LocalDateTime.of(2026, 7, dayOfMonth, 9, 0);
    }

    private static TimesheetEntry entry(String customer, long projectId, String project,
                                        String activity, LocalDateTime begin, long durationSeconds) {
        return new TimesheetEntry(
            begin,
            begin.plusSeconds(durationSeconds),
            durationSeconds,
            "Arbeit an " + activity,
            new TimesheetEntry.User(3, "Knut Most"),
            new TimesheetEntry.Customer(customer, "K-001"),
            new TimesheetEntry.Project(projectId, project, "A-100"),
            new TimesheetEntry.Activity(activity.hashCode(), activity),
            null);
    }
}
