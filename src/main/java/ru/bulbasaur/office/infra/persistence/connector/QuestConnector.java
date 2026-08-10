package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.QuestCode;
import ru.bulbasaur.office.domain.model.QuestStatus;
import ru.bulbasaur.office.infra.persistence.entity.PlayerQuestEntity;
import ru.bulbasaur.office.infra.persistence.repository.PlayerQuestJpaRepository;
import ru.bulbasaur.office.usecase.port.out.QuestRepositoryPort;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QuestConnector implements QuestRepositoryPort {

    private final PlayerQuestJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Map<QuestCode, QuestStatus> findStatuses(UUID playerId) {
        Map<QuestCode, QuestStatus> result = new EnumMap<>(QuestCode.class);
        for (PlayerQuestEntity entity : repository.findByPlayerId(playerId)) {
            QuestCode.fromCode(entity.getQuestCode())
                    .ifPresent(code -> result.put(code, entity.getStatus()));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<QuestStatus> findStatus(UUID playerId, QuestCode quest) {
        return repository.findByPlayerIdAndQuestCode(playerId, quest.code())
                .map(PlayerQuestEntity::getStatus);
    }

    @Override
    @Transactional
    public boolean startIfAbsent(UUID playerId, QuestCode quest) {
        return repository.insertInProgressIfAbsent(playerId, quest.code()) > 0;
    }

    @Override
    @Transactional
    public boolean complete(UUID playerId, QuestCode quest) {
        // На случай complete без предварительного start — сначала создаём IN_PROGRESS.
        repository.insertInProgressIfAbsent(playerId, quest.code());
        return repository.markCompleted(playerId, quest.code()) > 0;
    }
}
