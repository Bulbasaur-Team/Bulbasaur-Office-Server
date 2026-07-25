package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.bulbasaur.office.infra.persistence.entity.RetroReactionEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RetroReactionJpaRepository extends JpaRepository<RetroReactionEntity, UUID> {

    List<RetroReactionEntity> findByTargetTypeAndTargetIdIn(String targetType, Collection<UUID> targetIds);

    Optional<RetroReactionEntity> findByTargetTypeAndTargetIdAndPlayerIdAndEmoji(
            String targetType, UUID targetId, UUID playerId, String emoji);

    void deleteByTargetTypeAndTargetId(String targetType, UUID targetId);
}
