package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.PlayerWardrobeEntity;

import java.util.List;
import java.util.UUID;

public interface PlayerWardrobeJpaRepository extends JpaRepository<PlayerWardrobeEntity, PlayerWardrobeEntity.Pk> {

    List<PlayerWardrobeEntity> findByPlayerId(UUID playerId);

    boolean existsByPlayerIdAndItemCode(UUID playerId, String itemCode);

    @Modifying
    @Query(value = """
            insert into player_wardrobe (player_id, item_code, purchased_at)
            values (:playerId, :itemCode, now())
            on conflict do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("playerId") UUID playerId, @Param("itemCode") String itemCode);

    void deleteByPlayerIdAndItemCode(UUID playerId, String itemCode);
}
