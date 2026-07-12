package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.domain.model.GameId;
import ru.bulbasaur.office.domain.model.Role;
import ru.bulbasaur.office.usecase.port.out.EventLogPort;

import java.util.Locale;

/**
 * Запись событий Бульба Офиса в журнал в виде, похожем на логи микросервиса.
 * Текст события формируется здесь, оболочку строки (время, уровень, логгер)
 * добавляет слой представления при выдаче.
 */
@Service
@RequiredArgsConstructor
public class EventLogService {

    private static final String INFO = "INFO";

    private final EventLogPort log;

    public void playerRegistered(String login) {
        log.append(INFO, "registration-service", "Зарегистрировался новый Бульбазавр: " + login);
    }

    public void achievementGranted(String login, Achievement achievement) {
        log.append(INFO, "achievement-service",
                "Бульбазавр " + login + " получил ачивку «" + achievement.title() + "»");
    }

    public void wotdSolved(String login, GameId game, int attempts) {
        log.append(INFO, "wotd-service",
                "Бульбазавр " + login + " отгадал слово дня в " + gameTitle(game)
                        + " за " + attempts + " " + attemptsWord(attempts));
    }

    public void roleChanged(String login, Role role) {
        log.append(INFO, "player-service",
                "Бульбазавр " + login + " сменил роль, теперь он " + roleTitle(role));
    }

    public void leaderboardEntry(String login, GameId game, int position, long value) {
        log.append(INFO, "leaderboard-service",
                "Бульбазавр " + login + " сыграл в игру " + gameTitle(game)
                        + " и попал в лидерборд на " + position + " место с результатом " + result(game, value));
    }

    private static String gameTitle(GameId game) {
        return switch (game) {
            case BULBA_JUMP -> "Bulba Jump";
            case BULBA_PACKER -> "Bulba Packer";
            case BULBA_RACING -> "Bulba Racing";
            case BULBA_PARKING -> "Bulba Parking";
            case BULBA_GUESS -> "Bulba Guess";
            case BULBA_WORDLE -> "Bulba Wordle";
        };
    }

    private static String roleTitle(Role role) {
        return switch (role) {
            case DEV -> "бэкендер";
            case DEV_FE -> "фронтендер";
            case QA -> "тестировщик";
            case LEAD -> "тимлид";
            case ANALYSIS -> "аналитик";
            case DESIGN -> "дизайнер";
            case PRODUCT_OWNER -> "продакт-оунер";
        };
    }

    private static String result(GameId game, long value) {
        return switch (game) {
            case BULBA_PARKING -> String.format(Locale.ROOT, "%.1f с", value / 1000.0);
            case BULBA_GUESS, BULBA_WORDLE -> value + " " + wordsWord(value);
            case BULBA_JUMP, BULBA_PACKER, BULBA_RACING -> value + " " + pointsWord(value);
        };
    }

    private static String attemptsWord(long n) {
        return plural(n, "попытку", "попытки", "попыток");
    }

    private static String wordsWord(long n) {
        return plural(n, "слово", "слова", "слов");
    }

    private static String pointsWord(long n) {
        return plural(n, "очко", "очка", "очков");
    }

    private static String plural(long n, String one, String few, String many) {
        long mod100 = Math.abs(n) % 100;
        long mod10 = mod100 % 10;
        if (mod100 >= 11 && mod100 <= 14) {
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
