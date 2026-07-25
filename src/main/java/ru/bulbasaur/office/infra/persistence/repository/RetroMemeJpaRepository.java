package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.RetroMemeEntity;

import java.util.List;
import java.util.UUID;

public interface RetroMemeJpaRepository extends JpaRepository<RetroMemeEntity, UUID> {

    @Query("select m.id as id, m.roomId as roomId, m.authorId as authorId, m.mimeType as mimeType, m.createdAt as createdAt from RetroMemeEntity m where m.roomId = :roomId order by m.createdAt asc")
    List<MemeMeta> findMetaByRoomId(@Param("roomId") UUID roomId);

    long countByRoomId(UUID roomId);

    interface MemeMeta {
        UUID getId();

        UUID getRoomId();

        UUID getAuthorId();

        String getMimeType();
    }
}
