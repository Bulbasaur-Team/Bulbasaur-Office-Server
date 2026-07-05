package ru.bulbasaur.office.infra.ws;

import lombok.Getter;
import lombok.experimental.Accessors;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.domain.model.Role;

import java.util.UUID;

/**
 * Присутствие одного подключённого игрока: неизменяемые идентификаторы сессии и
 * изменяемые роль/локация/позиция. Меняемые поля volatile — их читает поток рассылки,
 * а пишет поток обработки сообщений этой сессии.
 */
@Getter
@Accessors(fluent = true)
public class PresenceState {

    private final String sessionId;
    private final WebSocketSession session;
    private final UUID playerId;
    private final String login;

    private volatile Role role;
    private volatile String locationId;
    private volatile double x;
    private volatile double y;
    private volatile boolean facing;

    public PresenceState(WebSocketSession session, UUID playerId, String login) {
        this.sessionId = session.getId();
        this.session = session;
        this.playerId = playerId;
        this.login = login;
    }

    public void place(Role role, String locationId, double x, double y, boolean facing) {
        this.role = role;
        this.locationId = locationId;
        this.x = x;
        this.y = y;
        this.facing = facing;
    }

    public void moveTo(double x, double y, boolean facing) {
        this.x = x;
        this.y = y;
        this.facing = facing;
    }

    /** true — игрок уже прислал join и получил роль/локацию (до этого он невидим для остальных). */
    public boolean isPlaced() {
        return role != null && locationId != null;
    }
}
