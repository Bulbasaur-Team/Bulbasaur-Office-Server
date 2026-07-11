package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Role;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.util.Optional;
import java.util.UUID;

/** Профиль текущего игрока: выбранная роль (empty — ещё не выбрана). */
@Service
@RequiredArgsConstructor
public class GetProfileUsecase {

    private final PlayerRepositoryPort players;

    public Optional<Role> execute(UUID playerId) {
        return players.roleOf(playerId);
    }
}
