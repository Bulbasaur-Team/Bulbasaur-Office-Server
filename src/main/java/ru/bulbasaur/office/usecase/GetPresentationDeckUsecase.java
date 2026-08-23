package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.QuestCode;
import ru.bulbasaur.office.domain.model.QuestStatus;
import ru.bulbasaur.office.usecase.dto.PresentationDeckView;
import ru.bulbasaur.office.usecase.port.out.PresentationDeckRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.QuestRepositoryPort;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPresentationDeckUsecase {

    private final QuestRepositoryPort quests;
    private final PresentationDeckRepositoryPort decks;

    @Transactional(readOnly = true)
    public PresentationDeckView execute(UUID playerId) {
        QuestStatus status = quests.findStatus(playerId, QuestCode.PRESENTATION).orElse(QuestStatus.LOCKED);
        List<String> slideIds = decks.find(playerId).orElse(List.of());
        return new PresentationDeckView(slideIds, status.name());
    }
}
