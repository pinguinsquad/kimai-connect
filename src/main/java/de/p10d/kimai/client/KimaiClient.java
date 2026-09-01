package de.p10d.kimai.client;

import de.p10d.kimai.core.ActivityInfo;
import de.p10d.kimai.core.ActivitySource;
import de.p10d.kimai.core.CreatedTimesheet;
import de.p10d.kimai.core.NewTimesheet;
import de.p10d.kimai.core.ProjectInfo;
import de.p10d.kimai.core.ProjectSource;
import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.TimesheetSource;
import de.p10d.kimai.core.TimesheetWriter;
import de.p10d.kimai.core.UserInfo;
import de.p10d.kimai.core.UserSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Kimai-REST-Adapter für alle Ports des Kerns. Lesend für Timesheets, User,
 * Projekte und Tätigkeiten; schreibend nur beim Anlegen von Timesheets.
 */
@Component
public class KimaiClient implements TimesheetSource, UserSource, ProjectSource, ActivitySource, TimesheetWriter {

    private static final DateTimeFormatter API_DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    // Kimai liefert Zeiten mit Offset ohne Doppelpunkt, z. B. 2026-07-01T09:00:00+0200
    private static final DateTimeFormatter KIMAI_DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final String TOTAL_COUNT_HEADER = "X-Total-Count";
    private static final ObjectMapper ERROR_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final KimaiProperties properties;

