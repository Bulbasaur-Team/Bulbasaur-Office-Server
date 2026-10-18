package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroMemeEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroMemeJpaRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddRetroMemeUsecase {

    private final RetroMemeJpaRepository memes;

    @Transactional
    public RetroResult<UUID> execute(UUID roomId, UUID authorId, String mimeType, byte[] data) {
        if (mimeType == null || !RetroConstants.MEME_MIME.contains(mimeType)) {
            return RetroResult.error("Формат не подходит. Нужны PNG, JPEG, WebP или GIF.");
        }
        if (data == null || data.length == 0) {
            return RetroResult.error("Файл пустой.");
        }
        if (data.length > RetroConstants.MAX_MEME_BYTES) {
            return RetroResult.error(tooLargeMessage(data.length));
        }
        if (memes.countByRoomId(roomId) >= RetroConstants.MAX_MEMES_PER_ROOM) {
            return RetroResult.error("В комнате уже " + RetroConstants.MAX_MEMES_PER_ROOM
                    + " мемов — это максимум. Удалите один, чтобы добавить новый.");
        }
        UUID id = UUID.randomUUID();
        RetroMemeEntity meme = new RetroMemeEntity();
        meme.setId(id);
        meme.setRoomId(roomId);
        meme.setAuthorId(authorId);
        meme.setMimeType(mimeType);
        meme.setImageData(data);
        meme.setCreatedAt(Instant.now());
        memes.save(meme);
        return RetroResult.ok(id);
    }

    /** Текст совпадает с retroMemeFileError на клиенте. */
    static String tooLargeMessage(int bytes) {
        return "Картинка весит " + formatWeight(bytes) + ". Максимум — 2 МБ, сожмите её или выберите другую.";
    }

    static String formatWeight(int bytes) {
        if (bytes < 0) {
            bytes = 0;
        }
        double mb = bytes / (1024d * 1024d);
        if (mb < 0.1d) {
            int kb = Math.max(1, (int) Math.ceil(bytes / 1024d));
            return kb + " КБ";
        }
        if (mb >= 10d) {
            return Math.round(mb) + " МБ";
        }
        int tenths = (int) Math.ceil(mb * 10d - 1e-9d);
        if (tenths % 10 == 0) {
            return (tenths / 10) + " МБ";
        }
        return (tenths / 10) + "," + (tenths % 10) + " МБ";
    }
}
