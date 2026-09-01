package de.p10d.kimai.core;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewTimesheetTest {

    private static final LocalDateTime BEGIN = LocalDateTime.of(2026, 8, 28, 9, 0);
    private static final ProjectInfo PROJECT = new ProjectInfo(1, "Projekt X", 7L, "ACME GmbH");
    private static final ActivityInfo ACTIVITY = new ActivityInfo(5, "Entwicklung", 1L);

    @Test
    void berechnetDieDauerAusBeginnUndEnde() {
        var entry = new NewTimesheet(BEGIN, BEGIN.plusMinutes(210), PROJECT, ACTIVITY, null, null, null, true);

        assertThat(entry.durationSeconds()).isEqualTo(12600);
        assertThat(entry.tags()).isEmpty();
    }

    @Test
    void endeVorOderGleichBeginnWirdAbgelehnt() {
        assertThatThrownBy(() -> new NewTimesheet(BEGIN, BEGIN, PROJECT, ACTIVITY, null, null, null, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ende muss nach dem Beginn");
        assertThatThrownBy(() -> new NewTimesheet(BEGIN, BEGIN.minusMinutes(1), PROJECT, ACTIVITY, null, null, null, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ende muss nach dem Beginn");
    }

    @Test
    void fehlendeZeitenWerdenAbgelehnt() {
        assertThatThrownBy(() -> new NewTimesheet(null, BEGIN, PROJECT, ACTIVITY, null, null, null, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Beginn und Ende");
    }

    @Test
    void projektUndTaetigkeitSindPflicht() {
        assertThatThrownBy(() -> new NewTimesheet(BEGIN, BEGIN.plusHours(1), null, ACTIVITY, null, null, null, true))
            .hasMessageContaining("Projekt");
        assertThatThrownBy(() -> new NewTimesheet(BEGIN, BEGIN.plusHours(1), PROJECT, null, null, null, null, true))
            .hasMessageContaining("Tätigkeit");
    }

    @Test
    void tagsWerdenKopiert() {
        var tags = new ArrayList<>(List.of("a", "b"));
        var entry = new NewTimesheet(BEGIN, BEGIN.plusHours(1), PROJECT, ACTIVITY, null, null, tags, true);
        tags.add("c");

        assertThat(entry.tags()).containsExactly("a", "b");
    }

    @Test
    void draftPrueftZeitenUndReferenzen() {
        assertThatThrownBy(() -> new TimesheetDraft(BEGIN, BEGIN, "Projekt X", "Entwicklung", null, null, null, true))
            .hasMessageContaining("Ende muss nach dem Beginn");
        assertThatThrownBy(() -> new TimesheetDraft(BEGIN, BEGIN.plusHours(1), " ", "Entwicklung", null, null, null, true))
            .hasMessageContaining("Projekt");
        assertThatThrownBy(() -> new TimesheetDraft(BEGIN, BEGIN.plusHours(1), "Projekt X", null, null, null, null, true))
            .hasMessageContaining("Tätigkeit");
    }
}
