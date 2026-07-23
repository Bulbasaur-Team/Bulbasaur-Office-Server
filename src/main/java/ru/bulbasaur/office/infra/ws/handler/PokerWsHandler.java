package ru.bulbasaur.office.infra.ws.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.infra.ws.PokerRegistry;
import ru.bulbasaur.office.infra.ws.PokerRoom;
import ru.bulbasaur.office.infra.ws.PresenceRegistry;
import ru.bulbasaur.office.infra.ws.PresenceState;
import ru.bulbasaur.office.infra.ws.WsMessenger;
import ru.bulbasaur.office.infra.ws.dto.PokerClosedOut;
import ru.bulbasaur.office.infra.ws.dto.PokerCreateMessage;
import ru.bulbasaur.office.infra.ws.dto.PokerErrorOut;
import ru.bulbasaur.office.infra.ws.dto.PokerJoinMessage;
import ru.bulbasaur.office.infra.ws.dto.PokerRoomsOut;
import ru.bulbasaur.office.infra.ws.dto.PokerTaskMessage;
import ru.bulbasaur.office.infra.ws.dto.PokerVoteMessage;
import ru.bulbasaur.office.usecase.AchievementService;
import ru.bulbasaur.office.usecase.RecordPokerVotingUsecase;
import ru.bulbasaur.office.usecase.dto.PokerVotingResult;
import ru.bulbasaur.office.usecase.dto.RecordPokerVotingCommand;

import java.util.List;
import java.util.UUID;

/** Planning Poker: комнаты, голоса, вскрытие. */
@Component
@RequiredArgsConstructor
@Slf4j
public class PokerWsHandler {

    private final PresenceRegistry registry;
    private final PokerRegistry pokerRegistry;
    private final RecordPokerVotingUsecase recordPokerVoting;
    private final AchievementService achievements;
    private final WsMessenger messenger;

    public void onList(WebSocketSession session) {
        messenger.send(session, roomsOut());
    }

    public void onCreate(WebSocketSession session, PokerCreateMessage msg) {
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
            messenger.send(session, PokerErrorOut.of("Слишком много активных комнат, попробуйте позже."));
            return;
        }
        room.join(state.playerId(), state.login(), state.role().name(), session);
        messenger.send(session, room.stateFor(state.playerId(), System.currentTimeMillis()));
    }

    public void onJoin(WebSocketSession session, PokerJoinMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        PokerRoom room = pokerRegistry.get(msg.roomId());
        if (room == null) {
            messenger.send(session, PokerErrorOut.of("Комната уже закрыта."));
            messenger.send(session, roomsOut());
            return;
        }
        if (!room.join(state.playerId(), state.login(), state.role().name(), session)) {
            messenger.send(session, PokerErrorOut.of("Комната переполнена."));
            return;
        }
        broadcastPokerState(room);
    }

    public void onLeave(WebSocketSession session) {
        PresenceState state = registry.get(session.getId());
        if (state == null) {
            return;
        }
        onPlayerGone(state.playerId());
    }

    /** Игрок ушёл из WS — выйти из покер-комнаты, если был. */
    public void onPlayerGone(UUID playerId) {
        PokerRoom room = pokerRegistry.roomOf(playerId);
        if (room != null && room.leave(playerId)) {
            broadcastPokerState(room);
        }
    }

    public void onAddTask(WebSocketSession session, PokerTaskMessage msg) {
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

    public void onVote(WebSocketSession session, PokerVoteMessage msg) {
        PokerRoom room = pokerRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null) {
            return;
        }
        if (room.vote(state.playerId(), msg.value())) {
            achievements.grant(state.playerId(), Achievement.DEMOCRACY);
            broadcastPokerState(room);
        }
    }

    public void onFinish(WebSocketSession session) {
        PokerRoom room = pokerRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null) {
            return;
        }
        PokerRoom.FinishedVoting finished = room.finish(state.playerId());
        if (finished == null) {
            return;
        }
        if (room.isAdmin(state.playerId())) {
            achievements.grant(state.playerId(), Achievement.CROUPIER);
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

    public void onClose(WebSocketSession session) {
        PokerRoom room = pokerRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null || !room.isAdmin(state.playerId())) {
            return;
        }
        pokerRegistry.remove(room.id());
        for (PokerRoom.Participant participant : room.participantsSnapshot()) {
            messenger.send(participant.session(), PokerClosedOut.of(room.id()));
        }
    }

    private PokerRoom pokerRoomOf(WebSocketSession session) {
        PresenceState state = registry.get(session.getId());
        if (state == null) {
            return null;
        }
        PokerRoom room = pokerRegistry.roomOf(state.playerId());
        if (room == null) {
            messenger.send(session, PokerClosedOut.of(null));
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
            messenger.send(participant.session(), room.stateFor(participant.playerId(), now));
        }
    }
}
