package ru.bulbasaur.office.infra.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomEntity;
import ru.bulbasaur.office.usecase.retro.CloseRetroRoomUsecase;
import ru.bulbasaur.office.usecase.retro.GetRetroRoomsUsecase;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Реестр активных ретро-комнат (сессии участников в памяти).
 * Истёкшие комнаты закрываются в БД лениво при обращении.
 */
@Component
@RequiredArgsConstructor
public class RetroRegistry {

    private final GetRetroRoomsUsecase getRooms;
    private final CloseRetroRoomUsecase closeRoom;
    private final Map<String, RetroRoom> rooms = new ConcurrentHashMap<>();

    public List<RetroRoom> active() {
        expireStale();
        return rooms.values().stream()
                .sorted(Comparator.comparing(RetroRoom::name))
                .toList();
    }

    public RetroRoom create(RetroRoomEntity entity, String adminLogin) {
        RetroRoom room = new RetroRoom(
                entity.getId().toString(),
                entity.getName(),
                entity.getAdminPlayerId(),
                adminLogin,
                entity.getClosesAt().toEpochMilli());
        rooms.put(room.id(), room);
        return room;
    }

    /** Поднять комнату из БД в память, если ещё ACTIVE. */
    public RetroRoom ensureLoaded(UUID roomId) {
        expireStale();
        RetroRoom existing = rooms.get(roomId.toString());
        if (existing != null) {
            return existing;
        }
        return getRooms.findRoom(roomId)
                .filter(e -> RetroRoomEntity.STATUS_ACTIVE.equals(e.getStatus()))
                .filter(e -> e.getClosesAt().toEpochMilli() > System.currentTimeMillis())
                .map(e -> {
                    String adminLogin = getRooms.loginMap(java.util.Set.of(e.getAdminPlayerId()))
                            .getOrDefault(e.getAdminPlayerId(), "?");
                    RetroRoom room = new RetroRoom(
                            e.getId().toString(),
                            e.getName(),
                            e.getAdminPlayerId(),
                            adminLogin,
                            e.getClosesAt().toEpochMilli());
                    rooms.put(room.id(), room);
                    return room;
                })
                .orElse(null);
    }

    public RetroRoom get(String roomId) {
        if (roomId == null) {
            return null;
        }
        expireStale();
        RetroRoom room = rooms.get(roomId);
        if (room != null && room.isExpired(System.currentTimeMillis())) {
            closeExpired(room);
            return null;
        }
        if (room == null) {
            try {
                return ensureLoaded(UUID.fromString(roomId));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return room;
    }

    public RetroRoom roomOf(UUID playerId) {
        for (RetroRoom room : active()) {
            if (room.hasParticipant(playerId)) {
                return room;
            }
        }
        return null;
    }

    public void remove(String roomId) {
        rooms.remove(roomId);
    }

    private void expireStale() {
        long now = System.currentTimeMillis();
        for (RetroRoom room : List.copyOf(rooms.values())) {
            if (room.isExpired(now)) {
                closeExpired(room);
            }
        }
    }

    private void closeExpired(RetroRoom room) {
        rooms.remove(room.id());
        closeRoom.execute(room.idUuid());
    }
}
