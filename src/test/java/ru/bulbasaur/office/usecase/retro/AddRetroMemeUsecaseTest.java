package ru.bulbasaur.office.usecase.retro;

import org.junit.jupiter.api.Test;
import ru.bulbasaur.office.infra.persistence.repository.RetroMemeJpaRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AddRetroMemeUsecaseTest {

    private final RetroMemeJpaRepository memes = mock(RetroMemeJpaRepository.class);
    private final AddRetroMemeUsecase usecase = new AddRetroMemeUsecase(memes);

    @Test
    void tooLargeSaysActualWeightAndLimit() {
        byte[] data = new byte[RetroConstants.MAX_MEME_BYTES + 1];
        RetroResult<UUID> result = usecase.execute(UUID.randomUUID(), UUID.randomUUID(), "image/png", data);

        assertThat(result.ok()).isFalse();
        assertThat(result.error()).isEqualTo(
                "Картинка весит 2,1 МБ. Максимум — 2 МБ, сожмите её или выберите другую.");
    }

    @Test
    void badFormatListsAllowedTypes() {
        RetroResult<UUID> result = usecase.execute(UUID.randomUUID(), UUID.randomUUID(), "image/heic", new byte[]{1});

        assertThat(result.error()).isEqualTo("Формат не подходит. Нужны PNG, JPEG, WebP или GIF.");
    }

    @Test
    void emptyFileSaysSo() {
        RetroResult<UUID> result = usecase.execute(UUID.randomUUID(), UUID.randomUUID(), "image/png", new byte[0]);

        assertThat(result.error()).isEqualTo("Файл пустой.");
    }

    @Test
    void roomFullSaysCap() {
        when(memes.countByRoomId(any())).thenReturn((long) RetroConstants.MAX_MEMES_PER_ROOM);
        RetroResult<UUID> result = usecase.execute(UUID.randomUUID(), UUID.randomUUID(), "image/jpeg", new byte[]{1});

        assertThat(result.error()).isEqualTo(
                "В комнате уже 40 мемов — это максимум. Удалите один, чтобы добавить новый.");
    }
}
