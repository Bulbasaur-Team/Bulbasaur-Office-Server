package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.BulbaCoinKind;
import ru.bulbasaur.office.domain.model.QuestCode;
import ru.bulbasaur.office.domain.model.QuestStatus;
import ru.bulbasaur.office.usecase.dto.QuestCompleteView;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;
import ru.bulbasaur.office.usecase.port.out.BulbaCoinRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.QuestRepositoryPort;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompleteQuestUsecase {

    private final QuestRepositoryPort quests;
    private final CountOwnedAchievementsUsecase countOwnedAchievements;
    private final CreditBulbaCoinsUsecase credit;
    private final BulbaCoinRepositoryPort coins;
    private final PlayerRepositoryPort players;
    private final EventLogService eventLog;

    @Transactional
    public QuestCompleteView execute(UUID playerId, QuestCode quest, String pin) {
        if (pin == null || !quest.pin().equalsIgnoreCase(pin.trim())) {
            throw new IllegalArgumentException("Неверный код");
        }

        QuestStatus existing = quests.findStatus(playerId, quest).orElse(QuestStatus.AVAILABLE);
        if (existing == QuestStatus.COMPLETED) {
            return new QuestCompleteView(QuestStatus.COMPLETED, coins.balanceOf(playerId), false);
        }

        // Завершить можно, если квест уже начат, либо если игрок достаточно «прокачан».
        if (existing != QuestStatus.IN_PROGRESS) {
            var owned = quests.findStatuses(playerId);
            int achievementCount = countOwnedAchievements.execute(playerId);
            if (!ListQuestsUsecase.prerequisitesMet(quest, owned, achievementCount)) {
                throw new IllegalArgumentException(
                        "Квест пока недоступен: нужно минимум " + quest.minAchievements() + " ачивок");
            }
        }

        boolean newlyCompleted = quests.complete(playerId, quest);
        boolean rewarded = credit.execute(
                playerId,
                quest.rewardBc(),
                BulbaCoinKind.QUEST_REWARD,
                "quest:" + quest.code(),
                quest.rewardTitle());
        if (newlyCompleted) {
            players.findById(playerId)
                    .map(StoredPlayer::login)
                    .ifPresent(login -> eventLog.questCompleted(login, quest.title()));
        }
        return new QuestCompleteView(QuestStatus.COMPLETED, coins.balanceOf(playerId), rewarded);
    }
}
