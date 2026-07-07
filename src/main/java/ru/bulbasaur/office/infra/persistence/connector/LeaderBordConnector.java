package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.Direction;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.infra.persistence.entity.LeaderboardEntryEntity;
import ru.bulbasaur.office.infra.persistence.repository.LeaderboardJpaRepository;
import ru.bulbasaur.office.usecase.port.out.LeaderboardRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LeaderBordConnector implements LeaderboardRepositoryPort {

    private final LeaderboardJpaRepository repository;

    @Override
    @Transactional
    public void submit(UUID playerId, GameId game, long value, Direction direction, boolean accumulate) {
        if (accumulate) {
            repository.upsertAdd(playerId, game.name(), value);
        } else {
            repository.upsertBest(playerId, game.name(), value, direction == Direction.HIGHER_BETTER);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardRow> top(GameId game, Direction direction, int limit) {
        Sort sort = direction == Direction.HIGHER_BETTER
                ? Sort.by(Sort.Order.desc("value"), Sort.Order.asc("updatedAt"))
                : Sort.by(Sort.Order.asc("value"), Sort.Order.asc("updatedAt"));
        return repository.findRows(game, PageRequest.of(0, limit, sort)).stream()
                .map(row -> new LeaderboardRow(row.getLogin(), row.getPlayerId(), row.getValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> valueOf(UUID playerId, GameId game) {
        return repository.findByPlayer_IdAndGame(playerId, game).map(LeaderboardEntryEntity::getValue);
    }

    @Override
    @Transactional(readOnly = true)
    public long betterCount(GameId game, long value, Direction direction) {
        return direction == Direction.HIGHER_BETTER
                ? repository.countByGameAndValueGreaterThan(game, value)
                : repository.countByGameAndValueLessThan(game, value);
    }
}
