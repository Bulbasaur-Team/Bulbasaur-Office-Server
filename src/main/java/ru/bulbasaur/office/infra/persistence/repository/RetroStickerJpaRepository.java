package ru.bulbasaur.office.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerEntity;

import java.util.List;
import java.util.UUID;

public interface RetroStickerJpaRepository extends JpaRepository<RetroStickerEntity, UUID> {

    List<RetroStickerEntity> findByRoomIdOrderBySortOrderAscCreatedAtAsc(UUID roomId);

    List<RetroStickerEntity> findByRoomIdAndBoardOrderBySortOrderAscCreatedAtAsc(UUID roomId, String board);

    long countByRoomIdAndBoard(UUID roomId, String board);

    List<RetroStickerEntity> findByGroupId(UUID groupId);

    @Query("select coalesce(max(s.sortOrder), -1) from RetroStickerEntity s where s.roomId = :roomId and s.board = :board")
    int maxSortOrder(@Param("roomId") UUID roomId, @Param("board") String board);
}
