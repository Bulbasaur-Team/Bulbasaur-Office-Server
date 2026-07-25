package ru.bulbasaur.office.usecase.retro;

import ru.bulbasaur.office.infra.persistence.entity.RetroStickerEntity;

import java.util.Set;

/** Лимиты и допустимые значения ретро. */
public final class RetroConstants {

    public static final int MAX_ACTIVE_ROOMS = 20;
    public static final int MAX_STICKERS_PER_BOARD = 80;
    public static final int MAX_MEMES_PER_ROOM = 40;
    public static final int MAX_MEME_BYTES = 2 * 1024 * 1024;
    public static final int MAX_STICKER_TEXT = 500;

    public static final Set<String> BOARDS = Set.of(
            RetroStickerEntity.BOARD_GOOD,
            RetroStickerEntity.BOARD_IMPROVE,
            RetroStickerEntity.BOARD_ACTIONS);

    public static final Set<String> STICKER_EMOJIS = Set.of("👍", "❤️", "😂", "😢", "🔥", "🎉");
    public static final String MEME_EMOJI = "😂";
    public static final Set<String> MEME_MIME = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    private RetroConstants() {
    }
}
