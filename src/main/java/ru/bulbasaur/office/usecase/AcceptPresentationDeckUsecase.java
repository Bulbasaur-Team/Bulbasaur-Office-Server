package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.PresentationDeckRules;
import ru.bulbasaur.office.domain.model.QuestCode;
import ru.bulbasaur.office.domain.model.QuestStatus;
import ru.bulbasaur.office.usecase.dto.PresentationDeckView;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.PresentationDeckRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.QuestRepositoryPort;

import java.util.List;
import java.util.UUID;

/** Принять колоду Бульбулем: сохранить порядок для Дня X и закрыть квест. */
@Service
@RequiredArgsConstructor
public class AcceptPresentationDeckUsecase {

    private final QuestRepositoryPort quests;
    private final PresentationDeckRepositoryPort decks;
    private final PlayerRepositoryPort players;
    private final EventLogService eventLog;

    @Transactional
    public PresentationDeckView execute(UUID playerId, List<String> slideIds) {
        QuestStatus status = quests.findStatus(playerId, QuestCode.PRESENTATION).orElse(QuestStatus.LOCKED);
        if (status == QuestStatus.COMPLETED) {
            List<String> existing = decks.find(playerId).orElse(List.of());
            return new PresentationDeckView(existing, QuestStatus.COMPLETED.name());
        }
        if (status != QuestStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Сначала нужно взять квест «Сборка презентации»");
        }
        List<String> normalized = slideIds == null ? List.of() : List.copyOf(slideIds);
        String reason = PresentationDeckRules.rejectReason(normalized);
        if (reason != null) {
            throw new IllegalArgumentException(reason);
        }
        decks.save(playerId, normalized);
        boolean newlyCompleted = quests.complete(playerId, QuestCode.PRESENTATION);
        if (newlyCompleted) {
            players.findById(playerId)
                    .map(StoredPlayer::login)
                    .ifPresent(login -> eventLog.questCompleted(login, QuestCode.PRESENTATION.title()));
        }
        return new PresentationDeckView(normalized, QuestStatus.COMPLETED.name());
    }
}
