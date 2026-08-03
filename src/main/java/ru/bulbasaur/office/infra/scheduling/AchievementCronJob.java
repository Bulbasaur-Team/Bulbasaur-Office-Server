package ru.bulbasaur.office.infra.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.bulbasaur.office.usecase.RecheckAchievementsUsecase;

/**
 * Страховочный прогон: раз в 5 минут пересчитывает ачивки всех игроков и выдаёт
 * недостающие. Нужен в первую очередь старым игрокам, которые уже заслужили ачивки
 * до появления фичи; в норме ачивки выдаются моментально по событию.
 */
@Component
@RequiredArgsConstructor
public class AchievementCronJob {

    private final RecheckAchievementsUsecase recheckAchievements;

    @Scheduled(cron = "0 */5 * * * *")
    public void recheckAll() {
        recheckAchievements.executeAll();
    }
}
