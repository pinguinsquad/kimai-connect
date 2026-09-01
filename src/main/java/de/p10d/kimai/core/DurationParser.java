package de.p10d.kimai.core;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Liest Dauerangaben, wie Menschen sie tippen: {@code 3h30m}, {@code 2h},
 * {@code 45m}, {@code 1:30} oder {@code 1,5h}/{@code 1.5h}.
 */
public final class DurationParser {

    private static final Pattern HOURS_MINUTES = Pattern.compile("(?:(\\d+)h)?\\s*(?:(\\d+)m)?");
    private static final Pattern CLOCK = Pattern.compile("(\\d+):([0-5]\\d)");
    private static final Pattern DECIMAL_HOURS = Pattern.compile("(\\d+)[.,](\\d+)h");

    private DurationParser() {
    }

    public static Duration parse(String value) {
        String text = value == null ? "" : value.strip().toLowerCase();
        Duration duration = tryParse(text);
        if (duration == null || duration.isZero()) {
            throw new IllegalArgumentException(
                "Ungültige Dauer „" + value + "“ — erwartet wird z. B. 3h30m, 2h, 45m oder 1:30.");
        }
        return duration;
    }

    private static Duration tryParse(String text) {
        if (text.isEmpty()) {
            return null;
        }
        Matcher clock = CLOCK.matcher(text);
        if (clock.matches()) {
            return Duration.ofHours(Long.parseLong(clock.group(1)))
                .plusMinutes(Long.parseLong(clock.group(2)));
        }
        Matcher decimal = DECIMAL_HOURS.matcher(text);
        if (decimal.matches()) {
            double hours = Double.parseDouble(decimal.group(1) + "." + decimal.group(2));
            return Duration.ofMinutes(Math.round(hours * 60));
        }
        Matcher hoursMinutes = HOURS_MINUTES.matcher(text);
        if (hoursMinutes.matches() && (hoursMinutes.group(1) != null || hoursMinutes.group(2) != null)) {
            long hours = hoursMinutes.group(1) == null ? 0 : Long.parseLong(hoursMinutes.group(1));
            long minutes = hoursMinutes.group(2) == null ? 0 : Long.parseLong(hoursMinutes.group(2));
            return Duration.ofHours(hours).plusMinutes(minutes);
        }
        return null;
    }
}
