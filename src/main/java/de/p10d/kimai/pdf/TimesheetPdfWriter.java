package de.p10d.kimai.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;
import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetReport;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Erzeugt pro Projekt einen PDF-Zeitnachweis (Spec 003).
 */
@Component
public class TimesheetPdfWriter {

    private static final String DEFAULT_TEMPLATE = "templates/zeitnachweis.html";
    private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final TemplateEngine engine;

    public TimesheetPdfWriter() {
        // openhtmltopdf loggt über java.util.logging an Logback vorbei — abschalten
        XRLog.setLoggingEnabled(false);
        // SpringTemplateEngine: SpringEL statt OGNL (der Boot-Starter schließt OGNL aus)
        this.engine = new SpringTemplateEngine();
        var resolver = new StringTemplateResolver();
        resolver.setTemplateMode("HTML");
        engine.setTemplateResolver(resolver);
    }

    /**
     * Schreibt die Nachweise in outputDir; customTemplate null = eingebautes
     * Template. Liefert die erzeugten Dateien (nach Projektname sortiert).
     */
    public List<Path> write(TimesheetReport report, Path outputDir, Path customTemplate) {
        String template = loadTemplate(customTemplate);
        Map<String, List<TimesheetEntry>> byProject = groupByProject(report);

        try {
            Files.createDirectories(outputDir);
            List<Path> files = new ArrayList<>();
            for (var group : byProject.values()) {
                files.add(writeProjectPdf(group, report, template, outputDir));
            }
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException("Ausgabeverzeichnis nicht beschreibbar: " + outputDir, e);
        }
    }

    private Path writeProjectPdf(List<TimesheetEntry> entries, TimesheetReport report,
                                 String template, Path outputDir) throws IOException {
        entries.sort(Comparator.comparing(TimesheetEntry::begin));
        var first = entries.getFirst();

        var context = new Context();
        context.setVariable("customerName", first.customer().name());
        context.setVariable("projectName", first.project().name());
        context.setVariable("rangeBegin", report.query().start().format(GERMAN_DATE));
        context.setVariable("rangeEnd", report.query().end().format(GERMAN_DATE));
        context.setVariable("generatedAt", LocalDate.now().format(GERMAN_DATE));
        context.setVariable("totalHours", formatDuration(
            entries.stream().mapToLong(TimesheetEntry::durationSeconds).sum()));
        context.setVariable("activitySummary", activitySummary(entries));
        context.setVariable("rows", entries.stream().map(entry -> Map.of(
            "day", entry.begin().format(GERMAN_DATE),
            "duration", formatDuration(entry.durationSeconds()),
            "activity", entry.activity().name(),
            "user", entry.user().name(),
            "description", entry.description() == null ? "" : entry.description())).toList());

        String html = engine.process(template, context);
        Path outFile = outputDir.resolve(fileName(first, report));
        try (OutputStream out = Files.newOutputStream(outFile)) {
            var builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, outputDir.toUri().toString());
            builder.toStream(out);
            builder.run();
        }
        return outFile;
    }

    private String fileName(TimesheetEntry first, TimesheetReport report) {
        String raw = "Zeitnachweis_%s_%s_%s_%s.pdf".formatted(
            first.customer().name(), first.project().name(),
            report.query().start().format(ISO_DATE), report.query().end().format(ISO_DATE));
        // Umlaute erhalten, sonstige Sonderzeichen ersetzen (FA-2)
        return raw.replaceAll("[^A-Za-z0-9äöüÄÖÜß._-]+", "_");
    }

    private static Map<String, List<TimesheetEntry>> groupByProject(TimesheetReport report) {
        Map<String, List<TimesheetEntry>> byProject = new TreeMap<>();
        for (TimesheetEntry entry : report.entries()) {
            byProject.computeIfAbsent(entry.project().name(), name -> new ArrayList<>()).add(entry);
        }
        return byProject;
    }

    private static List<Map<String, String>> activitySummary(List<TimesheetEntry> entries) {
        Map<String, Long> byActivity = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (TimesheetEntry entry : entries) {
            byActivity.merge(entry.activity().name(), entry.durationSeconds(), Long::sum);
        }
        return byActivity.entrySet().stream()
            .map(sum -> Map.of("activity", sum.getKey(), "duration", formatDuration(sum.getValue())))
            .toList();
    }

    private static String formatDuration(long seconds) {
        return "%02d:%02d".formatted(seconds / 3600, (seconds % 3600) / 60);
    }

    private String loadTemplate(Path customTemplate) {
        if (customTemplate != null) {
            if (!Files.isRegularFile(customTemplate)) {
                throw new IllegalArgumentException("Template nicht gefunden: " + customTemplate);
            }
            try {
                return Files.readString(customTemplate);
            } catch (IOException e) {
                throw new UncheckedIOException("Template nicht lesbar: " + customTemplate, e);
            }
        }
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(DEFAULT_TEMPLATE)) {
            if (in == null) {
                throw new IllegalStateException("Eingebautes Template fehlt: " + DEFAULT_TEMPLATE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Eingebautes Template nicht lesbar", e);
        }
    }
}
