package ru.bulbasaur.office.infra.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.infra.ws.dto.PlayerState;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Общая отправка WS-сообщений: сериализация JSON и рассылка по локации.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WsMessenger {

    private final PresenceRegistry registry;
    private final JsonMapper jsonMapper;

    public void send(WebSocketSession session, Object payload) {
        if (!session.isOpen()) {
            return;
        }
        try {
            TextMessage message = new TextMessage(jsonMapper.writeValueAsString(payload));
            // Отправка по одной сессии не потокобезопасна — сериализуем доступ к ней.
            synchronized (session) {
                session.sendMessage(message);
            }
        } catch (IOException e) {
            log.debug("не удалось отправить WS-сообщение сессии {}: {}", session.getId(), e.getMessage());
        }
    }

    /** Рассылка всем в локации, включая отправителя (общее состояние объекта комнаты). */
    public void broadcastAll(String locationId, Object payload) {
        for (PresenceState state : registry.othersInRoom(locationId, null)) {
            send(state.session(), payload);
        }
    }

    public void broadcast(String locationId, String exceptSessionId, Object payload) {
        for (PresenceState state : registry.othersInRoom(locationId, exceptSessionId)) {
            send(state.session(), payload);
        }
    }

    public PlayerState stateOf(PresenceState state) {
        return new PlayerState(
                state.sessionId(), state.login(), state.role().name(),
                state.x(), state.y(), state.facing(),
                state.heldItemId(), state.heldItemType());
    }
}
