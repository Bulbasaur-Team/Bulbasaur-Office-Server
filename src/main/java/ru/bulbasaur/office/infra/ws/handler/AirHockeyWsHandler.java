package ru.bulbasaur.office.infra.ws.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.infra.ws.AirHockeyRegistry;
import ru.bulbasaur.office.infra.ws.AirHockeySide;
import ru.bulbasaur.office.infra.ws.AirHockeyTable;
import ru.bulbasaur.office.infra.ws.PresenceRegistry;
import ru.bulbasaur.office.infra.ws.PresenceState;
import ru.bulbasaur.office.infra.ws.WsMessenger;
import ru.bulbasaur.office.infra.ws.dto.AirHockeyErrorOut;
import ru.bulbasaur.office.infra.ws.dto.AirHockeyJoinMessage;
import ru.bulbasaur.office.infra.ws.dto.AirHockeyPaddleMessage;
import ru.bulbasaur.office.infra.ws.dto.AirHockeyRematchRespondMessage;
import ru.bulbasaur.office.usecase.AchievementService;
import ru.bulbasaur.office.usecase.EventLogService;

import java.util.UUID;

/** Аэрохоккей: стол, биты, реванш и тик физики. */
@Component
@RequiredArgsConstructor
public class AirHockeyWsHandler {

    private final PresenceRegistry registry;
    private final AirHockeyRegistry airHockeyRegistry;
    private final AchievementService achievements;
    private final EventLogService eventLog;
    private final WsMessenger messenger;

    public void onJoin(WebSocketSession session, AirHockeyJoinMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        if (!AirHockeyTable.LOCATION_ID.equals(state.locationId())) {
            messenger.send(session, AirHockeyErrorOut.of("Аэрохоккей только в чилл-зоне."));
            return;
        }
        AirHockeySide side = AirHockeySide.from(msg.side()).orElse(null);
        if (side == null) {
            messenger.send(session, AirHockeyErrorOut.of("Неизвестная сторона стола."));
            return;
        }
        String[] errorOut = new String[1];
        AirHockeyTable table = airHockeyRegistry.join(
                side, state.playerId(), state.login(), session, errorOut);
        if (table == null) {
            messenger.send(session, AirHockeyErrorOut.of(errorOut[0] != null ? errorOut[0] : "Не удалось сесть за стол."));
            return;
        }
        broadcastLobby();
        if (table.phase() == AirHockeyTable.Phase.PLAYING) {
            grantHockeyPlayed(table);
            broadcastState(table);
        }
    }

    public void onLeave(WebSocketSession session) {
        PresenceState state = registry.get(session.getId());
        if (state == null) {
            return;
        }
        handleLeave(state.playerId());
    }

    public void onPaddle(WebSocketSession session, AirHockeyPaddleMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        AirHockeyTable table = airHockeyRegistry.tableOf(state.playerId());
        if (table == null) {
            return;
        }
        table.setPaddle(state.playerId(), msg.x(), msg.y());
        if (table.phase() == AirHockeyTable.Phase.PLAYING) {
            broadcastState(table);
        }
    }

