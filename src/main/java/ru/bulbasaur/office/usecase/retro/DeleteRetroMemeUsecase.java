package ru.bulbasaur.office.usecase.retro;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.RetroMemeEntity;
import ru.bulbasaur.office.infra.persistence.entity.RetroReactionEntity;
import ru.bulbasaur.office.infra.persistence.repository.RetroMemeJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.RetroReactionJpaRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteRetroMemeUsecase {

    private final RetroMemeJpaRepository memes;
    private final RetroReactionJpaRepository reactions;

    @Transactional
    public RetroResult<Void> execute(UUID memeId, UUID playerId) {
        Optional<RetroMemeEntity> opt = memes.findById(memeId);
        if (opt.isEmpty()) {
            return RetroResult.error("Мем не найден.");
        }
        RetroMemeEntity meme = opt.get();
        if (!meme.getAuthorId().equals(playerId)) {
            return RetroResult.error("Удалить можно только свой мем.");
        }
        reactions.deleteByTargetTypeAndTargetId(RetroReactionEntity.TARGET_MEME, memeId);
        memes.delete(meme);
        return RetroResult.okEmpty();
    }
}
