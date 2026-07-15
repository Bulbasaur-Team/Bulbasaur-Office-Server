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
    private final AchievementService achievements;
    private final EventLogService eventLog;

    public LeaderboardView execute(UUID playerId, String login, GameId game, long value, int limit) {
        Integer previousRank = leaderboard.valueOf(playerId, game)
                .map(v -> (int) leaderboard.betterCount(game, v, game.direction()) + 1)
                .orElse(null);

        leaderboard.submit(playerId, game, value, game.direction(), game.accumulate());
        achievements.recheck(playerId);
        LeaderboardView view = getLeaderboard.execute(game, playerId, login, limit);
        // Логируем, только если игрок реально попал в видимый топ.
        if (view.me() != null && view.me().rank() <= view.top().size()) {
            int rank = view.me().rank();
            if (previousRank == null || previousRank != rank) {
                eventLog.leaderboardEntry(login, game, rank, view.me().value());
            } else {
                eventLog.gamePlayed(login, game, value);
            }
        }
        return view;
    }
}
