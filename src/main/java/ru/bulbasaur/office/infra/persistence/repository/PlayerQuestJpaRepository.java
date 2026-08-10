package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.PlayerQuestEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayerQuestJpaRepository extends JpaRepository<PlayerQuestEntity, UUID> {

    List<PlayerQuestEntity> findByPlayerId(UUID playerId);

    Optional<PlayerQuestEntity> findByPlayerIdAndQuestCode(UUID playerId, String questCode);

    /**
     * Начать квест, если записи ещё нет. Возвращает число вставленных строк:
     * 1 — начат сейчас, 0 — уже был.
     */
    @Modifying
    @Query(value = """
            insert into player_quests (id, player_id, quest_code, status, started_at, completed_at)
            values (gen_random_uuid(), :playerId, :questCode, 'IN_PROGRESS', now(), null)
            on conflict (player_id, quest_code) do nothing
            """, nativeQuery = true)
    int insertInProgressIfAbsent(@Param("playerId") UUID playerId, @Param("questCode") String questCode);

    /**
     * Завершить квест, если он ещё не COMPLETED.
     * Возвращает число обновлённых строк: 1 — завершён сейчас, 0 — уже был / нет строки.
     */
    @Modifying
    @Query(value = """
            update player_quests
            set status = 'COMPLETED', completed_at = now()
            where player_id = :playerId
              and quest_code = :questCode
              and status <> 'COMPLETED'
            """, nativeQuery = true)
    int markCompleted(@Param("playerId") UUID playerId, @Param("questCode") String questCode);
}
