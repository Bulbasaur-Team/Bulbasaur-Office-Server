package ru.bulbasaur.office.infra.ws;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.bulbasaur.office.domain.model.Emote;
import ru.bulbasaur.office.domain.model.Role;
import ru.bulbasaur.office.infra.ws.dto.ChatMessage;
import ru.bulbasaur.office.infra.ws.dto.ChatOut;
import ru.bulbasaur.office.infra.ws.dto.EmoteMessage;
import ru.bulbasaur.office.infra.ws.dto.EmoteOut;
import ru.bulbasaur.office.infra.ws.dto.JoinMessage;
import ru.bulbasaur.office.infra.ws.dto.JoinedOut;
import ru.bulbasaur.office.infra.ws.dto.LeftOut;
import ru.bulbasaur.office.infra.ws.dto.MoveMessage;
import ru.bulbasaur.office.infra.ws.dto.MovedOut;
import ru.bulbasaur.office.infra.ws.dto.PlayerState;
import ru.bulbasaur.office.infra.ws.dto.RoomMessage;
import ru.bulbasaur.office.infra.ws.dto.SnapshotOut;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Реалтайм мультиплеера поверх WebSocket. Каждое сообщение — JSON с полем type.
 * Присутствие держим в {@link PresenceRegistry}; события рассылаем только игрокам
 * в той же локации, что и отправитель.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PresenceWebSocketHandler extends TextWebSocketHandler {

    private final PresenceRegistry registry;
    private final JsonMapper jsonMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID playerId = (UUID) session.getAttributes().get(JwtHandshakeInterceptor.PLAYER_ID);
        String login = (String) session.getAttributes().get(JwtHandshakeInterceptor.LOGIN);
        registry.register(session, playerId, login);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        JsonNode node = jsonMapper.readTree(message.getPayload());
        String type = node.path("type").asString();
        switch (type) {
            case "join" -> onJoin(session, jsonMapper.treeToValue(node, JoinMessage.class));
            case "move" -> onMove(session, jsonMapper.treeToValue(node, MoveMessage.class));
            case "room" -> onRoom(session, jsonMapper.treeToValue(node, RoomMessage.class));
            case "emote" -> onEmote(session, jsonMapper.treeToValue(node, EmoteMessage.class));
            // "chat" — чат временно отключён: сообщения не обрабатываются и не рассылаются.
            default -> log.debug("неизвестный/отключённый тип WS-сообщения: {}", type);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        PresenceState removed = registry.remove(session.getId());
        if (removed != null && removed.isPlaced()) {
            broadcast(removed.locationId(), session.getId(), LeftOut.of(session.getId()));
        }
    }

    private void onJoin(WebSocketSession session, JoinMessage msg) {
        Role role = Role.fromName(msg.role()).orElse(null);
        if (role == null) {
            log.debug("join с неизвестной ролью: {}", msg.role());
            return;
        }
        registry.place(session.getId(), role, msg.locationId(), msg.x(), msg.y(), msg.facing());
        sendSnapshot(session, msg.locationId());
        broadcast(msg.locationId(), session.getId(), JoinedOut.of(stateOf(registry.get(session.getId()))));
    }

    private void onMove(WebSocketSession session, MoveMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        registry.move(session.getId(), msg.x(), msg.y(), msg.facing());
        broadcast(state.locationId(), session.getId(),
                MovedOut.of(session.getId(), msg.x(), msg.y(), msg.facing()));
    }

    private void onRoom(WebSocketSession session, RoomMessage msg) {
        String previous = registry.changeRoom(session.getId(), msg.locationId(), msg.x(), msg.y(), msg.facing());
        if (previous != null && !previous.equals(msg.locationId())) {
            broadcast(previous, session.getId(), LeftOut.of(session.getId()));
        }
        sendSnapshot(session, msg.locationId());
        broadcast(msg.locationId(), session.getId(), JoinedOut.of(stateOf(registry.get(session.getId()))));
    }

    private void onChat(WebSocketSession session, ChatMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced() || msg.text() == null || msg.text().isBlank()) {
            return;
        }
        broadcast(state.locationId(), session.getId(),
                ChatOut.of(session.getId(), state.login(), msg.text()));
    }

    private void onEmote(WebSocketSession session, EmoteMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        Emote emote = Emote.fromName(msg.emote()).orElse(null);
        if (emote == null) {
            log.debug("неизвестная реакция: {}", msg.emote());
            return;
        }
        broadcast(state.locationId(), session.getId(), EmoteOut.of(session.getId(), emote.name()));
    }

    private void sendSnapshot(WebSocketSession session, String locationId) {
        List<PlayerState> others = registry.othersInRoom(locationId, session.getId()).stream()
                .map(this::stateOf)
                .toList();
        send(session, SnapshotOut.of(others));
    }

    private void broadcast(String locationId, String exceptSessionId, Object payload) {
        for (PresenceState state : registry.othersInRoom(locationId, exceptSessionId)) {
            send(state.session(), payload);
        }
    }

    private PlayerState stateOf(PresenceState state) {
        return new PlayerState(
                state.sessionId(), state.login(), state.role().name(),
                state.x(), state.y(), state.facing());
    }

    private void send(WebSocketSession session, Object payload) {
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
}
