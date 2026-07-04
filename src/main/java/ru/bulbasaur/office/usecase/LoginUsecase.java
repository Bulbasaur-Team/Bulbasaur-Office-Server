package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.usecase.dto.AuthResult;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;
import ru.bulbasaur.office.usecase.exception.InvalidCredentialsException;
import ru.bulbasaur.office.usecase.port.out.PasswordHasherPort;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.TokenPort;

/** Вход существующего игрока по логину/паролю. */
@Service
@RequiredArgsConstructor
public class LoginUsecase {

    private final PlayerRepositoryPort players;
    private final PasswordHasherPort passwordHasher;
    private final TokenPort tokens;

    public AuthResult execute(String login, String rawPassword) {
        StoredPlayer player = players.findByLogin(login)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(rawPassword, player.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return new AuthResult(tokens.issue(player.id(), player.login()), player.login());
    }
}
