package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Player;
import ru.bulbasaur.office.usecase.dto.AuthResult;
import ru.bulbasaur.office.usecase.exception.LoginAlreadyTakenException;
import ru.bulbasaur.office.usecase.port.out.PasswordHasherPort;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.TokenPort;

/** Регистрация нового игрока по логину/паролю. */
@Service
@RequiredArgsConstructor
public class RegisterUsecase {

    private final PlayerRepositoryPort players;
    private final PasswordHasherPort passwordHasher;
    private final TokenPort tokens;
    private final RecheckAchievementsUsecase recheckAchievements;
    private final EventLogService eventLog;
    private final EnsureWardrobeDefaultsUsecase ensureDefaults;

    public AuthResult execute(String login, String rawPassword) {
        if (players.existsByLogin(login)) {
            throw new LoginAlreadyTakenException(login);
        }
        Player player = players.create(login, passwordHasher.hash(rawPassword));
        eventLog.playerRegistered(player.login());
        ensureDefaults.execute(player.id());
        recheckAchievements.execute(player.id());
        return new AuthResult(tokens.issue(player.id(), player.login()), player.login());
    }
}
