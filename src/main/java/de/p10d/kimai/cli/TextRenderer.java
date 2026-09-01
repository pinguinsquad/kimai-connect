package de.p10d.kimai.cli;

import de.p10d.kimai.core.ActivityInfo;
import de.p10d.kimai.core.CreatedTimesheet;
import de.p10d.kimai.core.ProjectInfo;
import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetReport;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Menschenlesbare Tabellenausgabe (Spec 001 FA-5) für Reports, angelegte
 * Einträge sowie Projekt- und Tätigkeitslisten.
 */
@Component
public class TextRenderer {

    private static final DateTimeFormatter GERMAN_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
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
        rows.add(new String[]{"Gesamt", "", "", "", "", "", formatDuration(report.totalDurationSeconds())});
        return table(rows, Set.of(DURATION_COLUMN), rows.size() - 1);
    }

    /** Der angelegte Eintrag als einzeilige Tabelle mit den von Kimai gespeicherten Werten. */
    public String render(CreatedTimesheet created) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Datum", "Von", "Bis", "Dauer", "Projekt", "Tätigkeit", "Beschreibung", "Tags"});
        rows.add(new String[]{
            String.valueOf(created.id()),
            created.begin().format(GERMAN_DATE),
            created.begin().format(TIME),
            created.end().format(TIME),
            formatDuration(created.durationSeconds()),
            created.project().name(),
            created.activity().name(),
            singleLine(created.description()),
            String.join(", ", created.tags())
        });
        String note = created.billable() ? "" : "\nNicht abrechenbar.";
        return table(rows, Set.of(0, 4), -1) + note;
    }

    public String renderProjects(List<ProjectInfo> projects) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Kunde", "Projekt"});
        for (ProjectInfo project : projects) {
            rows.add(new String[]{
                String.valueOf(project.id()),
                project.customerName() == null ? "" : project.customerName(),
                project.name()
            });
        }
        return table(rows, Set.of(0), -1);
    }

    public String renderActivities(List<ActivityInfo> activities) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"ID", "Tätigkeit", "Projekt"});
        for (ActivityInfo activity : activities) {
            rows.add(new String[]{
                String.valueOf(activity.id()),
                activity.name(),
                activity.global() ? "(global)" : String.valueOf(activity.projectId())
            });
        }
        return table(rows, Set.of(0), -1);
    }

    /**
     * Formatiert Zeilen gleicher Länge als Tabelle; die erste Zeile ist der
     * Kopf, vor separatorBeforeRow (falls ≥ 0) steht eine Trennlinie.
     */
    private static String table(List<String[]> rows, Set<Integer> rightAligned, int separatorBeforeRow) {
        int[] widths = columnWidths(rows);
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < rows.size(); i++) {
            if (i == separatorBeforeRow) {
                output.append("-".repeat(totalWidth(widths))).append('\n');
            }
            output.append(formatRow(rows.get(i), widths, rightAligned)).append('\n');
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
        int[] widths = new int[rows.getFirst().length];
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

    private static String formatRow(String[] row, int[] widths, Set<Integer> rightAligned) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < row.length; i++) {
            if (i > 0) {
                line.append("  ");
            }
            line.append(pad(row[i], widths[i], rightAligned.contains(i)));
        }
        return line.toString().stripTrailing();
    }

    private static String pad(String value, int width, boolean rightAlign) {
        String padding = " ".repeat(width - value.length());
        return rightAlign ? padding + value : value + padding;
    }
}
