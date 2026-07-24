package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.usecase.dto.AuthResult;
import ru.bulbasaur.office.usecase.port.out.TokenPort;

import java.util.UUID;

/** Выпустить новый JWT по уже аутентифицированному игроку (sliding session). */
@Service
@RequiredArgsConstructor
public class RefreshTokenUsecase {

    private final TokenPort tokens;

    public AuthResult execute(UUID playerId, String login) {
        return new AuthResult(tokens.issue(playerId, login), login);
    }
}
