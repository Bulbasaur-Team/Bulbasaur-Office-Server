package ru.bulbasaur.office.infra.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.bulbasaur.office.domain.model.PokerSession;
import ru.bulbasaur.office.usecase.poker.ClosePokerRoomUsecase;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр активных покер-комнат. Живое голосование — в памяти; в БД уходят
 * карточка комнаты и результаты. Истёкшие и «осиротевшие» после рестарта
 * комнаты закрываются лениво при обращении.
 */
@Component
@RequiredArgsConstructor
public class PokerRegistry {

    private final ClosePokerRoomUsecase closeRoom;
    private final Map<String, PokerRoom> rooms = new ConcurrentHashMap<>();

    /** Активные комнаты для лобби (истёкшие по пути закрываются). */
    public List<PokerRoom> active() {
        expireStale();
        return rooms.values().stream()
                .sorted(Comparator.comparing(PokerRoom::name))
                .toList();
    }

    public PokerRoom create(PokerSession session, String adminLogin) {
        PokerRoom room = new PokerRoom(
                session.getId().toString(),
                session.getName(),
                session.getAdminPlayerId(),
                adminLogin,
                session.getClosesAt().toEpochMilli());
        rooms.put(room.id(), room);
        return room;
    }

    public PokerRoom get(String roomId) {
        if (roomId == null) {
            return null;
        }
        expireStale();
        return rooms.get(roomId);
    }

    /** Комната, в которой игрок сейчас участвует (или null). */
    public PokerRoom roomOf(UUID playerId) {
        for (PokerRoom room : active()) {
            if (room.hasParticipant(playerId)) {
                return room;
            }
        }
        return null;
    }

    public void remove(String roomId) {
        rooms.remove(roomId);
    }

    public Set<UUID> liveIds() {
        Set<UUID> ids = new HashSet<>();
        for (String id : rooms.keySet()) {
            ids.add(UUID.fromString(id));
        }
        return ids;
    }

    /** Закрыть истёкшие в памяти и осиротевшие в БД (лобби / создание комнаты). */
    public void reconcile() {
        expireStale();
        closeRoom.reconcile(liveIds());
    }

    private void expireStale() {
        long now = System.currentTimeMillis();
        for (PokerRoom room : List.copyOf(rooms.values())) {
            if (room.isExpired(now)) {
                rooms.remove(room.id());
                closeRoom.execute(room.idUuid());
            }
        }
    }
}
