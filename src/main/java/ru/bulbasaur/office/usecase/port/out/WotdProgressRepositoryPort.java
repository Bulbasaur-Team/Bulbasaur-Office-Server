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
}
