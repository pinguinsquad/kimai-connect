package de.p10d.kimai.client;

import de.p10d.kimai.core.ActivityInfo;
import de.p10d.kimai.core.NewTimesheet;
import de.p10d.kimai.core.ProjectInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Projekte, Tätigkeiten und Anlegen von Einträgen gegen MockRestServiceServer;
 * die lesenden Timesheet-Tests stehen in KimaiClientTest.
 */
class KimaiClientWriteTest {

    private static final String BASE_URL = "https://kimai.example/api";
    private static final ProjectInfo PROJECT = new ProjectInfo(1, "Projekt X", 7L, "ACME GmbH");
    private static final ActivityInfo ACTIVITY = new ActivityInfo(5, "Entwicklung", null);
    private static final LocalDateTime BEGIN = LocalDateTime.of(2026, 8, 28, 9, 0);

    private MockRestServiceServer server;
    private RestClient.Builder builder;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private KimaiClient client(String token) {
        return new KimaiClient(builder, new KimaiProperties(BASE_URL, token, 100));
    }

    @Test
    void listProjectsFragtSichtbareAbUndMapptKundeAusIdUndParentTitle() {
        server.expect(requestTo(startsWith(BASE_URL + "/projects")))
            .andExpect(queryParam("visible", "1"))
            .andExpect(header("Authorization", "Bearer test-token"))
            .andRespond(withSuccess("""
                [
                  {"id": 1, "name": "Projekt X", "customer": 7, "parentTitle": "ACME GmbH", "visible": true},
                  {"id": 2, "name": "Intern", "customer": null, "parentTitle": null, "visible": true}
                ]
                """, MediaType.APPLICATION_JSON));

        var projects = client("test-token").listProjects();

        server.verify();
        assertThat(projects).containsExactly(
            new ProjectInfo(1, "Projekt X", 7L, "ACME GmbH"),
            new ProjectInfo(2, "Intern", null, null));
    }

    @Test
    void listActivitiesMitProjektSetztDenFilterUndErkenntGlobale() {
        server.expect(requestTo(startsWith(BASE_URL + "/activities")))
            .andExpect(queryParam("visible", "1"))
            .andExpect(queryParam("project", "1"))
            .andRespond(withSuccess("""
                [
                  {"id": 5, "name": "Entwicklung", "project": null, "parentTitle": null},
                  {"id": 6, "name": "Review", "project": 1, "parentTitle": "Projekt X"}
                ]
                """, MediaType.APPLICATION_JSON));

        var activities = client("test-token").listActivities(1L);

        server.verify();
        assertThat(activities).containsExactly(
            new ActivityInfo(5, "Entwicklung", null),
            new ActivityInfo(6, "Review", 1L));
        assertThat(activities.getFirst().global()).isTrue();
    }

    @Test
    void listActivitiesOhneProjektSendetKeinenProjektfilter() {
        server.expect(requestTo(startsWith(BASE_URL + "/activities")))
            .andExpect(request -> assertThat(request.getURI().getQuery()).doesNotContain("project"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client("test-token").listActivities(null)).isEmpty();
        server.verify();
    }

    @Test
    void createSendetAlleFelderUndUebernimmtDieGespeichertenWerte() {
        server.expect(requestTo(BASE_URL + "/timesheets"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer test-token"))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.begin").value("2026-08-28T09:00:00"))
            .andExpect(jsonPath("$.end").value("2026-08-28T12:30:00"))
            .andExpect(jsonPath("$.project").value(1))
            .andExpect(jsonPath("$.activity").value(5))
            .andExpect(jsonPath("$.description").value("Beratung"))
            .andExpect(jsonPath("$.user").value(3))
            .andExpect(jsonPath("$.tags").value("a,b"))
            .andExpect(jsonPath("$.billable").value(true))
            .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).body("""
                {
                  "id": 4711,
                  "begin": "2026-08-28T09:00:00+0200",
                  "end": "2026-08-28T12:45:00+0200",
                  "duration": 13500,
                  "description": "Beratung",
                  "user": 3,
                  "activity": 5,
                  "project": 1,
                  "tags": ["a", "b"],
                  "billable": true,
                  "exported": false
                }
                """));

