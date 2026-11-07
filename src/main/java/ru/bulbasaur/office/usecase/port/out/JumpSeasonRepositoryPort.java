package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.JumpSeason;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.domain.model.SeasonChampion;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JumpSeasonRepositoryPort {

    /** Последний сезон с блокировкой строки: два закрытия не идут параллельно. */
    JumpSeason lockLatest();

    Optional<JumpSeason> findByNumber(int seasonNumber);

    /** Отметить сезон закрытым. Предыдущий должен быть уже завершён по времени. */
    void markAwarded(UUID seasonId, Instant awardedAt);

    /**
     * Открыть следующий сезон. Можно только если предыдущий уже закрыт
     * ({@code awardedAt} заполнен) — второго открытого сезона не бывает.
     */
    JumpSeason openNext(JumpSeason previous);

    /** Лучший результат игрока в сезоне. Худший повтор не затирает рекорд. */
    void upsertScore(UUID seasonId, UUID playerId, long value);

    List<LeaderboardRow> top(UUID seasonId, int limit);

    Optional<Long> valueOf(UUID seasonId, UUID playerId);

    long betterCount(UUID seasonId, long value);

    /** Победитель последнего закрытого сезона, в котором вообще были результаты. */
    Optional<SeasonChampion> previousWinner();
}
