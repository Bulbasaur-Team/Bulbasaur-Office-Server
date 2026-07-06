package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.usecase.port.out.PlayerRepositoryPort;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeleteAccountUsecase {

    private final PlayerRepositoryPort players;

    public void execute(UUID playerId) {
        players.deleteById(playerId);
    }
}