        var created = client("test-token").create(new NewTimesheet(
            BEGIN, BEGIN.plusMinutes(210), PROJECT, ACTIVITY, "Beratung", 3L, List.of("a", "b"), true));

        server.verify();
        assertThat(created.id()).isEqualTo(4711);
        // Kimai hat gerundet: die gespeicherten Werte zählen
        assertThat(created.end()).isEqualTo(LocalDateTime.of(2026, 8, 28, 12, 45));
        assertThat(created.durationSeconds()).isEqualTo(13500);
        assertThat(created.project()).isEqualTo(PROJECT);
        assertThat(created.activity()).isEqualTo(ACTIVITY);
        assertThat(created.userId()).isEqualTo(3L);
        assertThat(created.tags()).containsExactly("a", "b");
        assertThat(created.billable()).isTrue();
    }

    @Test
    void createLaesstOptionaleFelderWeg() {
        server.expect(requestTo(BASE_URL + "/timesheets"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.user").doesNotExist())
            .andExpect(jsonPath("$.tags").doesNotExist())
            .andExpect(jsonPath("$.description").doesNotExist())
            .andExpect(jsonPath("$.billable").value(false))
            .andRespond(withSuccess("""
                {"id": 1, "begin": "2026-08-28T09:00:00+0200", "end": "2026-08-28T10:00:00+0200",
                 "duration": 3600, "user": 2, "tags": [], "billable": false}
                """, MediaType.APPLICATION_JSON));

        var created = client("test-token").create(new NewTimesheet(
            BEGIN, BEGIN.plusHours(1), PROJECT, ACTIVITY, null, null, null, false));

        server.verify();
        assertThat(created.userId()).isEqualTo(2L);
        assertThat(created.tags()).isEmpty();
        assertThat(created.billable()).isFalse();
    }

    @Test
    void validierungsfehlerVonKimaiLandenInDerMeldung() {
        server.expect(requestTo(BASE_URL + "/timesheets"))
            .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body("""
                {"code": 400, "message": "Validation Failed", "errors": {"children": {
                  "begin": {"errors": ["Dieser Wert darf nicht leer sein."]},
                  "end": {},
                  "project": {"errors": ["Bitte wähle ein Projekt."]}
                }}}
                """));

        assertThatThrownBy(() -> client("geheimes-token").create(new NewTimesheet(
            BEGIN, BEGIN.plusHours(1), PROJECT, ACTIVITY, null, null, null, true)))
            .isInstanceOf(KimaiException.class)
            .hasMessageContaining("HTTP 400")
            .hasMessageContaining("Validation Failed")
            .hasMessageContaining("begin: Dieser Wert darf nicht leer sein.")
            .hasMessageContaining("project: Bitte wähle ein Projekt.")
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("geheimes-token"));
    }

    @Test
    void fehlerOhneJsonKoerperBleibtKnapp() {
        server.expect(requestTo(BASE_URL + "/timesheets"))
            .andRespond(withStatus(HttpStatus.FORBIDDEN).body("<html>Forbidden</html>"));

        assertThatThrownBy(() -> client("test-token").create(new NewTimesheet(
            BEGIN, BEGIN.plusHours(1), PROJECT, ACTIVITY, null, 9L, null, true)))
            .isInstanceOf(KimaiException.class)
            .hasMessage("Kimai-API-Fehler: HTTP 403");
    }

    @Test
    void ohneTokenKeinSchreibzugriff() {
        assertThatThrownBy(() -> client("").create(new NewTimesheet(
            BEGIN, BEGIN.plusHours(1), PROJECT, ACTIVITY, null, null, null, true)))
            .isInstanceOf(KimaiException.class)
            .hasMessageContaining("KIMAI_API_TOKEN");

        server.verify(); // keine Requests erwartet, keine erfolgt
    }
}