    public void onRematchRequest(WebSocketSession session) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        AirHockeyTable table = airHockeyRegistry.tableOf(state.playerId());
        if (table == null) {
            return;
        }
        String error = table.requestRematch(state.playerId());
        if (error != null) {
            messenger.send(session, AirHockeyErrorOut.of(error));
            return;
        }
        broadcastState(table);
    }

    public void onRematchCancel(WebSocketSession session) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        AirHockeyTable table = airHockeyRegistry.tableOf(state.playerId());
        if (table == null) {
            return;
        }
        String error = table.cancelRematch(state.playerId());
        if (error != null) {
            messenger.send(session, AirHockeyErrorOut.of(error));
            return;
        }
        broadcastState(table);
    }

    public void onRematchRespond(WebSocketSession session, AirHockeyRematchRespondMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        AirHockeyTable table = airHockeyRegistry.tableOf(state.playerId());
        if (table == null) {
            return;
        }
        String error = table.respondRematch(state.playerId(), msg.accept());
        if (error != null) {
            messenger.send(session, AirHockeyErrorOut.of(error));
            return;
        }
        broadcastState(table);
        if (table.phase() == AirHockeyTable.Phase.PLAYING) {
            grantHockeyPlayed(table);
            broadcastLobby();
        }
    }

    public void handleLeave(UUID playerId) {
        AirHockeyTable table = airHockeyRegistry.tableOf(playerId);
        if (table == null) {
            return;
        }
        boolean wasPlaying = table.phase() == AirHockeyTable.Phase.PLAYING;
        boolean wasEnded = table.phase() == AirHockeyTable.Phase.ENDED;
        AirHockeyTable.FinishedMatch finished = table.leave(playerId);
        broadcastLobby();
        if (finished != null) {
            logMatch(table, finished);
            broadcastState(table);
            if (!table.hasConnectedSeat()) {
                table.resetAfterEnd();
                airHockeyRegistry.removeIfIdle(table);
                broadcastLobby();
            }
        } else if (wasPlaying || wasEnded) {
            broadcastState(table);
            airHockeyRegistry.removeIfIdle(table);
            if (table.phase() == AirHockeyTable.Phase.IDLE) {
                broadcastLobby();
            }
        } else {
            airHockeyRegistry.removeIfIdle(table);
        }
    }

    @Scheduled(fixedRate = 33)
    public void tickAirHockey() {
        long now = System.currentTimeMillis();
        for (AirHockeyTable table : airHockeyRegistry.all()) {
            if (table.phase() != AirHockeyTable.Phase.PLAYING) {
                continue;
            }
            AirHockeyTable.FinishedMatch finished = table.tick(0.033, now);
            broadcastState(table);
            if (finished != null) {
                logMatch(table, finished);
                broadcastLobby();
            }
        }
    }

    private void logMatch(AirHockeyTable table, AirHockeyTable.FinishedMatch finished) {
        if (!table.markLogged()) {
            return;
        }
        eventLog.airHockeyPlayed(
                finished.redLogin(),
                finished.blueLogin(),
                finished.redScore(),
                finished.blueScore(),
                finished.winnerLogin());
        grantHockeyWin(table, finished.winnerLogin());
    }

    private void grantHockeyPlayed(AirHockeyTable table) {
        AirHockeyTable.Seat red = table.red();
        AirHockeyTable.Seat blue = table.blue();
        if (red != null) {
            achievements.grant(red.playerId(), Achievement.HOCKEY);
        }
        if (blue != null) {
            achievements.grant(blue.playerId(), Achievement.HOCKEY);
        }
    }

    private void grantHockeyWin(AirHockeyTable table, String winnerLogin) {
        if (winnerLogin == null) {
            return;
        }
        AirHockeyTable.Seat red = table.red();
        AirHockeyTable.Seat blue = table.blue();
        if (red != null && winnerLogin.equals(red.login())) {
            achievements.grant(red.playerId(), Achievement.OVECHKIN);
        } else if (blue != null && winnerLogin.equals(blue.login())) {
            achievements.grant(blue.playerId(), Achievement.OVECHKIN);
        }
    }

    private void broadcastState(AirHockeyTable table) {
        long now = System.currentTimeMillis();
        AirHockeyTable.Seat red = table.red();
        AirHockeyTable.Seat blue = table.blue();
        if (red != null && red.connected()) {
            messenger.send(red.session(), table.stateFor(red.playerId(), now));
        }
        if (blue != null && blue.connected()) {
            messenger.send(blue.session(), table.stateFor(blue.playerId(), now));
        }
    }

    private void broadcastLobby() {
        messenger.broadcastAll(AirHockeyTable.LOCATION_ID, airHockeyRegistry.lobbyOut());
    }
}
