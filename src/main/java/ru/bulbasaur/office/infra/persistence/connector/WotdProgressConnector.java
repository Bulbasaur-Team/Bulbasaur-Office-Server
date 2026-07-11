package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.infra.persistence.entity.PlayerEntity;
import ru.bulbasaur.office.infra.persistence.entity.WotdProgressEntity;
import ru.bulbasaur.office.infra.persistence.repository.WotdProgressJpaRepository;
import ru.bulbasaur.office.usecase.dto.WotdProgressView;
import ru.bulbasaur.office.usecase.dto.WotdProgressUpsert;
import ru.bulbasaur.office.usecase.port.out.WotdProgressRepositoryPort;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WotdProgressConnector implements WotdProgressRepositoryPort {

    private static final String SEPARATOR = "\n";

    private final WotdProgressJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<WotdProgressView> findPlayerProgress(UUID playerId, GameId game, LocalDate day) {
        return repository.findByPlayer_IdAndGameAndDay(playerId, game, day)
                .map(e -> new WotdProgressView(e.isSolved(), e.getAttempts(), split(e.getGuesses())));
    }

    @Override
    @Transactional
    public void upsertPlayerProgress(WotdProgressUpsert upsert) {
        WotdProgressEntity entity = repository
                .findByPlayer_IdAndGameAndDay(upsert.playerId(), upsert.game(), upsert.day())
                .orElseGet(() -> {
                    WotdProgressEntity e = new WotdProgressEntity();
                    e.setId(UUID.randomUUID());
                    PlayerEntity ref = new PlayerEntity();
                    ref.setId(upsert.playerId());
                    e.setPlayer(ref);
                    e.setGame(upsert.game());
                    e.setDay(upsert.day());
                    return e;
                });
        entity.setSolved(upsert.solved());
        entity.setAttempts(upsert.attempts());
        entity.setGuesses(String.join(SEPARATOR, upsert.guesses()));
        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardRow> findTopSolvedPlayers(GameId game, LocalDate day, int limit) {
        Sort sort = Sort.by(Sort.Order.asc("attempts"), Sort.Order.asc("updatedAt"));
        return repository.findSolvedRows(game, day, PageRequest.of(0, limit, sort)).stream()
                .map(row -> new LeaderboardRow(row.getLogin(), row.getPlayerId(), row.getValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findPlayerSolvedAttempts(UUID playerId, GameId game, LocalDate day) {
        return repository.findByPlayer_IdAndGameAndDay(playerId, game, day)
                .filter(WotdProgressEntity::isSolved)
                .map(e -> (long) e.getAttempts());
    }

    @Override
    @Transactional(readOnly = true)
    public long countPlayersWithFewerAttempts(GameId game, LocalDate day, long attempts) {
        return repository.countByGameAndDayAndSolvedTrueAndAttemptsLessThan(game, day, (int) attempts);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasSolvedAny(UUID playerId, GameId game) {
        return repository.existsByPlayer_IdAndGameAndSolvedTrue(playerId, game);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocalDate> solvedDays(UUID playerId, GameId game) {
        return repository.findSolvedDays(playerId, game);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean wasEverFirstToSolve(UUID playerId, GameId game) {
        return repository.wasEverFirstToSolve(playerId, game.name());
    }

    private List<String> split(String guesses) {
        if (guesses == null || guesses.isEmpty()) {
            return List.of();
        }
        return List.of(guesses.split(SEPARATOR));
    }
}
