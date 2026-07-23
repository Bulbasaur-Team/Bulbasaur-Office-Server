package ru.bulbasaur.office.infra.ws.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.domain.model.Emote;
import ru.bulbasaur.office.domain.model.Role;
import ru.bulbasaur.office.infra.ws.AirHockeyRegistry;
import ru.bulbasaur.office.infra.ws.AirHockeyTable;
import ru.bulbasaur.office.infra.ws.BulbaCatRegistry;
import ru.bulbasaur.office.infra.ws.ItemRegistry;
import ru.bulbasaur.office.infra.ws.PlacedItemRegistry;
import ru.bulbasaur.office.infra.ws.PresenceRegistry;
import ru.bulbasaur.office.infra.ws.PresenceState;
import ru.bulbasaur.office.infra.ws.ProjectorRegistry;
import ru.bulbasaur.office.infra.ws.WsMessenger;
import ru.bulbasaur.office.infra.ws.dto.ChatMessage;
import ru.bulbasaur.office.infra.ws.dto.ChatOut;
import ru.bulbasaur.office.infra.ws.dto.EmoteMessage;
import ru.bulbasaur.office.infra.ws.dto.EmoteOut;
import ru.bulbasaur.office.infra.ws.dto.ItemStateDto;
import ru.bulbasaur.office.infra.ws.dto.ItemsOut;
import ru.bulbasaur.office.infra.ws.dto.JoinMessage;
import ru.bulbasaur.office.infra.ws.dto.JoinedOut;
import ru.bulbasaur.office.infra.ws.dto.MoveMessage;
import ru.bulbasaur.office.infra.ws.dto.MovedOut;
import ru.bulbasaur.office.infra.ws.dto.PlacedItemsOut;
import ru.bulbasaur.office.infra.ws.dto.PlayerState;
import ru.bulbasaur.office.infra.ws.dto.RoomMessage;
import ru.bulbasaur.office.infra.ws.dto.SnapshotOut;
import ru.bulbasaur.office.usecase.AchievementService;

import java.util.List;
import java.util.UUID;

/** Присутствие: join / move / room / emote и снапшот комнаты. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceWsHandler {

    private final PresenceRegistry registry;
    private final ItemRegistry itemRegistry;
    private final PlacedItemRegistry placedItemRegistry;
    private final ProjectorRegistry projectorRegistry;
    private final AirHockeyRegistry airHockeyRegistry;
    private final BulbaCatRegistry bulbaCatRegistry;
    private final AchievementService achievements;
    private final WsMessenger messenger;
    private final PresenceLifecycle lifecycle;

    public void onJoin(WebSocketSession session, JoinMessage msg) {
        Role role = Role.fromName(msg.role()).orElse(null);
        if (role == null) {
            log.debug("join с неизвестной ролью: {}", msg.role());
            return;
        }
        registry.place(session.getId(), role, msg.locationId(), msg.x(), msg.y(), msg.facing());
        sendSnapshot(session, msg.locationId());
        messenger.broadcast(msg.locationId(), session.getId(),
                JoinedOut.of(messenger.stateOf(registry.get(session.getId()))));
    }

    public void onMove(WebSocketSession session, MoveMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        registry.move(session.getId(), msg.x(), msg.y(), msg.facing());
        messenger.broadcast(state.locationId(), session.getId(),
                MovedOut.of(session.getId(), msg.x(), msg.y(), msg.facing()));
    }

    public void onRoom(WebSocketSession session, RoomMessage msg) {
        PresenceState before = registry.get(session.getId());
        UUID playerId = before != null ? before.playerId() : null;
        String previous = registry.changeRoom(session.getId(), msg.locationId(), msg.x(), msg.y(), msg.facing());
        if (previous != null && !previous.equals(msg.locationId())) {
            lifecycle.onLeftLocation(playerId, session.getId(), previous);
        }
        sendSnapshot(session, msg.locationId());
        messenger.broadcast(msg.locationId(), session.getId(),
                JoinedOut.of(messenger.stateOf(registry.get(session.getId()))));
    }

    public void onChat(WebSocketSession session, ChatMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced() || msg.text() == null || msg.text().isBlank()) {
            return;
        }
        messenger.broadcast(state.locationId(), session.getId(),
                ChatOut.of(session.getId(), state.login(), msg.text()));
    }

    public void onEmote(WebSocketSession session, EmoteMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        Emote emote = Emote.fromName(msg.emote()).orElse(null);
        if (emote == null) {
            log.debug("неизвестная реакция: {}", msg.emote());
            return;
        }
        messenger.broadcast(state.locationId(), session.getId(), EmoteOut.of(session.getId(), emote.name()));
        if (emote == Emote.HEART) {
            achievements.grant(state.playerId(), Achievement.LOVER);
        }
    }

    private void sendSnapshot(WebSocketSession session, String locationId) {
        List<PlayerState> others = registry.othersInRoom(locationId, session.getId()).stream()
                .map(messenger::stateOf)
                .toList();
        messenger.send(session, SnapshotOut.of(others));
        List<ItemStateDto> items = itemRegistry.snapshot(locationId);
        if (!items.isEmpty()) {
            messenger.send(session, ItemsOut.of(items));
        }
        messenger.send(session, PlacedItemsOut.of(placedItemRegistry.snapshot(locationId)));
        messenger.send(session, projectorRegistry.snapshot(locationId));
        if (AirHockeyTable.LOCATION_ID.equals(locationId)) {
            messenger.send(session, airHockeyRegistry.lobbyOut());
        }
        if (BulbaCatRegistry.LOCATION_ID.equals(locationId)) {
            messenger.send(session, bulbaCatRegistry.snapshot());
        }
    }
}
