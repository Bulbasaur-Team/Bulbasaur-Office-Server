package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.QuestCode;
import ru.bulbasaur.office.domain.model.QuestStatus;
import ru.bulbasaur.office.usecase.dto.QuestStatusView;
import ru.bulbasaur.office.usecase.port.out.QuestRepositoryPort;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ListQuestsUsecase {

    private final QuestRepositoryPort quests;
    private final CountOwnedAchievementsUsecase countOwnedAchievements;

    @Transactional(readOnly = true)
    public List<QuestStatusView> execute(UUID playerId) {
        Map<QuestCode, QuestStatus> owned = quests.findStatuses(playerId);
        int achievementCount = countOwnedAchievements.execute(playerId);
        return Arrays.stream(QuestCode.values())
                .map(code -> new QuestStatusView(code.code(), resolveStatus(code, owned, achievementCount)))
                .toList();
    }

    static QuestStatus resolveStatus(QuestCode code, Map<QuestCode, QuestStatus> owned, int achievementCount) {
        QuestStatus stored = owned.get(code);
        if (stored == QuestStatus.COMPLETED || stored == QuestStatus.IN_PROGRESS) {
            return stored;
        }
        if (!prerequisitesMet(code, owned, achievementCount)) {
            return QuestStatus.LOCKED;
        }
        return QuestStatus.AVAILABLE;
    }

    /** Ачивки + опциональный завершённый предыдущий квест. */
    static boolean prerequisitesMet(QuestCode code, Map<QuestCode, QuestStatus> owned, int achievementCount) {
        if (achievementCount < code.minAchievements()) {
            return false;
        }
        return code.requiresCompleted()
                .map(req -> owned.get(req) == QuestStatus.COMPLETED)
                .orElse(true);
    }
}
