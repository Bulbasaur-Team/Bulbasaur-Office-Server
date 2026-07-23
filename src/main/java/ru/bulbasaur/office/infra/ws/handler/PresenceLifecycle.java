package ru.bulbasaur.office.infra.ws.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.infra.ws.AirHockeyTable;
import ru.bulbasaur.office.infra.ws.BulbaCatRegistry;
import ru.bulbasaur.office.infra.ws.PresenceRegistry;
import ru.bulbasaur.office.infra.ws.WsMessenger;
import ru.bulbasaur.office.infra.ws.dto.LeftOut;

import java.util.UUID;

/**
 * Общий cleanup при disconnect и смене локации: порядок как в старом монолите.
 */
@Component
@RequiredArgsConstructor
public class PresenceLifecycle {

    private final ItemWsHandler items;
    private final PokerWsHandler poker;
    private final AirHockeyWsHandler airHockey;
    private final CatWsHandler cat;
    private final WsMessenger messenger;
    private final PresenceRegistry registry;

    public void onDisconnect(WebSocketSession session) {
        var removed = registry.remove(session.getId());
        if (removed != null && removed.isPlaced()) {
            if (removed.heldItemId() != null) {
                items.dropToFloor(removed, session.getId(),
                        removed.heldItemId(), removed.heldItemType(), removed.x(), removed.y());
            }
            messenger.broadcast(removed.locationId(), session.getId(), LeftOut.of(session.getId()));
        }
        if (removed != null) {
            poker.onPlayerGone(removed.playerId());
            airHockey.handleLeave(removed.playerId());
            cat.clearTalkingAndBroadcast(session.getId());
        }
    }

    /** Игрок ушёл из previousLocation в другую (room). */
    public void onLeftLocation(UUID playerId, String sessionId, String previousLocationId) {
        messenger.broadcast(previousLocationId, sessionId, LeftOut.of(sessionId));
        if (playerId != null && AirHockeyTable.LOCATION_ID.equals(previousLocationId)) {
            airHockey.handleLeave(playerId);
        }
        if (BulbaCatRegistry.LOCATION_ID.equals(previousLocationId)) {
            cat.clearTalkingAndBroadcast(sessionId);
        }
    }
}
