package ru.bulbasaur.office.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JumpSeasonCalendarTest {

    private static final ZoneId MOSCOW = ZoneId.of("Europe/Moscow");

    @Test
    void firstSeason_starts30Oct2026at17msk_andLastsFourWeeks() {
        Instant start = JumpSeasonCalendar.startOf(1);
        Instant end = JumpSeasonCalendar.endOf(1);

        assertEquals(LocalDateTime.of(2026, 10, 30, 17, 0).atZone(MOSCOW).toInstant(), start);
        assertEquals(LocalDateTime.of(2026, 11, 27, 17, 0).atZone(MOSCOW).toInstant(), end);
        assertEquals(Duration.ofDays(28), Duration.between(start, end));
        assertEquals(end, JumpSeasonCalendar.startOf(2));
    }

    @Test
    void numberAt_isHalfOpen_soTheEndBelongsToTheNextSeason() {
        Instant start = JumpSeasonCalendar.startOf(1);
        Instant end = JumpSeasonCalendar.endOf(1);

        assertEquals(0, JumpSeasonCalendar.numberAt(start.minusMillis(1)));
        assertEquals(1, JumpSeasonCalendar.numberAt(start));
        assertEquals(1, JumpSeasonCalendar.numberAt(end.minusMillis(1)));
        assertEquals(2, JumpSeasonCalendar.numberAt(end));
        assertEquals(2, JumpSeasonCalendar.numberAt(end.plus(Duration.ofDays(7))));
        assertEquals(3, JumpSeasonCalendar.numberAt(JumpSeasonCalendar.endOf(2)));
    }

    @Test
    void seasonContains_matchesTheCalendarWindow() {
        JumpSeason season = new JumpSeason(null, 1, JumpSeasonCalendar.startOf(1), JumpSeasonCalendar.endOf(1), null);
        assertFalse(season.contains(season.startsAt().minusMillis(1)));
        assertTrue(season.contains(season.startsAt()));
        assertTrue(season.contains(season.endsAt().minusMillis(1)));
        assertFalse(season.contains(season.endsAt()));
        assertTrue(season.finishedAt(season.endsAt()));
        assertFalse(season.finishedAt(season.endsAt().minusMillis(1)));
    }
}
