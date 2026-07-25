package ru.bulbasaur.office.infra.ws.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.infra.persistence.entity.RetroRoomEntity;
import ru.bulbasaur.office.infra.ws.PresenceRegistry;
import ru.bulbasaur.office.infra.ws.PresenceState;
import ru.bulbasaur.office.infra.ws.RetroRegistry;
import ru.bulbasaur.office.infra.ws.RetroRoom;
import ru.bulbasaur.office.infra.ws.WsMessenger;
import ru.bulbasaur.office.infra.ws.dto.RetroAddStickerMessage;
import ru.bulbasaur.office.infra.ws.dto.RetroClosedOut;
import ru.bulbasaur.office.infra.ws.dto.RetroCreateMessage;
import ru.bulbasaur.office.infra.ws.dto.RetroDeleteMemeMessage;
import ru.bulbasaur.office.infra.ws.dto.RetroDeleteStickerMessage;
import ru.bulbasaur.office.infra.ws.dto.RetroEditStickerMessage;
import ru.bulbasaur.office.infra.ws.dto.RetroErrorOut;
import ru.bulbasaur.office.infra.ws.dto.RetroGroupStickersMessage;
import ru.bulbasaur.office.infra.ws.dto.RetroJoinMessage;
import ru.bulbasaur.office.infra.ws.dto.RetroMoodMessage;
import ru.bulbasaur.office.infra.ws.dto.RetroMoveStickerMessage;
import ru.bulbasaur.office.infra.ws.dto.RetroReactMessage;
import ru.bulbasaur.office.infra.ws.dto.RetroRoomsOut;
import ru.bulbasaur.office.infra.ws.dto.RetroStateOut;
import ru.bulbasaur.office.usecase.retro.AddRetroStickerUsecase;
import ru.bulbasaur.office.usecase.retro.BuildRetroStateUsecase;
import ru.bulbasaur.office.usecase.retro.CloseRetroRoomUsecase;
import ru.bulbasaur.office.usecase.retro.CreateRetroRoomUsecase;
import ru.bulbasaur.office.usecase.retro.DeleteRetroMemeUsecase;
import ru.bulbasaur.office.usecase.retro.DeleteRetroStickerUsecase;
import ru.bulbasaur.office.usecase.retro.EditRetroStickerUsecase;
import ru.bulbasaur.office.usecase.retro.GetRetroRoomsUsecase;
import ru.bulbasaur.office.usecase.retro.GroupRetroStickersUsecase;
import ru.bulbasaur.office.usecase.retro.MoveRetroStickerUsecase;
import ru.bulbasaur.office.usecase.retro.RetroResult;
import ru.bulbasaur.office.usecase.retro.ToggleRetroReactionUsecase;
import ru.bulbasaur.office.usecase.retro.UpsertRetroMoodUsecase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Ретроспектива: комнаты, доски, настроение, реакции. */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetroWsHandler {

    private final PresenceRegistry registry;
    private final RetroRegistry retroRegistry;
    private final WsMessenger messenger;
    private final CreateRetroRoomUsecase createRoom;
    private final CloseRetroRoomUsecase closeRoom;
    private final GetRetroRoomsUsecase getRooms;
    private final UpsertRetroMoodUsecase upsertMood;
    private final AddRetroStickerUsecase addSticker;
    private final EditRetroStickerUsecase editSticker;
    private final DeleteRetroStickerUsecase deleteSticker;
    private final GroupRetroStickersUsecase groupStickers;
    private final MoveRetroStickerUsecase moveSticker;
    private final ToggleRetroReactionUsecase toggleReaction;
    private final DeleteRetroMemeUsecase deleteMeme;
    private final BuildRetroStateUsecase buildState;

    public void onList(WebSocketSession session) {
        messenger.send(session, roomsOut());
    }

    public void onCreate(WebSocketSession session, RetroCreateMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        String name = msg.name() == null ? "" : msg.name().strip();
        if (name.isEmpty()) {
            name = "Retro WDM";
        }
        if (name.length() > 60) {
            name = name.substring(0, 60);
        }
        RetroRoomEntity entity = createRoom.execute(name, state.playerId());
        if (entity == null) {
            messenger.send(session, RetroErrorOut.of("Слишком много активных комнат, попробуйте позже."));
            return;
        }
        RetroRoom room = retroRegistry.create(entity, state.login());
        room.join(state.playerId(), state.login(), state.role().name(), session);
        messenger.send(session, stateFor(room, state.playerId()));
    }

    public void onJoin(WebSocketSession session, RetroJoinMessage msg) {
        PresenceState state = registry.get(session.getId());
        if (state == null || !state.isPlaced()) {
            return;
        }
        RetroRoom room = retroRegistry.get(msg.roomId());
        if (room == null) {
            messenger.send(session, RetroErrorOut.of("Комната уже закрыта."));
            messenger.send(session, roomsOut());
            return;
        }
        if (!room.join(state.playerId(), state.login(), state.role().name(), session)) {
            messenger.send(session, RetroErrorOut.of("Комната переполнена."));
            return;
        }
        broadcastState(room);
    }

    public void onLeave(WebSocketSession session) {
        PresenceState state = registry.get(session.getId());
        if (state == null) {
            return;
        }
        onPlayerGone(state.playerId());
    }

    public void onPlayerGone(UUID playerId) {
        RetroRoom room = retroRegistry.roomOf(playerId);
        if (room != null && room.leave(playerId)) {
            broadcastState(room);
        }
    }

    public void onClose(WebSocketSession session) {
        RetroRoom room = retroRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null || !room.isAdmin(state.playerId())) {
            return;
        }
        closeRoom.execute(room.idUuid());
        retroRegistry.remove(room.id());
        for (RetroRoom.Participant participant : room.participantsSnapshot()) {
            messenger.send(participant.session(), RetroClosedOut.of(room.id()));
        }
    }

    public void onMood(WebSocketSession session, RetroMoodMessage msg) {
        RetroRoom room = retroRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null) {
            return;
        }
        upsertMood.execute(room.idUuid(), state.playerId(), msg.value());
        broadcastState(room);
    }

    public void onAddSticker(WebSocketSession session, RetroAddStickerMessage msg) {
        RetroRoom room = retroRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null) {
            return;
        }
        sendErrOrBroadcast(session, room, addSticker.execute(room.idUuid(), msg.board(), state.playerId(), msg.text()));
    }

    public void onEditSticker(WebSocketSession session, RetroEditStickerMessage msg) {
        RetroRoom room = retroRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null || msg.stickerId() == null) {
            return;
        }
        UUID stickerId;
        try {
            stickerId = UUID.fromString(msg.stickerId());
        } catch (IllegalArgumentException e) {
            return;
        }
        sendErrOrBroadcast(session, room, editSticker.execute(stickerId, state.playerId(), msg.text()));
    }

    public void onDeleteSticker(WebSocketSession session, RetroDeleteStickerMessage msg) {
        RetroRoom room = retroRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null || msg.stickerId() == null) {
            return;
        }
        UUID stickerId;
        try {
            stickerId = UUID.fromString(msg.stickerId());
        } catch (IllegalArgumentException e) {
            return;
        }
        sendErrOrBroadcast(session, room, deleteSticker.execute(stickerId, state.playerId()));
    }

    public void onGroupStickers(WebSocketSession session, RetroGroupStickersMessage msg) {
        RetroRoom room = retroRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null || msg.stickerIds() == null) {
            return;
        }
        List<UUID> ids = new ArrayList<>();
        try {
            for (String s : msg.stickerIds()) {
                ids.add(UUID.fromString(s));
            }
        } catch (IllegalArgumentException e) {
            return;
        }
        sendErrOrBroadcast(session, room, groupStickers.execute(room.idUuid(), msg.board(), ids));
    }

    public void onMoveSticker(WebSocketSession session, RetroMoveStickerMessage msg) {
        RetroRoom room = retroRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null || msg.stickerId() == null || msg.board() == null) {
            return;
        }
        UUID stickerId;
        try {
            stickerId = UUID.fromString(msg.stickerId());
        } catch (IllegalArgumentException e) {
            return;
        }
        sendErrOrBroadcast(session, room, moveSticker.execute(
                room.idUuid(),
                msg.board(),
                stickerId,
                msg.ontoStickerId(),
                msg.ontoGroupId(),
                Boolean.TRUE.equals(msg.toBoard()),
                msg.beforeStickerId()));
    }

    public void onReact(WebSocketSession session, RetroReactMessage msg) {
        RetroRoom room = retroRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null || msg.targetId() == null) {
            return;
        }
        UUID targetId;
        try {
            targetId = UUID.fromString(msg.targetId());
        } catch (IllegalArgumentException e) {
            return;
        }
        sendErrOrBroadcast(session, room, toggleReaction.execute(
                room.idUuid(), msg.targetType(), targetId, state.playerId(), msg.emoji()));
    }

    public void onDeleteMeme(WebSocketSession session, RetroDeleteMemeMessage msg) {
        RetroRoom room = retroRoomOf(session);
        PresenceState state = registry.get(session.getId());
        if (room == null || state == null || msg.memeId() == null) {
            return;
        }
        UUID memeId;
        try {
            memeId = UUID.fromString(msg.memeId());
        } catch (IllegalArgumentException e) {
            return;
        }
        sendErrOrBroadcast(session, room, deleteMeme.execute(memeId, state.playerId()));
    }

    public void broadcastRoom(UUID roomId) {
        RetroRoom room = retroRegistry.get(roomId.toString());
        if (room != null) {
            broadcastState(room);
        }
    }

    public boolean isParticipant(UUID roomId, UUID playerId) {
        RetroRoom room = retroRegistry.get(roomId.toString());
        return room != null && room.hasParticipant(playerId);
    }

    private void sendErrOrBroadcast(WebSocketSession session, RetroRoom room, RetroResult<?> result) {
        if (!result.ok()) {
            messenger.send(session, RetroErrorOut.of(result.error()));
            return;
        }
        broadcastState(room);
    }

    private RetroRoom retroRoomOf(WebSocketSession session) {
        PresenceState state = registry.get(session.getId());
        if (state == null) {
            return null;
        }
        RetroRoom room = retroRegistry.roomOf(state.playerId());
        if (room == null) {
            messenger.send(session, RetroClosedOut.of(null));
        }
        return room;
    }

    private RetroRoomsOut roomsOut() {
        List<RetroRoomsOut.ActiveRoom> active = retroRegistry.active().stream()
                .map(r -> new RetroRoomsOut.ActiveRoom(r.id(), r.name(), r.adminLogin(), r.participantCount()))
                .toList();

        Set<String> known = active.stream().map(RetroRoomsOut.ActiveRoom::id).collect(Collectors.toSet());
        List<RetroRoomsOut.ActiveRoom> fromDb = new ArrayList<>(active);
        for (RetroRoomEntity e : getRooms.findActiveRooms()) {
            if (e.getClosesAt().toEpochMilli() <= System.currentTimeMillis()) {
                closeRoom.execute(e.getId());
                continue;
            }
            if (!known.contains(e.getId().toString())) {
                Map<UUID, String> logins = getRooms.loginMap(Set.of(e.getAdminPlayerId()));
                fromDb.add(new RetroRoomsOut.ActiveRoom(
                        e.getId().toString(),
                        e.getName(),
                        logins.getOrDefault(e.getAdminPlayerId(), "?"),
                        0));
            }
        }

        List<RetroRoomsOut.HistoryRoom> history = new ArrayList<>();
        for (RetroRoomEntity e : getRooms.findClosedRooms()) {
            Map<UUID, String> logins = getRooms.loginMap(Set.of(e.getAdminPlayerId()));
            long closedAt = e.getClosedAt() == null
                    ? e.getClosesAt().toEpochMilli()
                    : e.getClosedAt().toEpochMilli();
            history.add(new RetroRoomsOut.HistoryRoom(
                    e.getId().toString(),
                    e.getName(),
                    logins.getOrDefault(e.getAdminPlayerId(), "?"),
                    closedAt));
        }
        return RetroRoomsOut.of(fromDb, history);
    }

    private void broadcastState(RetroRoom room) {
        for (RetroRoom.Participant participant : room.participantsSnapshot()) {
            messenger.send(participant.session(), stateFor(room, participant.playerId()));
        }
    }

    private RetroStateOut stateFor(RetroRoom room, UUID viewerId) {
        RetroRoomEntity entity = getRooms.findRoom(room.idUuid()).orElse(null);
        if (entity == null) {
            return RetroStateOut.of(
                    room.id(), room.name(), room.isAdmin(viewerId), false,
                    room.remainingMs(System.currentTimeMillis()),
                    List.of(), List.of(), Map.of(), List.of());
        }
        return buildState.execute(
                entity,
                viewerId,
                room.participantsSnapshot(),
                room.remainingMs(System.currentTimeMillis()),
                false);
    }
}
