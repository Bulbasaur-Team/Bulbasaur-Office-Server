package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.Direction;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.LeaderboardRow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LeaderboardRepositoryPort {

    /**
     * Сохранить результат (одна строка на игрока/игру). accumulate=false — оставить лучший
     * по правилу direction; accumulate=true — прибавить value к текущему значению.
     */
    void submit(UUID playerId, GameId game, long value, Direction direction, boolean accumulate);

    /** Топ результатов игры, отсортированный по direction, не более limit строк. */
    List<LeaderboardRow> top(GameId game, Direction direction, int limit);

    /** Текущее (лучшее) значение игрока в игре, если есть. */
    Optional<Long> valueOf(UUID playerId, GameId game);

    /** Сколько результатов строго лучше данного значения — для вычисления ранга. */
    long betterCount(GameId game, long value, Direction direction);
}
