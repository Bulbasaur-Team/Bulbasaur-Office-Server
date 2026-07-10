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
import ru.bulbasaur.office.infra.ws.dto.ItemKickMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemKickedOut;
import ru.bulbasaur.office.infra.ws.dto.ItemMoveMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemMovedOut;
import ru.bulbasaur.office.infra.ws.dto.ItemStateDto;
import ru.bulbasaur.office.infra.ws.dto.ItemsOut;
import ru.bulbasaur.office.infra.ws.dto.JoinMessage;
import ru.bulbasaur.office.infra.ws.dto.JoinedOut;
import ru.bulbasaur.office.infra.ws.dto.LeftOut;
import ru.bulbasaur.office.infra.ws.dto.MoveMessage;
import ru.bulbasaur.office.infra.ws.dto.MovedOut;
import ru.bulbasaur.office.infra.ws.dto.PlayerState;
import ru.bulbasaur.office.infra.ws.dto.PokerClosedOut;
import ru.bulbasaur.office.infra.ws.dto.PokerCreateMessage;
import ru.bulbasaur.office.infra.ws.dto.PokerErrorOut;
import ru.bulbasaur.office.infra.ws.dto.PokerJoinMessage;
import ru.bulbasaur.office.infra.ws.dto.PokerRoomsOut;
import ru.bulbasaur.office.infra.ws.dto.PokerTaskMessage;
import ru.bulbasaur.office.infra.ws.dto.PokerVoteMessage;
import ru.bulbasaur.office.infra.ws.dto.RoomMessage;
import ru.bulbasaur.office.infra.ws.dto.SnapshotOut;
import ru.bulbasaur.office.usecase.RecordPokerVotingUsecase;
import ru.bulbasaur.office.usecase.dto.PokerVotingResult;
import ru.bulbasaur.office.usecase.dto.RecordPokerVotingCommand;

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
    private final ItemRegistry itemRegistry;
    private final PokerRegistry pokerRegistry;
    private final RecordPokerVotingUsecase recordPokerVoting;
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
            case "itemKick" -> onItemKick(session, jsonMapper.treeToValue(node, ItemKickMessage.class));
            case "itemMove" -> onItemMove(session, jsonMapper.treeToValue(node, ItemMoveMessage.class));
            case "pokerList" -> onPokerList(session);
            case "pokerCreate" -> onPokerCreate(session, jsonMapper.treeToValue(node, PokerCreateMessage.class));
            case "pokerJoin" -> onPokerJoin(session, jsonMapper.treeToValue(node, PokerJoinMessage.class));
            case "pokerLeave" -> onPokerLeave(session);
            case "pokerAddTask" -> onPokerAddTask(session, jsonMapper.treeToValue(node, PokerTaskMessage.class));
            case "pokerVote" -> onPokerVote(session, jsonMapper.treeToValue(node, PokerVoteMessage.class));
            case "pokerFinish" -> onPokerFinish(session);
            case "pokerClose" -> onPokerClose(session);
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
        if (removed != null) {
            PokerRoom room = pokerRegistry.roomOf(removed.playerId());
            if (room != null && room.leave(removed.playerId())) {
                broadcastPokerState(room);
            }
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

    /**
     * Удар по предмету. Конкурентные удары разруливает {@link ItemState#tryKick}:
     * победивший рассылается всей комнате, включая ударившего (это его подтверждение),
     * проигравший молча отбрасывается — его клиент скорректируется по чужому itemKicked.
     */
    private void onItemKick(WebSocketSession session, ItemKickMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        boolean accepted = itemRegistry.tryKick(
                state.locationId(), msg.itemId(), session.getId(),
                msg.x(), msg.y(), msg.vx(), msg.vy());
        if (accepted) {
            ItemKickedOut out = ItemKickedOut.of(
                    msg.itemId(), msg.kickId(), msg.x(), msg.y(), msg.vx(), msg.vy());
            send(session, out);
            broadcast(state.locationId(), session.getId(), out);
        }
    }

    /** Репорт позиции предмета: применяем и рассылаем, только если он от владельца. */
    private void onItemMove(WebSocketSession session, ItemMoveMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        boolean accepted = itemRegistry.move(
                state.locationId(), msg.itemId(), session.getId(),
                msg.x(), msg.y(), msg.vx(), msg.vy());
        if (accepted) {
            broadcast(state.locationId(), session.getId(),
                    ItemMovedOut.of(msg.itemId(), msg.x(), msg.y(), msg.vx(), msg.vy()));
        }
    }

    private void onPokerList(WebSocketSession session) {
        send(session, roomsOut());
    }

    private void onPokerCreate(WebSocketSession session, PokerCreateMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        String name = msg.name() == null ? "" : msg.name().strip();
        if (name.isEmpty()) {
            name = "Planning Poker";
        }
        if (name.length() > 60) {
            name = name.substring(0, 60);
        }
        PokerRoom room = pokerRegistry.create(name, state.playerId(), state.login());
        if (room == null) {
            send(session, PokerErrorOut.of("Слишком много активных комнат, попробуйте позже."));
            return;
        }
        room.join(state.playerId(), state.login(), state.role().name(), session);
        send(session, room.stateFor(state.playerId(), System.currentTimeMillis()));
    }

    private void onPokerJoin(WebSocketSession session, PokerJoinMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        PokerRoom room = pokerRegistry.get(msg.roomId());
        if (room == null) {
            send(session, PokerErrorOut.of("Комната уже закрыта."));
            send(session, roomsOut());
            return;
        }
        if (!room.join(state.playerId(), state.login(), state.role().name(), session)) {
            send(session, PokerErrorOut.of("Комната переполнена."));
            return;
        }
        broadcastPokerState(room);
    }

    private void onPokerLeave(WebSocketSession session) {
        PresenceState state = registry.get(session.getId());
        if (state == null) {
            return;
        }
        PokerRoom room = pokerRegistry.roomOf(state.playerId());
        if (room != null && room.leave(state.playerId())) {
            broadcastPokerState(room);
        }
    }

    private void onPokerAddTask(WebSocketSession session, PokerTaskMessage msg) {
        PokerRoom room = pokerRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null || msg.title() == null || msg.title().isBlank()) {
            return;
        }
        String title = msg.title().strip();
        if (title.length() > 200) {
            title = title.substring(0, 200);
        }
        if (room.addTask(state.playerId(), title)) {
            broadcastPokerState(room);
        }
    }

    private void onPokerVote(WebSocketSession session, PokerVoteMessage msg) {
        PokerRoom room = pokerRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null) {
            return;
        }
        if (room.vote(state.playerId(), msg.value())) {
            broadcastPokerState(room);
        }
    }

    /**
     * Вскрытие карт: голоса замораживаются, usecase считает среднюю с рекомендацией
     * и сохраняет результат в БД. Если сохранение упало, карты всё равно вскрыты —
     * комната покажет голоса без средней.
     */
    private void onPokerFinish(WebSocketSession session) {
        PokerRoom room = pokerRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null) {
            return;
        }
        PokerRoom.FinishedVoting finished = room.finish(state.playerId());
        if (finished == null) {
            return;
        }
        try {
            PokerVotingResult result = recordPokerVoting.execute(
                    new RecordPokerVotingCommand(room.name(), finished.title(), finished.votes()));
            room.setResult(result.average(), result.recommended());
        } catch (Exception e) {
            log.error("не удалось сохранить результат покера для комнаты {}", room.id(), e);
        }
        broadcastPokerState(room);
    }

    private void onPokerClose(WebSocketSession session) {
        PokerRoom room = pokerRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null || !room.isAdmin(state.playerId())) {
            return;
        }
        pokerRegistry.remove(room.id());
        for (PokerRoom.Participant participant : room.participantsSnapshot()) {
            send(participant.session(), PokerClosedOut.of(room.id()));
        }
    }

    /** Комната игрока; null с уведомлением pokerClosed, если её уже нет (истёк TTL). */
    private PokerRoom pokerRoomOf(WebSocketSession session) {
        PresenceState state = registry.get(session.getId());
        if (state == null) {
            return null;
        }
        PokerRoom room = pokerRegistry.roomOf(state.playerId());
        if (room == null) {
            send(session, PokerClosedOut.of(null));
        }
        return room;
    }

    private PokerRoomsOut roomsOut() {
        List<PokerRoomsOut.Room> rooms = pokerRegistry.active().stream()
                .map(r -> new PokerRoomsOut.Room(r.id(), r.name(), r.adminLogin(), r.participantCount()))
                .toList();
        return PokerRoomsOut.of(rooms);
    }

    private void broadcastPokerState(PokerRoom room) {
        long now = System.currentTimeMillis();
        for (PokerRoom.Participant participant : room.participantsSnapshot()) {
            send(participant.session(), room.stateFor(participant.playerId(), now));
        }
    }

    private void sendSnapshot(WebSocketSession session, String locationId) {
        List<PlayerState> others = registry.othersInRoom(locationId, session.getId()).stream()
                .map(this::stateOf)
                .toList();
        send(session, SnapshotOut.of(others));
        List<ItemStateDto> items = itemRegistry.snapshot(locationId);
        if (!items.isEmpty()) {
            send(session, ItemsOut.of(items));
        }
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
