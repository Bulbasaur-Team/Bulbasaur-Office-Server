package ru.bulbasaur.office.usecase.poker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.PokerSession;
import ru.bulbasaur.office.usecase.port.out.PokerRepositoryPort;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Чтение покер-сессий и имён игроков. */
@Service
@RequiredArgsConstructor
public class GetPokerRoomsUsecase {

    private final PokerRepositoryPort pokerPort;

    public List<PokerSession> findClosedRooms() {
        return pokerPort.findClosedSessions();
    }

    public Map<UUID, String> loginMap(Set<UUID> ids) {
        return pokerPort.loginsOf(ids);
    }
}
