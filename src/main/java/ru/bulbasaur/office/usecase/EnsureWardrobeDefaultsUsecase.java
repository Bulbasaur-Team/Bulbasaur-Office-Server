package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.usecase.port.out.WardrobeRepositoryPort;

import java.util.UUID;

/** Выдать игроку стартовый комплект одежды, если его ещё нет. */
@Service
@RequiredArgsConstructor
public class EnsureWardrobeDefaultsUsecase {

    private final WardrobeRepositoryPort wardrobe;

    @Transactional
    public void execute(UUID playerId) {
        wardrobe.grantDefaults(playerId);
    }
}
