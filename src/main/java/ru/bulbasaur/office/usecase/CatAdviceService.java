package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.LeaderboardRow;
import ru.bulbasaur.office.usecase.dto.WotdProgressView;
import ru.bulbasaur.office.usecase.port.out.AchievementRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.DayPort;
import ru.bulbasaur.office.usecase.port.out.LeaderboardRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.WotdProgressRepositoryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Советы Бульба Кота: только релевантные подсказки по прогрессу и ачивкам игрока.
 */
@Service
@RequiredArgsConstructor
public class CatAdviceService {

    private final AchievementRepositoryPort achievements;
    private final LeaderboardRepositoryPort leaderboard;
    private final WotdProgressRepositoryPort wotd;
    private final DayPort day;

    @Transactional(readOnly = true)
    public String pickAdvice(UUID playerId) {
        List<String> tips = collectTips(playerId);
        if (tips.isEmpty()) {
            return "Пока советов нет — просто помурчи рядом.";
        }
        return tips.get(ThreadLocalRandom.current().nextInt(tips.size()));
    }

    private List<String> collectTips(UUID playerId) {
        Set<Achievement> owned = achievements.findOwned(playerId);
        List<String> tips = new ArrayList<>();

        for (GameId game : GameId.values()) {
            if (leaderboard.valueOf(playerId, game).isEmpty()) {
                continue;
            }
            List<LeaderboardRow> top = leaderboard.top(game, game.direction(), 1);
            if (top.isEmpty()) {
                continue;
            }
            LeaderboardRow first = top.getFirst();
            if (playerId.equals(first.playerId())) {
                continue; // уже первый — «обогнать» неактуально
            }
            tips.add("Смотрю, ты уже играл в " + gameTitle(game)
                    + ". Сейчас на первом месте — " + first.login()
                    + " с результатом " + formatResult(game, first.value())
                    + ". Слабо обогнать?");
        }

        tips.add("Загляни к ноутбукам — там можно поболтать с Claude и Cursor.");
        tips.add("У ноутбуков есть BulbaTalk: можно поучаствовать в стратегическом планировании.");
        tips.add("Сходи к компу в дата-центре — там есть прикол.");

        if (!owned.contains(Achievement.HOCKEY)) {
            tips.add("Позови друга, зайдите в чилл-зону и сыграйте в аэрохоккей — за это дают ачивку.");
        }
        if (!owned.contains(Achievement.JUMPER)) {
            tips.add("У телека в чилл-зоне полно игр. Моя любимая — Bulba Jump, за неё можно получить аж четыре ачивки.");
        }
        if (!wotdSolved(playerId, GameId.BULBA_WORDLE)) {
            tips.add("Ты ещё не отгадал слово дня в Bulba Wordle — обязательно попробуй.");
        }
        if (!wotdSolved(playerId, GameId.BULBA_GUESS)) {
            tips.add("Ты ещё не отгадал слово дня в Bulba Guess — обязательно попробуй.");
        }
        if (!owned.contains(Achievement.VOLLEYBALL)) {
            tips.add("Нет ачивки «Волейболист»? Съезди на вьетнамский пляж через парковку.");
        }
        if (!owned.contains(Achievement.SYSADMIN)) {
            tips.add("Нет ачивки «Сисадмин»? Загляни к принтеру в дата-центре.");
        }
        if (!owned.contains(Achievement.TRADER)) {
            tips.add("Нет ачивки «Трейдер»? Посмотри графики на мониторах в дата-центре.");
        }
        // «Ещё не играл» — по факту записи в лидерборде, не по ачивке
        // (у Colors ачивка «Дизайнер» за 10 очков, а не за сам факт игры).
        if (leaderboard.valueOf(playerId, GameId.BULBA_SURKI).isEmpty()) {
            tips.add("Кажется, ты ещё не пробовал Bulba Surki — автомат стоит в чилл-зоне.");
        }
        if (leaderboard.valueOf(playerId, GameId.BULBA_PACKER).isEmpty()) {
            tips.add("Кажется, ты ещё не играл в Bulba Packer — склад ждёт.");
        }
        if (leaderboard.valueOf(playerId, GameId.BULBA_COLORS).isEmpty()) {
            tips.add("Кажется, ты ещё не играл в Bulba Colors — мольберт в кабинете дизайнера.");
        } else if (!owned.contains(Achievement.DESIGNER)) {
            tips.add("В Bulba Colors набери 10 очков у мольберта — будет ачивка «Дизайнер».");
        }
        if (!owned.contains(Achievement.CHAMELEON)) {
            tips.add("Смени роль через меню — получишь ачивку «Хамелеон».");
        }
        if (!owned.contains(Achievement.LOVER)) {
            tips.add("Жми реакции на панели внизу — за сердечко дают ачивку.");
        }
        if (!owned.contains(Achievement.CAREFUL)) {
            tips.add("Смени пароль через меню — получишь ачивку «Осторожный».");
        }
        if (!owned.contains(Achievement.COFFEEMAN)) {
            tips.add("Возьми кофе на кухне в чилл-зоне — и ачивка твоя.");
        }
        if (!owned.contains(Achievement.SPEAKER)) {
            tips.add("Включи слайды на проекторе — получишь ачивку.");
        }
        if (!owned.contains(Achievement.DISCIPLINE)) {
            tips.add("Отгадывай слово дня пять дней подряд — будет ачивка.");
        }
        if (!owned.contains(Achievement.DEMOCRACY)) {
            tips.add("Сыграй с коллегами в Planning Poker — получишь ачивку.");
        }

        return tips;
    }

    private boolean wotdSolved(UUID playerId, GameId game) {
        return wotd.findPlayerProgress(playerId, game, day.today())
                .map(WotdProgressView::solved)
                .orElse(false);
    }

    private static String gameTitle(GameId game) {
        return switch (game) {
            case BULBA_JUMP -> "Bulba Jump";
            case BULBA_PACKER -> "Bulba Packer";
            case BULBA_RACING -> "Bulba Racing";
            case BULBA_PARKING -> "Bulba Parking";
            case BULBA_TANKS -> "Bulba Tanks";
            case BULBA_COLORS -> "Bulba Colors";
            case BULBA_SURKI -> "Bulba Surki";
            case BULBA_GUESS -> "Bulba Guess";
            case BULBA_WORDLE -> "Bulba Wordle";
        };
    }

    private static String formatResult(GameId game, long value) {
        return switch (game) {
            case BULBA_PARKING -> String.format(Locale.ROOT, "%.1f с", value / 1000.0);
            case BULBA_GUESS, BULBA_WORDLE -> value + " " + plural(value, "слово", "слова", "слов");
            case BULBA_JUMP, BULBA_PACKER, BULBA_RACING, BULBA_TANKS, BULBA_COLORS, BULBA_SURKI
                    -> value + " " + plural(value, "очко", "очка", "очков");
        };
    }

    private static String plural(long n, String one, String few, String many) {
        long abs = Math.abs(n) % 100;
        long mod10 = abs % 10;
        if (abs >= 11 && abs <= 14) {
            return many;
        }
        if (mod10 == 1) {
            return one;
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return few;
        }
        return many;
    }
}
