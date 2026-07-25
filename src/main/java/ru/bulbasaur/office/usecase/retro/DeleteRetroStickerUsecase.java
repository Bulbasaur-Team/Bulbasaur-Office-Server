package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroReactionEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroReactionJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroStickerJpaRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteRetroStickerUsecase {

    private final RetroStickerJpaRepository stickers;
    private final RetroReactionJpaRepository reactions;
    private final RetroStickerBoardHelper boardHelper;

    @Transactional
    public RetroResult<Void> execute(UUID stickerId, UUID playerId) {
        Optional<RetroStickerEntity> opt = stickers.findById(stickerId);
        if (opt.isEmpty()) {
            return RetroResult.error("Стикер не найден.");
        }
        RetroStickerEntity sticker = opt.get();
        if (!sticker.getAuthorId().equals(playerId)) {
            return RetroResult.error("Удалить можно только свой стикер.");
        }
        reactions.deleteByTargetTypeAndTargetId(RetroReactionEntity.TARGET_STICKER, stickerId);
        UUID oldGroup = sticker.getGroupId();
        stickers.delete(sticker);
        boardHelper.cleanupGroup(oldGroup);
        return RetroResult.okEmpty();
    }
}
