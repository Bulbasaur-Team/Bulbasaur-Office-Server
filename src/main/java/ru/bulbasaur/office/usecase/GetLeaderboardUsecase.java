package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Direction;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.usecase.dto.LeaderboardRowView;
import ru.bulbasaur.office.usecase.dto.LeaderboardView;
import ru.bulbasaur.office.usecase.port.out.LeaderboardRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Построение лидерборда игры: топ и отдельно строка текущего игрока (подсветка своей). */
@Service
@RequiredArgsConstructor
public class GetLeaderboardUsecase {

    private final LeaderboardRepositoryPort leaderboard;

    public LeaderboardView execute(GameId game, UUID playerId, String login, int limit) {
        Direction direction = game.direction();

        List<LeaderboardRow> rows = leaderboard.top(game, direction, limit);
        List<LeaderboardRowView> top = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardRow row = rows.get(i);
            top.add(new LeaderboardRowView(i + 1, row.login(), row.value(), row.playerId().equals(playerId)));
        }

        LeaderboardRowView me = null;
        Optional<Long> myValue = leaderboard.valueOf(playerId, game);
        if (myValue.isPresent()) {
            long value = myValue.get();
            int rank = (int) leaderboard.betterCount(game, value, direction) + 1;
            me = new LeaderboardRowView(rank, login, value, true);
        }

        return new LeaderboardView(top, me);
    }
}
