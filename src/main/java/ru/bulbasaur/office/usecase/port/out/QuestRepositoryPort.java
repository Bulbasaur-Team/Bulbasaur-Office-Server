package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.QuestCode;
import ru.bulbasaur.office.domain.model.QuestStatus;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface QuestRepositoryPort {

    Map<QuestCode, QuestStatus> findStatuses(UUID playerId);

    Optional<QuestStatus> findStatus(UUID playerId, QuestCode quest);

    /** true — запись создана сейчас как IN_PROGRESS. */
    boolean startIfAbsent(UUID playerId, QuestCode quest);

    /** true — статус только что стал COMPLETED. */
    boolean complete(UUID playerId, QuestCode quest);
}
