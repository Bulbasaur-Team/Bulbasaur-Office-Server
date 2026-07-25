package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroMemeEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroReactionEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroMemeJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroReactionJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroStickerJpaRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ToggleRetroReactionUsecase {

    private final RetroStickerJpaRepository stickers;
    private final RetroMemeJpaRepository memes;
    private final RetroReactionJpaRepository reactions;

    @Transactional
    public RetroResult<Void> execute(
            UUID roomId, String targetType, UUID targetId, UUID playerId, String emoji
    ) {
        if (RetroReactionEntity.TARGET_STICKER.equals(targetType)) {
            if (!RetroConstants.STICKER_EMOJIS.contains(emoji)) {
                return RetroResult.error("Недопустимая реакция.");
            }
            Optional<RetroStickerEntity> sticker = stickers.findById(targetId);
            if (sticker.isEmpty() || !sticker.get().getRoomId().equals(roomId)) {
                return RetroResult.error("Стикер не найден.");
            }
        } else if (RetroReactionEntity.TARGET_MEME.equals(targetType)) {
            if (!RetroConstants.MEME_EMOJI.equals(emoji)) {
                return RetroResult.error("Для мемов доступна только реакция 😂.");
            }
            Optional<RetroMemeEntity> meme = memes.findById(targetId);
            if (meme.isEmpty() || !meme.get().getRoomId().equals(roomId)) {
                return RetroResult.error("Мем не найден.");
            }
        } else {
            return RetroResult.error("Неизвестный тип цели.");
        }

        Optional<RetroReactionEntity> existing = reactions
                .findByTargetTypeAndTargetIdAndPlayerIdAndEmoji(targetType, targetId, playerId, emoji);
        if (existing.isPresent()) {
            reactions.delete(existing.get());
        } else {
            RetroReactionEntity reaction = new RetroReactionEntity();
            reaction.setId(UUID.randomUUID());
            reaction.setTargetType(targetType);
            reaction.setTargetId(targetId);
            reaction.setPlayerId(playerId);
            reaction.setEmoji(emoji);
            reactions.save(reaction);
        }
        return RetroResult.okEmpty();
    }
}
