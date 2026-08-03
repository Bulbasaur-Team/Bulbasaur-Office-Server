package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.usecase.port.out.DayPort;
import ru.bulbasaur.office.usecase.port.out.WotdProgressRepositoryPort;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Выдать «Чемпион дня» победителям слова дня за вчера и +200 BC.
 */
@Service
@RequiredArgsConstructor
public class AwardDayChampionsUsecase {

    private final DayPort day;
    private final WotdProgressRepositoryPort wotd;
    private final GrantAchievementUsecase grant;
    private final CreditBulbaCoinsUsecase credit;

    public void execute() {
        LocalDate yesterday = day.today().minusDays(1);
        for (GameId game : List.of(GameId.BULBA_GUESS, GameId.BULBA_WORDLE)) {
            List<LeaderboardRow> top = wotd.findTopSolvedPlayers(game, yesterday, 1);
            if (!top.isEmpty()) {
                UUID winnerId = top.get(0).playerId();
                grant.execute(winnerId, Achievement.DAY_CHAMPION);
                String gameTitle = game == GameId.BULBA_GUESS ? "Bulba Guess" : "Bulba Wordle";
                credit.execute(winnerId, 200, BulbaCoinKind.WOTD_CHAMPION,
                        "wotd_champ:" + game.code() + ":" + yesterday,
                        "Лучший результат слова дня («" + gameTitle + "»)");
            }
        }
    }
}
