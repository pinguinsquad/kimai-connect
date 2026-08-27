package de.p10d.kimai.cli;

import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetReport;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Menschenlesbare Tabellenausgabe (Spec 001 FA-5).
 */
@Component
public class TextRenderer {

    private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String[] HEADER =
        {"Datum", "User", "Kunde", "Projekt", "Aktivität", "Beschreibung", "Dauer"};
    private static final int DURATION_COLUMN = HEADER.length - 1;

    public String render(TimesheetReport report) {
        List<String[]> rows = new ArrayList<>();
        rows.add(HEADER);
        for (TimesheetEntry entry : report.entries()) {
            rows.add(new String[]{
                entry.begin().format(GERMAN_DATE),
                entry.user().name(),
                entry.customer().name(),
                entry.project().name(),
                entry.activity().name(),
                singleLine(entry.description()),
                formatDuration(entry.durationSeconds())
            });
        }
        String[] sumRow = {"Gesamt", "", "", "", "", "", formatDuration(report.totalDurationSeconds())};
        rows.add(sumRow);

        int[] widths = columnWidths(rows);
        StringBuilder output = new StringBuilder();
        for (String[] row : rows) {
            if (row == sumRow) {
                output.append("-".repeat(totalWidth(widths))).append('\n');
            }
            output.append(formatRow(row, widths)).append('\n');
        }
        return output.toString();
    }

    private static String singleLine(String description) {
        if (description == null) {
            return "";
        }
        return String.join("; ", description.lines().map(String::strip).toList());
    }

    private static String formatDuration(long seconds) {
        return "%d:%02d".formatted(seconds / 3600, (seconds % 3600) / 60);
    }

    private static int[] columnWidths(List<String[]> rows) {
        int[] widths = new int[HEADER.length];
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }
        return widths;
    }

    private static int totalWidth(int[] widths) {
        int total = 2 * (widths.length - 1);
        for (int width : widths) {
            total += width;
        }
        return total;
    }

    private static String formatRow(String[] row, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < row.length; i++) {
            if (i > 0) {
                line.append("  ");
            }
            if (i == DURATION_COLUMN) {
                line.append(pad(row[i], widths[i], true));
            } else {
                line.append(pad(row[i], widths[i], false));
            }
        }
        return line.toString().stripTrailing();
    }

    private static String pad(String value, int width, boolean rightAlign) {
        String padding = " ".repeat(width - value.length());
        return rightAlign ? padding + value : value + padding;
    }
}
