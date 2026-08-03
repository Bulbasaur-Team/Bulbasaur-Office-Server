package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.PlayerEquippedEntity;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PlayerEquippedJpaRepository extends JpaRepository<PlayerEquippedEntity, PlayerEquippedEntity.Pk> {

    List<PlayerEquippedEntity> findByPlayerId(UUID playerId);

    List<PlayerEquippedEntity> findByPlayerIdIn(Collection<UUID> playerIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            insert into player_equipped (player_id, category, item_code)
            values (:playerId, :category, :itemCode)
            on conflict (player_id, category) do update set item_code = excluded.item_code
            """, nativeQuery = true)
    int upsert(@Param("playerId") UUID playerId,
               @Param("category") String category,
               @Param("itemCode") String itemCode);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            insert into player_equipped (player_id, category, item_code)
            values (:playerId, :category, :itemCode)
            on conflict (player_id, category) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("playerId") UUID playerId,
                       @Param("category") String category,
                       @Param("itemCode") String itemCode);
}