    public KimaiClient(RestClient.Builder builder, KimaiProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    @Override
    public List<TimesheetEntry> fetch(TimesheetQuery query) {
        requireConfiguration();

        List<TimesheetEntry> entries = new ArrayList<>();
        Integer totalCount = null;
        for (int page = 1; ; page++) {
            ResponseEntity<List<KimaiTimesheet>> response = requestPage(query, page);
            List<KimaiTimesheet> items = response.getBody() == null ? List.of() : response.getBody();
            items.forEach(item -> entries.add(toEntry(item)));

            if (totalCount == null) {
                String header = response.getHeaders().getFirst(TOTAL_COUNT_HEADER);
                if (header != null) {
                    try {
                        totalCount = Integer.parseInt(header);
                    } catch (NumberFormatException ignored) {
                        // Header unbrauchbar — Abbruchkriterium bleibt die nicht volle Seite
                    }
                }
            }
            boolean lastPage = items.size() < properties.pageSize()
                || (totalCount != null && entries.size() >= totalCount);
            if (lastPage) {
                return entries;
            }
        }
    }

    @Override
    public List<UserInfo> listUsers() {
        requireConfiguration();
        List<KimaiApiUser> users = call(() -> restClient.get()
            .uri("/users")
            .header("Authorization", "Bearer " + properties.token())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            }));
        return users == null ? List.of()
            : users.stream().map(user -> new UserInfo(user.id(), user.name())).toList();
    }

    @Override
    public List<ProjectInfo> listProjects() {
        requireConfiguration();
        List<KimaiApiProject> projects = call(() -> restClient.get()
            .uri(uriBuilder -> uriBuilder.path("/projects").queryParam("visible", 1).build())
            .header("Authorization", "Bearer " + properties.token())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            }));
        return projects == null ? List.of()
            : projects.stream()
                .map(p -> new ProjectInfo(p.id(), p.name(), p.customer(), p.parentTitle()))
                .toList();
    }

    @Override
    public List<ActivityInfo> listActivities(Long projectId) {
        requireConfiguration();
        List<KimaiApiActivity> activities = call(() -> restClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder.path("/activities").queryParam("visible", 1);
                if (projectId != null) {
                    builder = builder.queryParam("project", projectId);
                }
                return builder.build();
            })
            .header("Authorization", "Bearer " + properties.token())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {
            }));
        return activities == null ? List.of()
            : activities.stream().map(a -> new ActivityInfo(a.id(), a.name(), a.project())).toList();
    }

    @Override
    public CreatedTimesheet create(NewTimesheet entry) {
        requireConfiguration();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("begin", entry.begin().format(API_DATE_TIME));
        body.put("end", entry.end().format(API_DATE_TIME));
        body.put("project", entry.project().id());
        body.put("activity", entry.activity().id());
        if (entry.description() != null && !entry.description().isBlank()) {
            body.put("description", entry.description());
        }
        if (entry.userId() != null) {
            body.put("user", entry.userId());
        }
        if (!entry.tags().isEmpty()) {
            // die API erwartet Tags als kommagetrennten String
            body.put("tags", String.join(",", entry.tags()));
        }
        body.put("billable", entry.billable());

        KimaiApiTimesheet created = call(() -> restClient.post()
            .uri("/timesheets")
            .header("Authorization", "Bearer " + properties.token())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(KimaiApiTimesheet.class));
        if (created == null) {
            throw new KimaiException("Kimai-API-Fehler: leere Antwort beim Anlegen des Eintrags.");
        }
        LocalDateTime begin = LocalDateTime.parse(created.begin(), KIMAI_DATE_TIME);
        LocalDateTime end = created.end() == null ? entry.end() : LocalDateTime.parse(created.end(), KIMAI_DATE_TIME);
        return new CreatedTimesheet(
            created.id(),
            begin,
            end,
            created.duration() == null ? java.time.Duration.between(begin, end).getSeconds() : created.duration(),
            created.description(),
            entry.project(),
            entry.activity(),
            created.user() == null ? entry.userId() : created.user(),
            created.tags(),
            created.billable() == null ? entry.billable() : created.billable());
    }

    private void requireConfiguration() {
        if (properties.baseUrl() == null || properties.baseUrl().isBlank()) {
            throw new KimaiException("KIMAI_BASE_URL ist nicht gesetzt (z. B. https://kimai.example.org/api).");
        }
        if (properties.token() == null || properties.token().isBlank()) {
            throw new KimaiException("KIMAI_API_TOKEN ist nicht gesetzt.");
        }
    }

    private ResponseEntity<List<KimaiTimesheet>> requestPage(TimesheetQuery query, int page) {
        return call(() -> restClient.get()
            .uri(uriBuilder -> {
                var builder = uriBuilder.path("/timesheets")
                    .queryParam("user", query.userId() == null ? "all" : query.userId())
                    .queryParam("begin", query.start().atStartOfDay().format(API_DATE_TIME))
                    .queryParam("end", query.end().atTime(23, 59, 59).format(API_DATE_TIME))
                    .queryParam("full", "1")
                    .queryParam("size", properties.pageSize())
                    .queryParam("page", page);
                if (query.billableOnly()) {
                    builder = builder.queryParam("billable", "1");
                }
                return builder.build();
            })
            .header("Authorization", "Bearer " + properties.token())
            .retrieve()
            .toEntity(new ParameterizedTypeReference<>() {
            }));
    }

    /** Führt den API-Aufruf aus und übersetzt Fehler in eine KimaiException mit deutscher Meldung. */
    private static <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException e) {
            throw new KimaiException("Kimai-API-Fehler: HTTP " + e.getStatusCode().value()
                + errorDetails(e.getResponseBodyAsString()), e);
        } catch (RestClientException e) {
            throw new KimaiException("Kimai-API nicht erreichbar: " + e.getMessage(), e);
        }
    }

    /**
     * Zieht aus einer Fehlerantwort die Meldung und alle Validierungsfehler
     * (Kimai: {"message": "Validation Failed", "errors": {"children": {"begin": {"errors": [...]}}}}).
     */
    static String errorDetails(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        JsonNode root;
        try {
            root = ERROR_MAPPER.readTree(responseBody);
        } catch (RuntimeException e) {
            return "";
        }
        List<String> details = new ArrayList<>();
        String message = root.path("message").asText(null);
        if (message != null && !message.isBlank()) {
            details.add(message);
        }
        collectErrors(root.path("errors"), "", details);
        return details.isEmpty() ? "" : " – " + String.join("; ", details);
    }

    private static void collectErrors(JsonNode node, String field, List<String> details) {
        if (node.isMissingNode() || node.isNull()) {
            return;
        }
        JsonNode errors = node.path("errors");
        if (errors.isArray()) {
            errors.forEach(error -> details.add((field.isEmpty() ? "" : field + ": ") + error.asText()));
        }
        JsonNode children = node.path("children");
        if (children.isObject()) {
            children.properties().forEach(child -> collectErrors(child.getValue(), child.getKey(), details));
        }
    }

    private TimesheetEntry toEntry(KimaiTimesheet item) {
        return new TimesheetEntry(
            LocalDateTime.parse(item.begin(), KIMAI_DATE_TIME),
            LocalDateTime.parse(item.end(), KIMAI_DATE_TIME),
            item.duration() == null ? 0 : item.duration(),
            item.description(),
            new TimesheetEntry.User(item.user().id(),
                item.user().alias() != null ? item.user().alias() : item.user().username()),
            new TimesheetEntry.Customer(item.project().customer().name(), item.project().customer().number()),
            new TimesheetEntry.Project(item.project().id(), item.project().name(), item.project().orderNumber()),
            new TimesheetEntry.Activity(item.activity().id(), item.activity().name()),
            item.rate());
    }
}
