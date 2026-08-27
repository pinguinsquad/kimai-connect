package de.p10d.kimai.mcp;

import de.p10d.kimai.cli.JsonRenderer;
import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.TimesheetService;
import de.p10d.kimai.core.TimesheetSource;
import de.p10d.kimai.pdf.TimesheetPdfWriter;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PdfToolsTest {

    private final RecordingSource source = new RecordingSource();
    private final PdfTools tools = new PdfTools(
        new TimesheetService(source), new TimesheetPdfWriter(), new JsonRenderer());
    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path outputDir;

    @Test
    void erzeugtPdfsUndLiefertAbsolutePfade() {
        source.entries = List.of(entry());

        var result = tools.generateTimesheetPdfs(
            "2026-07-01", "2026-07-31", null, null, outputDir.toString());

        assertThat(result.isError()).isFalse();
        JsonNode files = mapper.readTree(text(result)).path("files");
        assertThat(files).hasSize(1);
        Path file = Path.of(files.get(0).asText());
        assertThat(file).isAbsolute().exists();
        assertThat(file.getFileName().toString())
            .isEqualTo("Zeitnachweis_ACME_Projekt_X_2026-07-01_2026-07-31.pdf");
    }

    @Test
    void leeresErgebnisMeldetKeineEintraegeUndSchreibtNichts() throws Exception {
        var result = tools.generateTimesheetPdfs(
            "2026-07-01", "2026-07-31", null, null, outputDir.toString());

        assertThat(result.isError()).isFalse();
        assertThat(text(result)).contains("Keine Einträge");
        try (Stream<Path> files = Files.list(outputDir)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void ungueltigesDatumErgibtDeutschenToolFehler() {
        var result = tools.generateTimesheetPdfs(
            "01.07.2026", "2026-07-31", null, null, outputDir.toString());

        assertThat(result.isError()).isTrue();
        assertThat(text(result)).contains("Ungültiges Datum").contains("yyyy-MM-dd");
        assertThat(source.calls).isZero();
    }

    private static String text(CallToolResult result) {
        return ((TextContent) result.content().getFirst()).text();
    }

    private static TimesheetEntry entry() {
        var begin = LocalDateTime.of(2026, 7, 1, 9, 0);
        return new TimesheetEntry(
            begin, begin.plusSeconds(12600), 12600, "Beratung",
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
