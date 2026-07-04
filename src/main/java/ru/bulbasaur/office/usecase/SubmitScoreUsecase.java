package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.usecase.dto.LeaderboardView;
import ru.bulbasaur.office.usecase.port.out.LeaderboardRepositoryPort;

import java.util.UUID;

/** Отправка результата (сохраняется лучший) и возврат обновлённого лидерборда. */
@Service
@RequiredArgsConstructor
public class SubmitScoreUsecase {

    private final LeaderboardRepositoryPort leaderboard;
    private final GetLeaderboardUsecase getLeaderboard;

    public LeaderboardView execute(UUID playerId, String login, GameId game, long value, int limit) {
        leaderboard.submit(playerId, game, value, game.direction());
        return getLeaderboard.execute(game, playerId, login, limit);
    }
}
