package de.p10d.kimai.client;

import de.p10d.kimai.core.TimesheetEntry;
import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.TimesheetSource;
import de.p10d.kimai.core.UserInfo;
import de.p10d.kimai.core.UserSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class KimaiClient implements TimesheetSource, UserSource {

    private static final DateTimeFormatter API_DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    // Kimai liefert Zeiten mit Offset ohne Doppelpunkt, z. B. 2026-07-01T09:00:00+0200
    private static final DateTimeFormatter KIMAI_DATE_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final String TOTAL_COUNT_HEADER = "X-Total-Count";

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
        try {
            List<KimaiApiUser> users = restClient.get()
                .uri("/users")
                .header("Authorization", "Bearer " + properties.token())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
            return users == null ? List.of()
                : users.stream().map(user -> new UserInfo(user.id(), user.name())).toList();
        } catch (RestClientResponseException e) {
            throw new KimaiException("Kimai-API-Fehler: HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new KimaiException("Kimai-API nicht erreichbar: " + e.getMessage(), e);
        }
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
        try {
            return restClient.get()
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
                });
        } catch (RestClientResponseException e) {
            throw new KimaiException("Kimai-API-Fehler: HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new KimaiException("Kimai-API nicht erreichbar: " + e.getMessage(), e);
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
