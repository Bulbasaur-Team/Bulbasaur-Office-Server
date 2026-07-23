package ru.bulbasaur.office.infra.ws.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.infra.ws.PresenceRegistry;
import ru.bulbasaur.office.infra.ws.PresenceState;
import ru.bulbasaur.office.infra.ws.ProjectorRegistry;
import ru.bulbasaur.office.infra.ws.WsMessenger;
import ru.bulbasaur.office.infra.ws.dto.ProjectorIndexMessage;
import ru.bulbasaur.office.infra.ws.dto.ProjectorOnMessage;
import ru.bulbasaur.office.infra.ws.dto.ProjectorStateOut;
import ru.bulbasaur.office.usecase.AchievementService;

/** Проектор в локации: включение, слайды, выключение. */
@Component
@RequiredArgsConstructor
public class ProjectorWsHandler {

    private final PresenceRegistry registry;
    private final ProjectorRegistry projectorRegistry;
    private final AchievementService achievements;
    private final WsMessenger messenger;

    public void onOn(WebSocketSession session, ProjectorOnMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        if (!projectorRegistry.turnOn(state.locationId(), msg.ownerId())) {
            return;
        }
        achievements.grant(state.playerId(), Achievement.SPEAKER);
        messenger.broadcastAll(state.locationId(), projectorRegistry.snapshot(state.locationId()));
    }

    public void onOff(WebSocketSession session) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        if (!projectorRegistry.turnOff(state.locationId())) {
            return;
        }
        messenger.broadcastAll(state.locationId(), ProjectorStateOut.off());
    }

    public void onIndex(WebSocketSession session, ProjectorIndexMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        if (!projectorRegistry.setIndex(state.locationId(), msg.index())) {
            return;
        }
        messenger.broadcastAll(state.locationId(), projectorRegistry.snapshot(state.locationId()));
    }
}
