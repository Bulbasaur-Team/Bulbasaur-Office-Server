package ru.bulbasaur.office.infra.ws;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр покер-комнат. Эфемерный, как {@link PresenceRegistry}: комнаты живут в
 * памяти, в БД уходят только результаты голосований. Истёкшие комнаты (TTL 2 часа)
 * вычищаются лениво — при любом обращении; открытые клиенты закрывают их сами по
 * таймеру, который сервер отдаёт в каждом pokerState (remainingMs).
 */
@Component
public class PokerRegistry {

    private static final int MAX_ROOMS = 20;

    private final Map<String, PokerRoom> rooms = new ConcurrentHashMap<>();

    /** Активные комнаты для лобби (истёкшие по пути удаляются). */
    public List<PokerRoom> active() {
        long now = System.currentTimeMillis();
        rooms.values().removeIf(room -> room.isExpired(now));
        return rooms.values().stream()
                .sorted(Comparator.comparing(PokerRoom::name))
                .toList();
    }

    public PokerRoom create(String name, UUID adminPlayerId, String adminLogin) {
        if (active().size() >= MAX_ROOMS) {
            return null;
        }
        PokerRoom room = new PokerRoom(
                UUID.randomUUID().toString(), name, adminPlayerId, adminLogin, System.currentTimeMillis());
        rooms.put(room.id(), room);
        return room;
    }

    public PokerRoom get(String roomId) {
        if (roomId == null) {
            return null;
        }
        PokerRoom room = rooms.get(roomId);
        if (room != null && room.isExpired(System.currentTimeMillis())) {
            rooms.remove(roomId);
            return null;
        }
        return room;
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
}
