package ru.bulbasaur.office.infra.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.domain.model.Role;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Эфемерный реестр присутствия: кто сейчас онлайн, в какой локации и где стоит.
 * Живёт только в памяти — при отключении игрок исчезает, в БД ничего не пишется.
 */
@Component
public class PresenceRegistry {

    private final Map<String, PresenceState> sessions = new ConcurrentHashMap<>();

    public void register(WebSocketSession session, UUID playerId, String login) {
        sessions.put(session.getId(), new PresenceState(session, playerId, login));
    }

    public PresenceState remove(String sessionId) {
        return sessions.remove(sessionId);
    }

    public PresenceState get(String sessionId) {
        return sessions.get(sessionId);
    }

    /** Ставит игрока в локацию с позицией (обработка join). */
    public void place(String sessionId, Role role, String locationId, double x, double y, boolean facing) {
        PresenceState state = sessions.get(sessionId);
        if (state != null) {
            state.place(role, locationId, x, y, facing);
        }
    }

    public void move(String sessionId, double x, double y, boolean facing) {
        PresenceState state = sessions.get(sessionId);
        if (state != null) {
            state.moveTo(x, y, facing);
        }
    }

    /** Переносит игрока в другую локацию, возвращает id прежней локации (или null). */
    public String changeRoom(String sessionId, String locationId, double x, double y, boolean facing) {
        PresenceState state = sessions.get(sessionId);
        if (state == null) {
            return null;
        }
        String previous = state.locationId();
        state.place(state.role(), locationId, x, y, facing);
        return previous;
    }

    /** Игроки в указанной локации (уже вошедшие), кроме одной сессии. */
    public List<PresenceState> othersInRoom(String locationId, String exceptSessionId) {
        return sessions.values().stream()
                .filter(PresenceState::isPlaced)
                .filter(s -> locationId.equals(s.locationId()))
                .filter(s -> !s.sessionId().equals(exceptSessionId))
                .toList();
    }

    /** Все открытые сессии игрока (у одного игрока может быть несколько вкладок). */
    public List<WebSocketSession> sessionsOf(UUID playerId) {
        return sessions.values().stream()
                .filter(s -> playerId.equals(s.playerId()))
                .map(PresenceState::session)
                .toList();
    }
}
