package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.usecase.port.out.AchievementRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.LeaderboardRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.WotdProgressRepositoryPort;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Пересчитать выводимые из состояния ачивки и выдать недостающие.
 * Ошибка в проверке одной ачивки не ломает основное действие.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecheckAchievementsUsecase {

    private static final long JUMPER_SCORE = 10_000;
    private static final long GREAT_JUMPER_SCORE = 100_000;
    private static final long LEGEND_JUMPER_SCORE = 200_000;
    // Лидерборд показывает время как (мс/1000).toFixed(1): как «4.5 с» отображается
    // всё вплоть до 4550 мс включительно (4.55 в double ≈ 4.5499…, округляется к 4.5),
    // а 4551 мс — уже «4.6 с».
    private static final long TRUCKER_PRO_MILLIS = 4_550;
    private static final long GUARD_WORDS = 10;
    private static final int DISCIPLINE_DAYS = 5;
    private static final long DESIGNER_SCORE = 10;
    private static final long TETRACHROMAT_SCORE = 15;
    private static final long SURKI_TAMER_SCORE = 20;

    private static final Set<Achievement> EVENT_DRIVEN = EnumSet.of(
            Achievement.VOLLEYBALL, Achievement.TENNIS, Achievement.LOVER, Achievement.DAY_CHAMPION,
            Achievement.CHEATER, Achievement.CROUPIER, Achievement.DEMOCRACY, Achievement.COFFEEMAN,
            Achievement.HOCKEY, Achievement.OVECHKIN, Achievement.SPEAKER, Achievement.MEOW,
            Achievement.TRADER, Achievement.SYSADMIN, Achievement.CAREFUL, Achievement.SOCIAL);

    private final AchievementRepositoryPort achievements;
    private final LeaderboardRepositoryPort leaderboard;
    private final WotdProgressRepositoryPort wotd;
    private final PlayerRepositoryPort players;
    private final GrantAchievementUsecase grant;

    public void execute(UUID playerId) {
        Set<Achievement> owned = achievements.findOwned(playerId);
        for (Achievement achievement : Achievement.values()) {
            if (owned.contains(achievement) || EVENT_DRIVEN.contains(achievement)) {
                continue;
            }
            try {
                if (isMet(playerId, achievement)) {
                    grant.execute(playerId, achievement);
                }
            } catch (Exception e) {
                log.error("не удалось проверить ачивку {} игрока {}", achievement, playerId, e);
            }
        }
    }

    /** Страховочный прогон по всем игрокам. */
    public void executeAll() {
        for (UUID playerId : players.findAllIds()) {
            execute(playerId);
        }
    }

    private boolean isMet(UUID playerId, Achievement achievement) {
        return switch (achievement) {
            case BULBAZAVR -> true;
            case VOLLEYBALL, TENNIS, LOVER, DAY_CHAMPION, CHEATER, CROUPIER, DEMOCRACY,
                 COFFEEMAN, HOCKEY, OVECHKIN, SPEAKER, MEOW, TRADER, SYSADMIN, CAREFUL, SOCIAL
                    -> false;
            case JUMPER -> hasEntry(playerId, GameId.BULBA_JUMP);
            case JUMPER_10K -> reached(playerId, GameId.BULBA_JUMP, JUMPER_SCORE);
            case JUMPER_100K -> reached(playerId, GameId.BULBA_JUMP, GREAT_JUMPER_SCORE);
            case JUMPER_200K -> reached(playerId, GameId.BULBA_JUMP, LEGEND_JUMPER_SCORE);
            case SHOPAHOLIC -> hasEntry(playerId, GameId.BULBA_PACKER);
            case TRUCKER -> hasEntry(playerId, GameId.BULBA_PARKING);
            case TRUCKER_PRO -> atMost(playerId, GameId.BULBA_PARKING, TRUCKER_PRO_MILLIS);
            case PSYCHIC -> hasEntry(playerId, GameId.BULBA_GUESS) || wotd.hasSolvedAny(playerId, GameId.BULBA_GUESS);
            case DECODER -> hasEntry(playerId, GameId.BULBA_WORDLE) || wotd.hasSolvedAny(playerId, GameId.BULBA_WORDLE);
            case GUARD -> reached(playerId, GameId.BULBA_WORDLE, GUARD_WORDS);
            case LIGHTNING -> wotd.wasEverFirstToSolve(playerId, GameId.BULBA_GUESS)
                    || wotd.wasEverFirstToSolve(playerId, GameId.BULBA_WORDLE);
            case DISCIPLINE -> hasConsecutiveSolvedDays(playerId, GameId.BULBA_WORDLE, DISCIPLINE_DAYS);
            case CHAMPION -> isFirstInAnyLeaderboard(playerId);
            case DESIGNER -> reached(playerId, GameId.BULBA_COLORS, DESIGNER_SCORE);
            case TETRACHROMAT -> reached(playerId, GameId.BULBA_COLORS, TETRACHROMAT_SCORE);
            case HAMMER -> hasEntry(playerId, GameId.BULBA_SURKI);
            case DADATA -> reached(playerId, GameId.BULBA_SURKI, SURKI_TAMER_SCORE);
        };
    }

    private boolean hasEntry(UUID playerId, GameId game) {
        return leaderboard.valueOf(playerId, game).isPresent();
    }

    private boolean reached(UUID playerId, GameId game, long threshold) {
        return leaderboard.valueOf(playerId, game).orElse(Long.MIN_VALUE) >= threshold;
    }

    private boolean atMost(UUID playerId, GameId game, long threshold) {
        return leaderboard.valueOf(playerId, game).map(v -> v <= threshold).orElse(false);
    }

    private boolean isFirstInAnyLeaderboard(UUID playerId) {
        for (GameId game : GameId.values()) {
            List<LeaderboardRow> top = leaderboard.top(game, game.direction(), 1);
            if (!top.isEmpty() && top.getFirst().playerId().equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasConsecutiveSolvedDays(UUID playerId, GameId game, int required) {
        List<LocalDate> days = wotd.solvedDays(playerId, game);
        int run = 0;
        LocalDate previous = null;
        for (LocalDate day : days) {
            run = (previous != null && day.equals(previous.plusDays(1))) ? run + 1 : 1;
            if (run >= required) {
                return true;
            }
            previous = day;
        }
        return false;
    }
}
