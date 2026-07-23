package ru.bulbasaur.office.infra.ws.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.infra.ws.ItemRegistry;
import ru.bulbasaur.office.infra.ws.PlacedItemRegistry;
import ru.bulbasaur.office.infra.ws.PresenceRegistry;
import ru.bulbasaur.office.infra.ws.PresenceState;
import ru.bulbasaur.office.infra.ws.WsMessenger;
import ru.bulbasaur.office.infra.ws.dto.ItemDropMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemDroppedOut;
import ru.bulbasaur.office.infra.ws.dto.ItemGoneMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemGrabMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemHeldOut;
import ru.bulbasaur.office.infra.ws.dto.ItemKickMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemKickedOut;
import ru.bulbasaur.office.infra.ws.dto.ItemMoveMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemMovedOut;
import ru.bulbasaur.office.infra.ws.dto.ItemPlaceMessage;
import ru.bulbasaur.office.infra.ws.dto.ItemPlacedOut;
import ru.bulbasaur.office.infra.ws.dto.ItemReleasedOut;
import ru.bulbasaur.office.infra.ws.dto.ItemRemovedOut;
import ru.bulbasaur.office.infra.ws.dto.PlacedItemDto;
import ru.bulbasaur.office.usecase.AchievementService;
import ru.bulbasaur.office.usecase.port.out.LiveMetricsPort;

import java.util.List;
import java.util.Map;

/** Физичные предметы: удары, стрим позиции, захват/бросок/стол. */
@Component
@RequiredArgsConstructor
public class ItemWsHandler {

    private static final String VOLLEYBALL_LOCATION = "vietnam-beach";
    private static final String VOLLEYBALL_ITEM_PREFIX = "volleyball";
    private static final String TENNIS_ITEM_PREFIX = "tennis";

    private final PresenceRegistry registry;
    private final ItemRegistry itemRegistry;
    private final PlacedItemRegistry placedItemRegistry;
    private final AchievementService achievements;
    private final LiveMetricsPort liveMetrics;
    private final WsMessenger messenger;

    /**
     * Удар по предмету. Конкурентные удары разруливает ItemState#tryKick:
     * победивший рассылается всей комнате, включая ударившего.
     */
    public void onKick(WebSocketSession session, ItemKickMessage msg) {
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
            messenger.send(session, out);
            messenger.broadcast(state.locationId(), session.getId(), out);
            grantKickAchievements(state, msg.itemId());
            recordKickMetric(msg.itemId());
        }
    }

    public void onMove(WebSocketSession session, ItemMoveMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        boolean accepted = itemRegistry.move(
                state.locationId(), msg.itemId(), session.getId(),
                msg.x(), msg.y(), msg.vx(), msg.vy());
        if (accepted) {
            messenger.broadcast(state.locationId(), session.getId(),
                    ItemMovedOut.of(msg.itemId(), msg.x(), msg.y(), msg.vx(), msg.vy()));
        }
    }

    public void onGrab(WebSocketSession session, ItemGrabMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced() || msg.itemId() == null || msg.itemType() == null) {
            return;
        }
        state.hold(msg.itemId(), msg.itemType());
        boolean fromTable = placedItemRegistry.remove(state.locationId(), msg.itemId());
        if (fromTable) {
            messenger.broadcast(state.locationId(), session.getId(), ItemRemovedOut.of(msg.itemId()));
        } else if ("coffee".equals(msg.itemType()) && msg.itemId().startsWith("coffee")) {
            liveMetrics.recordCoffeeCup();
        }
        if ("coffee".equals(msg.itemType())) {
            achievements.grant(state.playerId(), Achievement.COFFEEMAN);
        }
        messenger.broadcast(state.locationId(), session.getId(),
                ItemHeldOut.of(session.getId(), msg.itemId(), msg.itemType()));
    }

    public void onDrop(WebSocketSession session, ItemDropMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        state.dropHeld();
        dropToFloor(state, session.getId(), msg.itemId(), msg.itemType(), msg.x(), msg.y());
        messenger.broadcast(state.locationId(), session.getId(), ItemReleasedOut.of(session.getId()));
    }

    public void onPlace(WebSocketSession session, ItemPlaceMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        state.dropHeld();
        messenger.broadcast(state.locationId(), session.getId(), ItemReleasedOut.of(session.getId()));

        long expiresAt = placedItemRegistry.expiryOf(msg.itemId());
        boolean placed = placedItemRegistry.place(
                state.locationId(), msg.itemId(), msg.itemType(), msg.tableIndex(), msg.x(), msg.y(), expiresAt);
        if (!placed) {
            return;
        }
        PlacedItemDto item = new PlacedItemDto(
                msg.itemId(), msg.itemType(), msg.tableIndex(), msg.x(), msg.y(), expiresAt);
        messenger.broadcast(state.locationId(), session.getId(), ItemPlacedOut.of(item));
    }

    public void onGone(WebSocketSession session, ItemGoneMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        state.dropHeld();
        placedItemRegistry.removeEverywhere(msg.itemId());
        messenger.broadcast(state.locationId(), session.getId(), ItemReleasedOut.of(session.getId()));
    }

    /**
     * Предмет ложится на пол локации и рассылается остальным.
     * Нужен также при disconnect игрока с предметом в лапах.
     */
    public void dropToFloor(PresenceState state, String exceptSessionId,
                            String itemId, String itemType, double x, double y) {
        if (itemRegistry.rest(state.locationId(), itemId, x, y)) {
            messenger.broadcast(state.locationId(), exceptSessionId, ItemDroppedOut.of(itemId, itemType, x, y));
        }
    }

    @Scheduled(fixedRate = 30_000)
    public void sweepExpiredPlacedItems() {
        for (Map.Entry<String, List<String>> entry : placedItemRegistry.sweepExpired().entrySet()) {
            for (String itemId : entry.getValue()) {
                messenger.broadcast(entry.getKey(), null, ItemRemovedOut.of(itemId));
            }
        }
    }

    private void grantKickAchievements(PresenceState state, String itemId) {
        if (itemId == null) {
            return;
        }
        if (VOLLEYBALL_LOCATION.equals(state.locationId()) && itemId.startsWith(VOLLEYBALL_ITEM_PREFIX)) {
            achievements.grant(state.playerId(), Achievement.VOLLEYBALL);
        }
        if (itemId.startsWith(TENNIS_ITEM_PREFIX)) {
            achievements.grant(state.playerId(), Achievement.TENNIS);
        }
    }

    private void recordKickMetric(String itemId) {
        if (itemId == null) {
            return;
        }
        if (itemId.startsWith(TENNIS_ITEM_PREFIX)) {
            liveMetrics.recordTennisKick();
        } else if (itemId.startsWith(VOLLEYBALL_ITEM_PREFIX)) {
            liveMetrics.recordVolleyballKick();
        }
    }
}
