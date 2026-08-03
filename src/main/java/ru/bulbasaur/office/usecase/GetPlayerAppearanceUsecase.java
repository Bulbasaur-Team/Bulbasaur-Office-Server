package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.usecase.port.out.WardrobeRepositoryPort;

import java.util.UUID;

/** Текущая внешность игрока (экипированные слоты). */
@Service
@RequiredArgsConstructor
public class GetPlayerAppearanceUsecase {

    private final WardrobeRepositoryPort wardrobe;

    @Transactional(readOnly = true)
    public PlayerAppearance execute(UUID playerId) {
        return wardrobe.appearanceOf(playerId);
    }
}
