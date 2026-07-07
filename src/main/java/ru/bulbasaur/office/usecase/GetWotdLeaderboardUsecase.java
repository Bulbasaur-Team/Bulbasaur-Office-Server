package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.usecase.dto.GetWotdLeaderboardQuery;
import ru.bulbasaur.office.usecase.dto.LeaderboardRowView;
import ru.bulbasaur.office.usecase.dto.LeaderboardView;
import ru.bulbasaur.office.usecase.port.out.WotdProgressRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.DayPort;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetWotdLeaderboardUsecase {

    private final WotdProgressRepositoryPort progress;
    private final DayPort day;

    public LeaderboardView execute(GetWotdLeaderboardQuery query) {
        GameId game = query.game();
        UUID playerId = query.playerId();
        LocalDate today = day.today();

        List<LeaderboardRow> rows = progress.findTopSolvedPlayers(game, today, query.limit());
        List<LeaderboardRowView> top = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            LeaderboardRow row = rows.get(i);
            top.add(LeaderboardRowView.builder()
                    .rank(i + 1)
                    .login(row.login())
                    .value(row.value())
                    .currentPlayer(row.playerId().equals(playerId))
                    .build());
        }

        LeaderboardRowView me = null;
        Optional<Long> myValue = progress.findPlayerSolvedAttempts(playerId, game, today);
        if (myValue.isPresent()) {
            long value = myValue.get();
            int rank = (int) progress.countPlayersWithFewerAttempts(game, today, value) + 1;
            me = LeaderboardRowView.builder()
                    .rank(rank)
                    .login(query.login())
                    .value(value)
                    .currentPlayer(true)
                    .build();
        }

        return new LeaderboardView(top, me);
    }
}
