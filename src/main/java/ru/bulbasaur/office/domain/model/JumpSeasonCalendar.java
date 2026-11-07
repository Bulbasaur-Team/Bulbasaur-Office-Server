package ru.bulbasaur.office.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Календарь сезонов Bulba Jump. Сезоны идут стык в стык от якоря, без зазора и без
 * пересечения: конец одного — это начало следующего. Интервал сезона полуоткрытый
 * {@code [start, end)}.
 *
 * <p>Даты первого сезона продублированы в Liquibase ({@code 042-jump-seasons}).
 */
public final class JumpSeasonCalendar {

    public static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    public static final Duration LENGTH = Duration.ofDays(28);
    public static final List<Long> PRIZES = List.of(10_000L, 5_000L, 3_000L);

    /** Рекордсмен до сезонов: результат нигде не хранился, показываем как прошлого победителя. */
    public static final String LEGACY_WINNER_LOGIN = "Sodnom";
    public static final long LEGACY_WINNER_SCORE = 729_467L;

    private static final LocalDateTime FIRST_START_LOCAL = LocalDateTime.of(2026, 10, 30, 17, 0);
    private static final Instant FIRST_START = FIRST_START_LOCAL.atZone(ZONE).toInstant();

    private JumpSeasonCalendar() {
    }

    public static Instant startOf(int number) {
        if (number < 1) {
            throw new IllegalArgumentException("Номер сезона начинается с 1");
        }
        return FIRST_START.plus(LENGTH.multipliedBy(number - 1L));
    }

    public static Instant endOf(int number) {
        return startOf(number).plus(LENGTH);
    }

    /**
     * Номер сезона, в который попадает момент. {@code 0}, если сезон ещё не начался.
     * Ровно в момент конца сезона начинается следующий.
     */
    public static int numberAt(Instant now) {
        if (now.isBefore(FIRST_START)) {
            return 0;
        }
        long index = Duration.between(FIRST_START, now).dividedBy(LENGTH);
        return (int) index + 1;
    }
}
