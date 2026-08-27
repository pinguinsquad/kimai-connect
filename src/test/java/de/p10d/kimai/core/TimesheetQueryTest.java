package de.p10d.kimai.core;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimesheetQueryTest {

    private static final LocalDate START = LocalDate.of(2026, 7, 1);
    private static final LocalDate END = LocalDate.of(2026, 7, 31);

    @Test
    void gueltigeQueryMitAllenFiltern() {
        var query = new TimesheetQuery(START, END, 2L, false);

        assertThat(query.start()).isEqualTo(START);
        assertThat(query.end()).isEqualTo(END);
        assertThat(query.userId()).isEqualTo(2L);
        assertThat(query.billableOnly()).isFalse();
    }

    @Test
    void standardIstNurAbrechenbarUndAlleUser() {
        var query = new TimesheetQuery(START, END);

        assertThat(query.billableOnly()).isTrue();
        assertThat(query.userId()).isNull();
    }

    @Test
    void fehlenderZeitraumWirdAbgelehnt() {
        assertThatThrownBy(() -> new TimesheetQuery(null, null, null, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Start- und Enddatum");
    }

    @Test
    void nurStartdatumWirdAbgelehnt() {
        assertThatThrownBy(() -> new TimesheetQuery(START, null, null, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Start- und Enddatum");
    }

    @Test
    void nurEnddatumWirdAbgelehnt() {
        assertThatThrownBy(() -> new TimesheetQuery(null, END, null, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Start- und Enddatum");
    }

    @Test
    void endeVorStartWirdAbgelehnt() {
        assertThatThrownBy(() -> new TimesheetQuery(END, START, null, true))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Enddatum")
            .hasMessageContaining("Startdatum");
    }
}
