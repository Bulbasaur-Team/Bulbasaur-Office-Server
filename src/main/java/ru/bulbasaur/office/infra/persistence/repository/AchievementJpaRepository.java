package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.AchievementEntity;

import java.util.List;
import java.util.UUID;

public interface AchievementJpaRepository extends JpaRepository<AchievementEntity, UUID> {

    List<AchievementEntity> findByPlayerId(UUID playerId);

    /**
     * Выдать ачивку, если её ещё нет. Возвращает число вставленных строк:
     * 1 — выдана только что, 0 — уже была.
     */
    @Modifying
    @Query(value = """
            insert into achievements (id, player_id, code, granted_at)
            values (gen_random_uuid(), :playerId, :code, now())
            on conflict (player_id, code) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("playerId") UUID playerId, @Param("code") String code);
}
