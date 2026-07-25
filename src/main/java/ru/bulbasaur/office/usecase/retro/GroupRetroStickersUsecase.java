package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerGroupEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroStickerGroupJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroStickerJpaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupRetroStickersUsecase {

    private final RetroStickerJpaRepository stickers;
    private final RetroStickerGroupJpaRepository groups;
    private final RetroStickerBoardHelper boardHelper;

    @Transactional
    public RetroResult<Void> execute(UUID roomId, String board, List<UUID> stickerIds) {
        if (!RetroConstants.BOARDS.contains(board) || stickerIds == null || stickerIds.size() < 2) {
            return RetroResult.error("Нужно выбрать минимум 2 стикера.");
        }
        List<RetroStickerEntity> selected = stickers.findAllById(stickerIds);
        if (selected.size() != stickerIds.size()) {
            return RetroResult.error("Стикер не найден.");
        }
        for (RetroStickerEntity s : selected) {
            if (!s.getRoomId().equals(roomId) || !s.getBoard().equals(board)) {
                return RetroResult.error("Стикеры должны быть с одной доски.");
            }
        }
        RetroStickerGroupEntity group = new RetroStickerGroupEntity();
        group.setId(UUID.randomUUID());
        group.setRoomId(roomId);
        group.setBoard(board);
        groups.save(group);
        for (RetroStickerEntity s : selected) {
            s.setGroupId(group.getId());
            stickers.save(s);
        }
        List<RetroStickerEntity> ordered = new ArrayList<>(boardHelper.boardStickers(roomId, board));
        List<RetroStickerEntity> picked = new ArrayList<>();
        for (UUID id : stickerIds) {
            ordered.stream().filter(s -> s.getId().equals(id)).findFirst().ifPresent(picked::add);
        }
        ordered.removeIf(s -> stickerIds.contains(s.getId()));
        ordered.addAll(picked);
        boardHelper.applyOrder(ordered);
        boardHelper.coalesceGroups(roomId, board);
        return RetroResult.okEmpty();
    }
}
