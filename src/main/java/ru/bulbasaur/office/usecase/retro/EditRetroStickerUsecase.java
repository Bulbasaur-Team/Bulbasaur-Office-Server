package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroStickerJpaRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EditRetroStickerUsecase {

    private final RetroStickerJpaRepository stickers;

    @Transactional
    public RetroResult<Void> execute(UUID stickerId, UUID playerId, String text) {
        Optional<RetroStickerEntity> opt = stickers.findById(stickerId);
        if (opt.isEmpty()) {
            return RetroResult.error("Стикер не найден.");
        }
        RetroStickerEntity sticker = opt.get();
        if (!sticker.getAuthorId().equals(playerId)) {
            return RetroResult.error("Редактировать можно только свой стикер.");
        }
        String trimmed = text == null ? "" : text.strip();
        if (trimmed.isEmpty()) {
            return RetroResult.error("Текст стикера пуст.");
        }
        if (trimmed.length() > RetroConstants.MAX_STICKER_TEXT) {
            trimmed = trimmed.substring(0, RetroConstants.MAX_STICKER_TEXT);
        }
        sticker.setText(trimmed);
        stickers.save(sticker);
        return RetroResult.okEmpty();
    }
}
