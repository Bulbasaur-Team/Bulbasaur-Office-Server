package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.usecase.dto.ProfileView;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;
import ru.bulbasaur.office.usecase.exception.PlayerNotFoundException;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.util.UUID;

/** Профиль текущего игрока: баланс BC и внешность. */
@Service
@RequiredArgsConstructor
public class GetProfileUsecase {

    private final PlayerRepositoryPort players;
    private final GetBulbaCoinBalanceUsecase balance;
    private final EnsureWardrobeDefaultsUsecase ensureDefaults;
    private final GetPlayerAppearanceUsecase appearance;

    public ProfileView execute(UUID playerId) {
        StoredPlayer player = players.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException(playerId.toString()));
        ensureDefaults.execute(playerId);
        return new ProfileView(player.login(), balance.execute(playerId), appearance.execute(playerId));
    }
}
