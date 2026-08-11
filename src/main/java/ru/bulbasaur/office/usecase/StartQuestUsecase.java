package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.QuestCode;
import ru.bulbasaur.office.domain.model.QuestStatus;
import ru.bulbasaur.office.usecase.dto.QuestStatusView;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.QuestRepositoryPort;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StartQuestUsecase {

    private final QuestRepositoryPort quests;
    private final CountOwnedAchievementsUsecase countOwnedAchievements;
    private final PlayerRepositoryPort players;
    private final EventLogService eventLog;

    @Transactional
    public QuestStatusView execute(UUID playerId, QuestCode quest) {
        QuestStatus existing = quests.findStatus(playerId, quest).orElse(null);
        if (existing == QuestStatus.COMPLETED) {
            return new QuestStatusView(quest.code(), QuestStatus.COMPLETED);
        }
        if (existing == QuestStatus.IN_PROGRESS) {
            return new QuestStatusView(quest.code(), QuestStatus.IN_PROGRESS);
        }
        var owned = quests.findStatuses(playerId);
        int achievementCount = countOwnedAchievements.execute(playerId);
        if (!ListQuestsUsecase.prerequisitesMet(quest, owned, achievementCount)) {
            throw new IllegalArgumentException(lockedMessage(quest));
        }
        if (quests.startIfAbsent(playerId, quest)) {
            players.findById(playerId)
                    .map(StoredPlayer::login)
                    .ifPresent(login -> eventLog.questStarted(login, quest.title()));
        }
        return new QuestStatusView(quest.code(), QuestStatus.IN_PROGRESS);
    }

    private static String lockedMessage(QuestCode quest) {
        if (quest.requiresCompleted().isPresent()) {
            return "Квест пока недоступен: нужно пройти предыдущий квест и набрать минимум "
                    + quest.minAchievements() + " ачивок";
        }
        return "Квест пока недоступен: нужно минимум " + quest.minAchievements() + " ачивок";
    }
}
