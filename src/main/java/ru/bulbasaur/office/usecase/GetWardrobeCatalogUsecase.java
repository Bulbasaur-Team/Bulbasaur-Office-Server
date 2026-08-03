package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.usecase.dto.WardrobeCatalogView;
import ru.bulbasaur.office.usecase.port.out.WardrobeRepositoryPort;

import java.util.UUID;

/** Каталог гардероба: баланс BC, внешность и список предметов. */
@Service
@RequiredArgsConstructor
public class GetWardrobeCatalogUsecase {

    private final WardrobeRepositoryPort wardrobe;
    private final EnsureWardrobeDefaultsUsecase ensureDefaults;
    private final GetBulbaCoinBalanceUsecase balance;

    @Transactional
    public WardrobeCatalogView execute(UUID playerId) {
        ensureDefaults.execute(playerId);
        PlayerAppearance appearance = wardrobe.appearanceOf(playerId);
        return new WardrobeCatalogView(
                balance.execute(playerId),
                appearance,
                wardrobe.catalog(playerId));
    }
}
