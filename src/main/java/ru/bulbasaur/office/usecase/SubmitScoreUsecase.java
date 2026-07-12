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
        leaderboard.submit(playerId, game, value, game.direction(), game.accumulate());
        achievements.recheck(playerId);
        LeaderboardView view = getLeaderboard.execute(game, playerId, login, limit);
        // Логируем, только если игрок реально попал в видимый топ.
        if (view.me() != null && view.me().rank() <= view.top().size()) {
            eventLog.leaderboardEntry(login, game, view.me().rank(), view.me().value());
        }
        return view;
    }
}
