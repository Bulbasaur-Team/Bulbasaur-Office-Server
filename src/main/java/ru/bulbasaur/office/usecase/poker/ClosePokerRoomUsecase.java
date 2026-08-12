package ru.bulbasaur.office.usecase.poker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.usecase.port.out.PokerRepositoryPort;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClosePokerRoomUsecase {

    private final PokerRepositoryPort pokerPort;

    public void execute(UUID roomId) {
        pokerPort.closeSession(roomId);
    }

    /**
     * Закрыть ACTIVE-комнаты, которых нет в памяти (рестарт сервера) или у которых
     * вышел TTL — иначе они навсегда занимали бы слот и не попадали в историю.
     */
    public void reconcile(Set<UUID> liveIds) {
        pokerPort.closeStaleSessions(liveIds, Instant.now());
    }
}
