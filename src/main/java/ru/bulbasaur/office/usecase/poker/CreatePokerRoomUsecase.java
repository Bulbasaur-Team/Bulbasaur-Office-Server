package ru.bulbasaur.office.usecase.poker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.PokerSession;
import ru.bulbasaur.office.usecase.port.out.PokerRepositoryPort;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePokerRoomUsecase {

    private final PokerRepositoryPort pokerPort;

    public PokerSession execute(String name, UUID adminPlayerId) {
        if (pokerPort.countActiveSessions() >= PokerConstants.MAX_ACTIVE_ROOMS) {
            return null;
        }
        return pokerPort.createSession(
                name, adminPlayerId, Instant.now().plusMillis(PokerConstants.TTL_MS));
    }
}
