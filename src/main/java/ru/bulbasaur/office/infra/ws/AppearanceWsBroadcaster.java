package ru.bulbasaur.office.infra.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.infra.ws.dto.AppearanceOut;
import ru.bulbasaur.office.usecase.AppearanceBroadcaster;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppearanceWsBroadcaster implements AppearanceBroadcaster {

    private final PresenceRegistry registry;
    private final WsMessenger messenger;

    @Override
    public void broadcast(UUID playerId, PlayerAppearance appearance) {
        for (PresenceState state : registry.statesOf(playerId)) {
            if (!state.isPlaced()) {
                continue;
            }
            state.setAppearance(appearance);
            messenger.broadcastAll(state.locationId(), AppearanceOut.of(state.sessionId(), appearance));
        }
    }
}
