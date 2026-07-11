package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.usecase.dto.StoredPlayer;
import ru.bulbasaur.office.usecase.exception.InvalidCredentialsException;
import ru.bulbasaur.office.usecase.exception.WrongPasswordException;
import ru.bulbasaur.office.usecase.port.out.PasswordHasherPort;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.util.UUID;

/** Смена пароля: проверяем старый и сохраняем хеш нового. */
@Service
@RequiredArgsConstructor
public class ChangePasswordUsecase {

    private final PlayerRepositoryPort players;
    private final PasswordHasherPort passwordHasher;

    public void execute(UUID playerId, String oldPassword, String newPassword) {
        StoredPlayer player = players.findById(playerId)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(oldPassword, player.passwordHash())) {
            throw new WrongPasswordException();
        }
        players.updatePassword(playerId, passwordHasher.hash(newPassword));
    }
}
