package com.cheatbreaker.client.module.type;

import java.time.Clock;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ClockText {
    private static final DateTimeFormatter TWENTY_FOUR_HOUR_WITH_SECONDS =
            DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TWENTY_FOUR_HOUR =
            DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TWELVE_HOUR_WITH_SECONDS =
            DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH);
    private static final DateTimeFormatter TWELVE_HOUR =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final Clock clock;
    private long cachedSecond;
    private boolean cachedTwentyFourHour, cachedSeconds;
    private String cachedText;

    public ClockText(Clock clock) {
        this.clock = clock;
    }

    public String format(boolean twentyFourHour, boolean showSeconds) {
        long second = Math.floorDiv(this.clock.millis(), 1000L);
        if (cachedText != null && second == cachedSecond && cachedTwentyFourHour == twentyFourHour
                && cachedSeconds == showSeconds) return cachedText;
        DateTimeFormatter formatter;
        if (twentyFourHour) {
            formatter = showSeconds ? TWENTY_FOUR_HOUR_WITH_SECONDS : TWENTY_FOUR_HOUR;
        } else {
            formatter = showSeconds ? TWELVE_HOUR_WITH_SECONDS : TWELVE_HOUR;
        }
        cachedSecond = second; cachedTwentyFourHour = twentyFourHour; cachedSeconds = showSeconds;
        // Format the same instant used for the cache key, including second-boundary races.
        return cachedText = java.time.Instant.ofEpochSecond(second).atZone(this.clock.getZone()).toLocalTime().format(formatter);
    }
}
