package de.p10d.kimai.core;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

/**
 * Fachlicher Einstiegspunkt zum Erfassen von Zeiten: Projekte und Tätigkeiten
 * nachschlagen, Referenzen (Name oder ID) auflösen und Einträge anlegen.
 * Wird von CLI und MCP gemeinsam genutzt.
 */
@Service
public class TimeTrackingService {

    private static final int MAX_SUGGESTIONS = 15;

    private final ProjectSource projectSource;
    private final ActivitySource activitySource;
    private final TimesheetWriter writer;

    public TimeTrackingService(ProjectSource projectSource, ActivitySource activitySource,
                               TimesheetWriter writer) {
        this.projectSource = projectSource;
        this.activitySource = activitySource;
        this.writer = writer;
    }

    /**
     * Projekte, optional auf einen Kunden eingeschränkt (Kunden-ID oder
     * Namensbestandteil, Groß-/Kleinschreibung egal).
     */
    public List<ProjectInfo> listProjects(String customer) {
        List<ProjectInfo> projects = projectSource.listProjects();
        if (customer == null || customer.isBlank()) {
            return projects;
        }
        String needle = customer.strip();
        if (isId(needle)) {
            long id = Long.parseLong(needle);
            return projects.stream().filter(p -> p.customerId() != null && p.customerId() == id).toList();
        }
        return projects.stream()
            .filter(p -> p.customerName() != null && containsIgnoreCase(p.customerName(), needle))
            .toList();
    }

    /** Tätigkeiten, optional die eines Projekts (Name oder ID) einschließlich der globalen. */
    public List<ActivityInfo> listActivities(String project) {
        if (project == null || project.isBlank()) {
            return activitySource.listActivities(null);
        }
        return activitySource.listActivities(resolveProject(project).id());
    }

    /** Löst Projekt und Tätigkeit auf und legt den Eintrag an. */
    public CreatedTimesheet record(TimesheetDraft draft) {
        ProjectInfo project = resolveProject(draft.project());
        ActivityInfo activity = resolveActivity(draft.activity(), project);
        return writer.create(new NewTimesheet(
            draft.begin(), draft.end(), project, activity,
            draft.description(), draft.userId(), draft.tags(), draft.billable()));
    }

    /**
     * Findet ein Projekt per ID oder Name. Ein Name muss eindeutig sein: erst
     * exakter Treffer, sonst Namensbestandteil; bei mehreren Treffern Fehler
     * mit den Kandidaten, bei keinem Fehler mit den verfügbaren Projekten.
     */
    public ProjectInfo resolveProject(String reference) {
        return resolve("Projekt", reference, projectSource.listProjects(),
            ProjectInfo::id, ProjectInfo::name, TimeTrackingService::describe);
    }

    /**
     * Findet eine Tätigkeit des Projekts (oder eine globale) per ID oder Name.
     * Passen eine projekteigene und eine globale Tätigkeit exakt, gewinnt die
     * projekteigene.
     */
    public ActivityInfo resolveActivity(String reference, ProjectInfo project) {
        List<ActivityInfo> candidates = activitySource.listActivities(project.id());
        return resolve("Tätigkeit", reference, candidates,
            ActivityInfo::id, ActivityInfo::name, TimeTrackingService::describe);
    }

    private static <T> T resolve(String kind, String reference, List<T> candidates, ToLongFunction<T> id,
                                 Function<T, String> name, Function<T, String> describe) {
        String needle = reference == null ? "" : reference.strip();
        if (needle.isEmpty()) {
            throw new IllegalArgumentException(kind + " muss angegeben werden (Name oder ID).");
        }
        if (isId(needle)) {
            long wanted = Long.parseLong(needle);
            return candidates.stream()
                .filter(c -> id.applyAsLong(c) == wanted)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    kind + " mit ID " + needle + " nicht gefunden." + available(kind, candidates, describe)));
        }
        List<T> exact = candidates.stream()
            .filter(c -> name.apply(c) != null && name.apply(c).strip().equalsIgnoreCase(needle))
            .toList();
        if (exact.size() == 1) {
            return exact.getFirst();
        }
        if (exact.size() > 1) {
            List<T> specific = exact.stream().filter(c -> !isGlobal(c)).toList();
            if (specific.size() == 1) {
                return specific.getFirst();
            }
            throw ambiguous(kind, needle, exact, describe);
        }
        List<T> partial = candidates.stream()
            .filter(c -> name.apply(c) != null && containsIgnoreCase(name.apply(c), needle))
            .toList();
        if (partial.size() == 1) {
            return partial.getFirst();
        }
        if (partial.size() > 1) {
            throw ambiguous(kind, needle, partial, describe);
        }
        throw new IllegalArgumentException(
            kind + " „" + reference + "“ nicht gefunden." + available(kind, candidates, describe));
    }

    private static <T> IllegalArgumentException ambiguous(String kind, String needle, List<T> matches,
                                                          Function<T, String> describe) {
        return new IllegalArgumentException(
            kind + " „" + needle + "“ ist nicht eindeutig. Treffer:\n" + listing(matches, describe)
                + "\nBitte den vollständigen Namen oder die ID angeben.");
    }

    private static <T> String available(String kind, List<T> candidates, Function<T, String> describe) {
        if (candidates.isEmpty()) {
            return " Es sind keine " + plural(kind) + " verfügbar.";
        }
        String more = candidates.size() > MAX_SUGGESTIONS
            ? "\n  … und " + (candidates.size() - MAX_SUGGESTIONS) + " weitere" : "";
        return " Verfügbare " + plural(kind) + ":\n"
            + listing(candidates.subList(0, Math.min(MAX_SUGGESTIONS, candidates.size())), describe) + more;
    }

    private static <T> String listing(List<T> items, Function<T, String> describe) {
        return items.stream().map(item -> "  " + describe.apply(item)).collect(Collectors.joining("\n"));
    }

    private static String plural(String kind) {
        return kind.equals("Projekt") ? "Projekte" : "Tätigkeiten";
    }

    private static String describe(ProjectInfo project) {
        String customer = project.customerName() == null ? "" : project.customerName() + " / ";
        return project.id() + "  " + customer + project.name();
    }

    private static String describe(ActivityInfo activity) {
        return activity.id() + "  " + activity.name() + (activity.global() ? " (global)" : "");
    }

    private static boolean isGlobal(Object item) {
        return item instanceof ActivityInfo activity && activity.global();
    }

    private static boolean isId(String value) {
        return !value.isEmpty() && value.chars().allMatch(Character::isDigit);
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }
}
