package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroMemeEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroMemeJpaRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetRetroMemeUsecase {

    private final RetroMemeJpaRepository memes;

    @Transactional(readOnly = true)
    public Optional<RetroMemeEntity> execute(UUID memeId) {
        return memes.findById(memeId);
    }
}
