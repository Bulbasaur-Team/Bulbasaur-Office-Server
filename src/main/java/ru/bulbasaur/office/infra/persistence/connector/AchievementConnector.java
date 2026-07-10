package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.infra.persistence.entity.AchievementEntity;
import ru.bulbasaur.office.infra.persistence.repository.AchievementJpaRepository;
import ru.bulbasaur.office.usecase.port.out.AchievementRepositoryPort;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AchievementConnector implements AchievementRepositoryPort {

    private final AchievementJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Set<Achievement> findOwned(UUID playerId) {
        Set<Achievement> owned = EnumSet.noneOf(Achievement.class);
        for (AchievementEntity entity : repository.findByPlayerId(playerId)) {
            Achievement.fromCode(entity.getCode()).ifPresent(owned::add);
        }
        return owned;
    }

    @Override
    @Transactional
    public boolean grant(UUID playerId, Achievement achievement) {
        return repository.insertIfAbsent(playerId, achievement.code()) > 0;
    }
}
