package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroStickerEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroStickerJpaRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddRetroStickerUsecase {

    private final RetroStickerJpaRepository stickers;

    @Transactional
    public RetroResult<Void> execute(UUID roomId, String board, UUID authorId, String text) {
        if (!RetroConstants.BOARDS.contains(board)) {
            return RetroResult.error("Неизвестная доска.");
        }
        String trimmed = text == null ? "" : text.strip();
        if (trimmed.isEmpty()) {
            return RetroResult.error("Текст стикера пуст.");
        }
        if (trimmed.length() > RetroConstants.MAX_STICKER_TEXT) {
            trimmed = trimmed.substring(0, RetroConstants.MAX_STICKER_TEXT);
        }
        if (stickers.countByRoomIdAndBoard(roomId, board) >= RetroConstants.MAX_STICKERS_PER_BOARD) {
            return RetroResult.error("На доске слишком много стикеров.");
        }
        RetroStickerEntity sticker = new RetroStickerEntity();
        sticker.setId(UUID.randomUUID());
        sticker.setRoomId(roomId);
        sticker.setBoard(board);
        sticker.setAuthorId(authorId);
        sticker.setText(trimmed);
        sticker.setSortOrder(stickers.maxSortOrder(roomId, board) + 1);
        sticker.setCreatedAt(Instant.now());
        stickers.save(sticker);
        return RetroResult.okEmpty();
    }
}
