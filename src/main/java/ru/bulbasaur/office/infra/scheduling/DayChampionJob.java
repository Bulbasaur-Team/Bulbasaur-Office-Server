package ru.bulbasaur.office.infra.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.bulbasaur.office.usecase.AchievementService;

/**
 * Выдаёт ачивку «Чемпион дня» победителям слова дня за прошедшие сутки. Запускается
 * вскоре после смены московских суток (слово дня меняется в 00:00 Europe/Moscow),
 * когда рейтинг прошлого дня уже финализирован.
 */
@Component
@RequiredArgsConstructor
public class DayChampionJob {

    private final AchievementService achievements;

    @Scheduled(cron = "0 5 0 * * *", zone = "Europe/Moscow")
    public void awardDayChampions() {
        achievements.awardDayChampions();
    }
}
