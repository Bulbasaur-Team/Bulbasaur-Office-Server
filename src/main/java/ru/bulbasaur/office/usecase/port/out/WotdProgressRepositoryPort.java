package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.usecase.dto.WotdProgressView;
import ru.bulbasaur.office.usecase.dto.WotdProgressUpsert;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WotdProgressRepositoryPort {

    Optional<WotdProgressView> findPlayerProgress(UUID playerId, GameId game, LocalDate day);

    void upsertPlayerProgress(WotdProgressUpsert upsert);

    List<LeaderboardRow> findTopSolvedPlayers(GameId game, LocalDate day, int limit);

    Optional<Long> findPlayerSolvedAttempts(UUID playerId, GameId game, LocalDate day);

    long countPlayersWithFewerAttempts(GameId game, LocalDate day, long attempts);

    /** Разгадывал ли игрок слово дня в этой игре хотя бы раз. */
    boolean hasSolvedAny(UUID playerId, GameId game);

    /** Дни, в которые игрок разгадал слово дня в этой игре (по возрастанию). */
    List<LocalDate> solvedDays(UUID playerId, GameId game);

    /** Был ли игрок хотя бы раз первым в лидерборде слова дня этой игры. */
    boolean wasEverFirst(UUID playerId, GameId game);
}
