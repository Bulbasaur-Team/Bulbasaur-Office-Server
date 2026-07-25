package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerGroupEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroStickerGroupJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroStickerJpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MoveRetroStickerUsecase {

    private final RetroStickerJpaRepository stickers;
    private final RetroStickerGroupJpaRepository groups;
    private final RetroStickerBoardHelper boardHelper;

    @Transactional
    public RetroResult<Void> execute(
            UUID roomId,
            String board,
            UUID stickerId,
            String ontoStickerId,
            String ontoGroupId,
            boolean toBoard,
            String beforeStickerId
    ) {
        if (!RetroConstants.BOARDS.contains(board)) {
            return RetroResult.error("Неизвестная доска.");
        }
        Optional<RetroStickerEntity> opt = stickers.findById(stickerId);
        if (opt.isEmpty()) {
            return RetroResult.error("Стикер не найден.");
        }
        RetroStickerEntity moving = opt.get();
        if (!moving.getRoomId().equals(roomId) || !moving.getBoard().equals(board)) {
            return RetroResult.error("Стикер не с этой доски.");
        }
        UUID oldGroup = moving.getGroupId();
        UUID insertBefore = null;
        if (beforeStickerId != null && !beforeStickerId.isBlank()) {
            try {
                insertBefore = UUID.fromString(beforeStickerId);
            } catch (IllegalArgumentException e) {
                return RetroResult.error("Некорректный beforeStickerId.");
            }
            if (insertBefore.equals(stickerId)) {
                insertBefore = null;
            }
        }

        if (ontoStickerId != null && !ontoStickerId.isBlank()) {
            UUID ontoId;
            try {
                ontoId = UUID.fromString(ontoStickerId);
            } catch (IllegalArgumentException e) {
                return RetroResult.error("Некорректный ontoStickerId.");
            }
            if (ontoId.equals(stickerId)) {
                return RetroResult.okEmpty();
            }
            Optional<RetroStickerEntity> ontoOpt = stickers.findById(ontoId);
            if (ontoOpt.isEmpty()) {
                return RetroResult.error("Целевой стикер не найден.");
            }
            RetroStickerEntity onto = ontoOpt.get();
            if (!onto.getRoomId().equals(roomId) || !onto.getBoard().equals(board)) {
                return RetroResult.error("Стикеры должны быть с одной доски.");
            }
            if (onto.getGroupId() != null) {
                moving.setGroupId(onto.getGroupId());
            } else {
                RetroStickerGroupEntity group = new RetroStickerGroupEntity();
                group.setId(UUID.randomUUID());
                group.setRoomId(roomId);
                group.setBoard(board);
                groups.save(group);
                onto.setGroupId(group.getId());
                stickers.save(onto);
                moving.setGroupId(group.getId());
            }
            List<RetroStickerEntity> ordered = boardHelper.boardStickers(roomId, board);
            int ontoIdx = RetroStickerBoardHelper.indexOf(ordered, ontoId);
            UUID before = null;
            if (ontoIdx >= 0 && ontoIdx + 1 < ordered.size()) {
                UUID nextId = ordered.get(ontoIdx + 1).getId();
                if (!nextId.equals(stickerId)) {
                    before = nextId;
                }
            }
            stickers.save(moving);
            boardHelper.renumber(roomId, board, stickerId, before);
        } else if (ontoGroupId != null && !ontoGroupId.isBlank()) {
            UUID groupId;
            try {
                groupId = UUID.fromString(ontoGroupId);
            } catch (IllegalArgumentException e) {
                return RetroResult.error("Некорректный ontoGroupId.");
            }
            if (groups.findById(groupId).isEmpty()) {
                return RetroResult.error("Группа не найдена.");
            }
            moving.setGroupId(groupId);
            stickers.save(moving);
            boardHelper.renumber(roomId, board, stickerId, insertBefore);
        } else if (toBoard) {
            moving.setGroupId(null);
            stickers.save(moving);
            boardHelper.renumber(roomId, board, stickerId, insertBefore);
        } else if (insertBefore != null) {
            stickers.save(moving);
            boardHelper.renumber(roomId, board, stickerId, insertBefore);
        } else {
            return RetroResult.error("Не указана цель перемещения.");
        }

        boardHelper.cleanupGroup(oldGroup);
        boardHelper.coalesceGroups(roomId, board);
        return RetroResult.okEmpty();
    }
}
