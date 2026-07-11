package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.usecase.dto.AchievementView;
import ru.bulbasaur.office.usecase.exception.PlayerNotFoundException;
import ru.bulbasaur.office.usecase.port.out.AchievementNotifierPort;
import ru.bulbasaur.office.usecase.port.out.AchievementRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.DayPort;
import ru.bulbasaur.office.usecase.port.out.LeaderboardRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.WotdProgressRepositoryPort;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Выдача ачивок. Условия делятся на два вида: выводимые из сохранённого состояния
 * (лидерборды, слово дня, факт регистрации) — их пересчитывает {@link #recheck} и cron;
 * и событийные (удар по мячу) — они выдаются точечно через {@link #grant}. При каждой
 * свежей выдаче игроку уходит попап (если он онлайн).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AchievementService {

    private static final long JUMPER_SCORE = 10_000;
    private static final long GREAT_JUMPER_SCORE = 100_000;
    private static final long LEGEND_JUMPER_SCORE = 200_000;
    // Лидерборд показывает время как (мс/1000).toFixed(1): как «4.5 с» отображается
    // всё вплоть до 4550 мс включительно (4.55 в double ≈ 4.5499…, округляется к 4.5),
    // а 4551 мс — уже «4.6 с».
    private static final long TRUCKER_PRO_MILLIS = 4_550;
    private static final long GUARD_WORDS = 10;
    private static final int DISCIPLINE_DAYS = 5;

    // Событийные ачивки: их условие нельзя вывести из сохранённого состояния, поэтому
    // общий пересчёт их не трогает — они выдаются точечно (удар по мячу, эмодзи, итог дня).
    private static final Set<Achievement> EVENT_DRIVEN = EnumSet.of(
            Achievement.VOLLEYBALL, Achievement.TENNIS, Achievement.LOVER, Achievement.DAY_CHAMPION);

    private final AchievementRepositoryPort achievements;
    private final AchievementNotifierPort notifier;
    private final LeaderboardRepositoryPort leaderboard;
    private final WotdProgressRepositoryPort wotd;
    private final PlayerRepositoryPort players;
    private final DayPort day;

    /**
     * Список ачивок игрока для окна ачивок: признак «получена» и редкость (процент
     * игроков, владеющих ачивкой). Отсортирован от самых распространённых к самым редким.
     */
    public List<AchievementView> list(UUID playerId) {
        Set<Achievement> owned = achievements.findOwned(playerId);
        Map<Achievement, Long> owners = achievements.countOwners();
        long totalPlayers = Math.max(1, players.countPlayers());
        return Arrays.stream(Achievement.values())
                .map(a -> new AchievementView(
                        a.code(), a.title(), a.description(), a.image(), owned.contains(a),
                        owners.getOrDefault(a, 0L) * 100.0 / totalPlayers))
                .sorted(Comparator.comparingDouble(AchievementView::percent).reversed())
                .toList();
    }

    /** То же для чужого игрока по логину — для экрана сообщества. */
    public List<AchievementView> listByLogin(String login) {
        UUID playerId = players.findByLogin(login)
                .orElseThrow(() -> new PlayerNotFoundException(login))
                .id();
        return list(playerId);
    }

    /**
     * Пересчитать выводимые из состояния ачивки игрока и выдать недостающие. Пересчёт —
     * побочный эффект основного действия (регистрация, отправка результата), поэтому
     * ошибка в проверке отдельной ачивки не должна ломать это действие: логируем и идём дальше.
     */
    public void recheck(UUID playerId) {
        Set<Achievement> owned = achievements.findOwned(playerId);
        for (Achievement achievement : Achievement.values()) {
            if (owned.contains(achievement) || !isDerivable(achievement)) {
                continue;
            }
            try {
                if (isMet(playerId, achievement)) {
                    grant(playerId, achievement);
                }
            } catch (Exception e) {
                log.error("не удалось проверить ачивку {} игрока {}", achievement, playerId, e);
            }
        }
    }

    /** Пересчитать ачивки у всех игроков (страховочный прогон по расписанию). */
    public void recheckAll() {
        for (UUID playerId : players.findAllIds()) {
            recheck(playerId);
        }
    }

    /**
     * Выдать «Чемпион дня» победителям слова дня за вчера. Зовётся при смене суток:
     * рейтинг прошедшего дня уже финализирован, первый в Bulba Guess и Bulba Wordle
     * получает ачивку.
     */
    public void awardDayChampions() {
        LocalDate yesterday = day.today().minusDays(1);
        for (GameId game : List.of(GameId.BULBA_GUESS, GameId.BULBA_WORDLE)) {
            List<LeaderboardRow> top = wotd.findTopSolvedPlayers(game, yesterday, 1);
            if (!top.isEmpty()) {
                grant(top.get(0).playerId(), Achievement.DAY_CHAMPION);
            }
        }
    }

    /** Точечная выдача ачивки (для событийных условий вроде удара по мячу). */
    public void grant(UUID playerId, Achievement achievement) {
        if (achievements.grant(playerId, achievement)) {
            notifier.notifyGranted(playerId, achievement);
        }
    }

    private boolean isDerivable(Achievement achievement) {
        return !EVENT_DRIVEN.contains(achievement);
    }

    private boolean isMet(UUID playerId, Achievement achievement) {
        return switch (achievement) {
            case BULBAZAVR -> true; // сам факт существования игрока
            case VOLLEYBALL, TENNIS, LOVER, DAY_CHAMPION -> false; // только через событие
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
        // Обычные лидерборды (лидерборды слова дня сюда не входят).
        for (GameId game : GameId.values()) {
            Optional<Long> value = leaderboard.valueOf(playerId, game);
            if (value.isPresent() && leaderboard.betterCount(game, value.get(), game.direction()) == 0) {
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
