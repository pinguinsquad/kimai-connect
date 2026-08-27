package de.p10d.kimai.client;

import de.p10d.kimai.core.TimesheetQuery;
import de.p10d.kimai.core.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KimaiClientTest {

    private static final String BASE_URL = "https://kimai.example/api";
    private static final TimesheetQuery JULI = new TimesheetQuery(
        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

    private MockRestServiceServer server;
    private RestClient.Builder builder;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
    }

    private KimaiClient client(String token, int pageSize) {
        return new KimaiClient(builder, new KimaiProperties(BASE_URL, token, pageSize));
    }

    @Test
    void setztQueryParameterUndTokenUndMapptDieAntwort() {
        server.expect(requestTo(startsWith(BASE_URL + "/timesheets")))
            .andExpect(queryParam("user", "all"))
            .andExpect(queryParam("billable", "1"))
            .andExpect(queryParam("begin", "2026-07-01T00:00:00"))
            .andExpect(queryParam("end", "2026-07-31T23:59:59"))
            .andExpect(queryParam("full", "1"))
            .andExpect(queryParam("size", "100"))
            .andExpect(queryParam("page", "1"))
            .andExpect(header("Authorization", "Bearer test-token"))
            .andRespond(withSuccess(eintrag(262.5), MediaType.APPLICATION_JSON)
                .header("X-Total-Count", "1"));

        var entries = client("test-token", 100).fetch(JULI);

        server.verify();
        assertThat(entries).hasSize(1);
        var entry = entries.getFirst();
        assertThat(entry.begin()).isEqualTo(LocalDateTime.of(2026, 7, 1, 9, 0));
        assertThat(entry.end()).isEqualTo(LocalDateTime.of(2026, 7, 1, 12, 30));
        assertThat(entry.durationSeconds()).isEqualTo(12600);
        assertThat(entry.description()).isEqualTo("Beratung");
        assertThat(entry.user()).isEqualTo(new de.p10d.kimai.core.TimesheetEntry.User(2, "Erika"));
        assertThat(entry.customer().name()).isEqualTo("ACME GmbH");
        assertThat(entry.customer().number()).isEqualTo("K-001");
        assertThat(entry.project().name()).isEqualTo("Projekt X");
        assertThat(entry.project().orderNumber()).isEqualTo("A-100");
        assertThat(entry.activity().name()).isEqualTo("Entwicklung");
        assertThat(entry.rate()).isEqualTo(262.5);
        assertThat(entry.ratePerHour()).isEqualTo(75.0);
    }

    @Test
    void userfilterUndAlleEintraegeSchaltenDieParameterUm() {
        var query = new TimesheetQuery(JULI.start(), JULI.end(), 2L, false);

        server.expect(requestTo(startsWith(BASE_URL + "/timesheets")))
            .andExpect(queryParam("user", "2"))
            .andExpect(request -> assertThat(request.getURI().getQuery()).doesNotContain("billable"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        var entries = client("test-token", 100).fetch(query);

        server.verify();
        assertThat(entries).isEmpty();
    }

    @Test
    void paginiertBisAlleEintraegeGeladenSind() {
        server.expect(requestTo(startsWith(BASE_URL + "/timesheets")))
            .andExpect(queryParam("page", "1"))
            .andRespond(withSuccess(liste(eintragJson("Eintrag 1"), eintragJson("Eintrag 2")),
                MediaType.APPLICATION_JSON).header("X-Total-Count", "3"));
        server.expect(requestTo(startsWith(BASE_URL + "/timesheets")))
            .andExpect(queryParam("page", "2"))
            .andRespond(withSuccess(liste(eintragJson("Eintrag 3")), MediaType.APPLICATION_JSON)
                .header("X-Total-Count", "3"));

        var entries = client("test-token", 2).fetch(JULI);

        server.verify();
        assertThat(entries).hasSize(3);
        assertThat(entries.getLast().description()).isEqualTo("Eintrag 3");
    }

    @Test
    void apiFehlerFuehrtZuVerstaendlicherExceptionOhneToken() {
        server.expect(requestTo(startsWith(BASE_URL + "/timesheets")))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client("geheimes-token", 100).fetch(JULI))
            .isInstanceOf(KimaiException.class)
            .hasMessageContaining("Kimai-API")
            .hasMessageContaining("500")
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("geheimes-token"));
    }

    @Test
    void fehlendesRateFeldBrichtNicht() {
        server.expect(requestTo(startsWith(BASE_URL + "/timesheets")))
            .andRespond(withSuccess(eintrag(null), MediaType.APPLICATION_JSON)
                .header("X-Total-Count", "1"));

        var entries = client("test-token", 100).fetch(JULI);

        assertThat(entries.getFirst().rate()).isNull();
        assertThat(entries.getFirst().ratePerHour()).isNull();
    }

    @Test
    void fehlendeBaseUrlBrichtVorDemApiAufrufAb() {
        var client = new KimaiClient(builder, new KimaiProperties("", "test-token", 100));

        assertThatThrownBy(() -> client.fetch(JULI))
            .isInstanceOf(KimaiException.class)
            .hasMessageContaining("KIMAI_BASE_URL");

        server.verify(); // keine Requests erwartet, keine erfolgt
    }

    @Test
    void fehlendesTokenBrichtVorDemApiAufrufAb() {
        assertThatThrownBy(() -> client("", 100).fetch(JULI))
            .isInstanceOf(KimaiException.class)
            .hasMessageContaining("KIMAI_API_TOKEN");

        server.verify(); // keine Requests erwartet, keine erfolgt
    }

    @Test
    void listUsersSetztTokenUndMapptMitAliasFallback() {
        server.expect(requestTo(BASE_URL + "/users"))
            .andExpect(header("Authorization", "Bearer test-token"))
            .andRespond(withSuccess("""
                [
                  {"id": 3, "alias": "Erika Mustermann", "username": "erika", "enabled": true},
                  {"id": 2, "alias": null, "username": "oliver", "enabled": true}
                ]
                """, MediaType.APPLICATION_JSON));

        var users = client("test-token", 100).listUsers();

        server.verify();
        assertThat(users).containsExactly(
            new UserInfo(3, "Erika Mustermann"),
            new UserInfo(2, "oliver"));
    }

    @Test
    void listUsersApiFehlerFuehrtZuVerstaendlicherException() {
        server.expect(requestTo(BASE_URL + "/users"))
            .andRespond(withServerError());

        assertThatThrownBy(() -> client("geheimes-token", 100).listUsers())
            .isInstanceOf(KimaiException.class)
            .hasMessageContaining("Kimai-API")
            .hasMessageContaining("500")
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("geheimes-token"));
    }

    @Test
    void listUsersOhneTokenBrichtVorDemApiAufrufAb() {
        assertThatThrownBy(() -> client("", 100).listUsers())
            .isInstanceOf(KimaiException.class)
            .hasMessageContaining("KIMAI_API_TOKEN");

        server.verify();
    }

    private static String eintrag(Double rate) {
        return liste(eintragJson("Beratung", rate));
    }

    private static String eintragJson(String description) {
        return eintragJson(description, 262.5);
    }

    private static String eintragJson(String description, Double rate) {
        return """
            {
              "begin": "2026-07-01T09:00:00+0200",
              "end": "2026-07-01T12:30:00+0200",
              "duration": 12600,
              "description": "%s",
              %s
              "break": 0,
              "exported": false,
              "user": {"id": 2, "alias": "Erika", "username": "erika"},
              "activity": {"id": 5, "name": "Entwicklung"},
              "project": {
                "id": 1, "name": "Projekt X", "orderNumber": "A-100",
                "customer": {"name": "ACME GmbH", "number": "K-001"}
              }
            }
            """.formatted(description, rate == null ? "" : "\"rate\": " + rate + ",");
    }

    private static String liste(String... items) {
        return "[" + String.join(",", items) + "]";
    }
}
