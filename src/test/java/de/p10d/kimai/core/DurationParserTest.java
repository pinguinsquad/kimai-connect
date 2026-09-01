package de.p10d.kimai.core;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DurationParserTest {

    @ParameterizedTest
    @CsvSource({
        "3h30m, 210",
        "2h, 120",
        "45m, 45",
        "1:30, 90",
        "0:15, 15",
        "1.5h, 90",
        "'1,25h', 75",
        "' 2H 15M ', 135",
        "10h, 600"
    })
    void liestUeblicheSchreibweisen(String input, long minutes) {
        assertThat(DurationParser.parse(input)).isEqualTo(Duration.ofMinutes(minutes));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "3", "0h", "0m", "1:60", "h30m", "1:5"})
    void ungueltigeAngabenErgebenDeutscheFehlermeldung(String input) {
        assertThatThrownBy(() -> DurationParser.parse(input))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ungültige Dauer")
            .hasMessageContaining("3h30m");
    }

    @org.junit.jupiter.api.Test
    void nullErgibtDeutscheFehlermeldung() {
        assertThatThrownBy(() -> DurationParser.parse(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Ungültige Dauer");
    }
}
