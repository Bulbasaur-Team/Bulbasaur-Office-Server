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
            return RetroResult.error("Неподдерживаемый формат изображения.");
        }
        if (data == null || data.length == 0) {
            return RetroResult.error("Пустое изображение.");
        }
        if (data.length > RetroConstants.MAX_MEME_BYTES) {
            return RetroResult.error("Изображение слишком большое (макс. 2 МБ).");
        }
        if (memes.countByRoomId(roomId) >= RetroConstants.MAX_MEMES_PER_ROOM) {
            return RetroResult.error("Слишком много мемов в комнате.");
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
}
