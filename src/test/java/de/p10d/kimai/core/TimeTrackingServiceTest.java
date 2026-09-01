package de.p10d.kimai.core;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeTrackingServiceTest {

    private static final LocalDateTime BEGIN = LocalDateTime.of(2026, 8, 28, 9, 0);

    private static final ProjectInfo ACME_WEBSITE = new ProjectInfo(1, "Website", 10L, "ACME GmbH");
    private static final ProjectInfo ACME_APP = new ProjectInfo(2, "App", 10L, "ACME GmbH");
    private static final ProjectInfo MUELLER_WEBSITE = new ProjectInfo(3, "Website", 11L, "Müller AG");
    private static final ProjectInfo MUELLER_RELAUNCH = new ProjectInfo(4, "Website Relaunch", 11L, "Müller AG");

    private static final ActivityInfo GLOBAL_DEV = new ActivityInfo(50, "Entwicklung", null);
    private static final ActivityInfo GLOBAL_MEETING = new ActivityInfo(51, "Besprechung", null);
    private static final ActivityInfo APP_DEV = new ActivityInfo(60, "Entwicklung", 2L);
    private static final ActivityInfo APP_TEST = new ActivityInfo(61, "Test", 2L);

    private final RecordingWriter writer = new RecordingWriter();
    private Long requestedProjectId;
    private int activityCalls;

    private final TimeTrackingService service = new TimeTrackingService(
        () -> List.of(ACME_WEBSITE, ACME_APP, MUELLER_WEBSITE, MUELLER_RELAUNCH),
        projectId -> {
            activityCalls++;
            requestedProjectId = projectId;
            if (projectId == null) {
                return List.of(GLOBAL_DEV, GLOBAL_MEETING, APP_DEV, APP_TEST);
            }
            return projectId == 2L
                ? List.of(GLOBAL_DEV, GLOBAL_MEETING, APP_DEV, APP_TEST)
                : List.of(GLOBAL_DEV, GLOBAL_MEETING);
        },
        writer);

    @Test
    void projektPerIdOderEindeutigemNamen() {
        assertThat(service.resolveProject("2")).isEqualTo(ACME_APP);
        assertThat(service.resolveProject("app")).isEqualTo(ACME_APP);
        assertThat(service.resolveProject(" Website Relaunch ")).isEqualTo(MUELLER_RELAUNCH);
    }

    @Test
    void namensbestandteilReichtWennEindeutig() {
        assertThat(service.resolveProject("relaunch")).isEqualTo(MUELLER_RELAUNCH);
    }

    @Test
    void mehrdeutigerNameNenntDieKandidaten() {
        // exakt "Website" gibt es zweimal – der Bestandteil-Treffer "Website Relaunch" zählt dann nicht
        assertThatThrownBy(() -> service.resolveProject("Website"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("nicht eindeutig")
            .hasMessageContaining("1  ACME GmbH / Website")
            .hasMessageContaining("3  Müller AG / Website")
            .satisfies(e -> assertThat(e.getMessage()).doesNotContain("Relaunch"))
            .hasMessageContaining("ID angeben");
    }

    @Test
    void unbekanntesProjektListetDieVerfuegbaren() {
        assertThatThrownBy(() -> service.resolveProject("Shop"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Projekt „Shop“ nicht gefunden")
            .hasMessageContaining("Verfügbare Projekte")
            .hasMessageContaining("2  ACME GmbH / App")
            .hasMessageContaining("4  Müller AG / Website Relaunch");
    }

    @Test
    void unbekannteIdMeldetDasAlsSolche() {
        assertThatThrownBy(() -> service.resolveProject("99"))
            .hasMessageContaining("Projekt mit ID 99 nicht gefunden");
    }

    @Test
    void taetigkeitWirdImProjektGesuchtUndProjekteigeneSchlaegtGlobale() {
        assertThat(service.resolveActivity("Entwicklung", ACME_APP)).isEqualTo(APP_DEV);
        assertThat(requestedProjectId).isEqualTo(2L);
        assertThat(service.resolveActivity("Entwicklung", ACME_WEBSITE)).isEqualTo(GLOBAL_DEV);
        assertThat(service.resolveActivity("61", ACME_APP)).isEqualTo(APP_TEST);
    }

    @Test
    void unbekannteTaetigkeitListetDieDesProjektsMitGlobalKennzeichen() {
        assertThatThrownBy(() -> service.resolveActivity("Design", ACME_APP))
            .hasMessageContaining("Tätigkeit „Design“ nicht gefunden")
            .hasMessageContaining("50  Entwicklung (global)")
            .hasMessageContaining("61  Test");
    }

    @Test
    void recordLoestAufUndSchreibt() {
        var draft = new TimesheetDraft(BEGIN, BEGIN.plusMinutes(90), "App", "Test",
            "Regressionstests", 3L, List.of("qa"), false);

        var created = service.record(draft);

        assertThat(writer.last.project()).isEqualTo(ACME_APP);
        assertThat(writer.last.activity()).isEqualTo(APP_TEST);
        assertThat(writer.last.begin()).isEqualTo(BEGIN);
        assertThat(writer.last.end()).isEqualTo(BEGIN.plusMinutes(90));
        assertThat(writer.last.description()).isEqualTo("Regressionstests");
        assertThat(writer.last.userId()).isEqualTo(3L);
        assertThat(writer.last.tags()).containsExactly("qa");
        assertThat(writer.last.billable()).isFalse();
        assertThat(created.id()).isEqualTo(4711);
    }

    @Test
    void recordSchreibtNichtsWennDieAufloesungScheitert() {
        var draft = new TimesheetDraft(BEGIN, BEGIN.plusMinutes(90), "Website", "Test", null, null, null, true);

        assertThatThrownBy(() -> service.record(draft)).hasMessageContaining("nicht eindeutig");
        assertThat(writer.last).isNull();
        assertThat(activityCalls).isZero();
    }

    @Test
    void projektlisteNachKundeAlsNameOderId() {
        assertThat(service.listProjects(null)).hasSize(4);
        assertThat(service.listProjects("müller")).containsExactly(MUELLER_WEBSITE, MUELLER_RELAUNCH);
        assertThat(service.listProjects("10")).containsExactly(ACME_WEBSITE, ACME_APP);
        assertThat(service.listProjects("Niemand")).isEmpty();
    }

    @Test
    void taetigkeitslisteOhneProjektIstUngefiltertMitProjektAufgeloest() {
        assertThat(service.listActivities(null)).hasSize(4);
        assertThat(requestedProjectId).isNull();

        assertThat(service.listActivities("Website Relaunch")).containsExactly(GLOBAL_DEV, GLOBAL_MEETING);
        assertThat(requestedProjectId).isEqualTo(4L);
    }

    private static class RecordingWriter implements TimesheetWriter {
        private NewTimesheet last;

        @Override
        public CreatedTimesheet create(NewTimesheet entry) {
            last = entry;
            return new CreatedTimesheet(4711, entry.begin(), entry.end(), entry.durationSeconds(),
                entry.description(), entry.project(), entry.activity(), entry.userId(), entry.tags(),
                entry.billable());
        }
    }
}
