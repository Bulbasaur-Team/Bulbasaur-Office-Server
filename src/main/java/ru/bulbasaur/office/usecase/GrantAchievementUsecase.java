package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.usecase.port.out.AchievementNotifierPort;
import ru.bulbasaur.office.usecase.port.out.AchievementRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.util.UUID;

/** Точечная выдача ачивки (событийные условия и результат пересчёта). */
@Service
@RequiredArgsConstructor
public class GrantAchievementUsecase {

    private final AchievementRepositoryPort achievements;
    private final AchievementNotifierPort notifier;
    private final PlayerRepositoryPort players;
    private final CreditBulbaCoinsUsecase credit;
    private final EventLogService eventLog;

    public void execute(UUID playerId, Achievement achievement) {
        if (achievements.grant(playerId, achievement)) {
            credit.execute(playerId, 1000, BulbaCoinKind.ACHIEVEMENT,
                    "achievement:" + achievement.code(),
                    "Ачивка «" + achievement.title() + "»");
            notifier.notifyGranted(playerId, achievement);
            players.findById(playerId)
                    .ifPresent(player -> eventLog.achievementGranted(player.login(), achievement));
        }
    }
}
