package ru.bulbasaur.office.usecase.poker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.PokerScale;
import ru.bulbasaur.office.domain.model.PokerSession;
import ru.bulbasaur.office.usecase.port.out.PokerRepositoryPort;
import ru.bulbasaur.office.usecase.room.RoomAccessUsecase;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreatePokerRoomUsecase {

    private final PokerRepositoryPort pokerPort;
    private final RoomAccessUsecase roomAccess;

    @Transactional
    public PokerSession execute(String name, PokerScale scale, UUID adminPlayerId, String passwordHash) {
        if (pokerPort.countActiveSessions() >= PokerConstants.MAX_ACTIVE_ROOMS) {
            return null;
        }
        PokerSession session = pokerPort.createSession(
                name, scale, adminPlayerId, Instant.now().plusMillis(PokerConstants.TTL_MS), passwordHash);
        if (passwordHash != null) {
            roomAccess.grantPoker(session.getId(), adminPlayerId);
        }
        return session;
    }
}
