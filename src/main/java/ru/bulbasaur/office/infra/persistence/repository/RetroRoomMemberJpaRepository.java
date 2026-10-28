package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomMemberEntity;

import java.util.Set;
import java.util.UUID;

public interface RetroRoomMemberJpaRepository extends JpaRepository<RetroRoomMemberEntity, RetroRoomMemberEntity.Pk> {

    @Query("select m.roomId from RetroRoomMemberEntity m where m.playerId = :playerId")
    Set<UUID> findRoomIdsByPlayerId(@Param("playerId") UUID playerId);

    @Modifying
    @Query(value = """
            insert into retro_room_members(room_id, player_id, granted_at)
            values (:roomId, :playerId, now())
            on conflict do nothing
            """, nativeQuery = true)
    void grant(@Param("roomId") UUID roomId, @Param("playerId") UUID playerId);
}
