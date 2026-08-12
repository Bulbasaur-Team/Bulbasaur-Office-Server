package ru.bulbasaur.office.usecase.poker;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.domain.model.PokerSession;
import ru.bulbasaur.office.domain.model.PokerTask;
import ru.bulbasaur.office.domain.model.PokerVote;
import ru.bulbasaur.office.infra.ws.dto.PokerStateOut;
import ru.bulbasaur.office.usecase.exception.PokerRoomActiveException;
import ru.bulbasaur.office.usecase.exception.PokerRoomNotFoundException;
import ru.bulbasaur.office.usecase.port.out.PokerRepositoryPort;
import ru.bulbasaur.office.usecase.port.out.WardrobeRepositoryPort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Снимок закрытой покер-комнаты для просмотра истории. */
@Service
@RequiredArgsConstructor
public class BuildPokerHistoryStateUsecase {

    private final PokerRepositoryPort pokerPort;
    private final WardrobeRepositoryPort wardrobePort;

    public PokerStateOut execute(UUID roomId, UUID viewerId) {
        PokerSession session = loadClosedSession(roomId);
        List<PokerTask> tasks = pokerPort.findSessionTasks(session.getId());
        Set<UUID> playerIds = collectPlayerIds(session, tasks);
        Map<UUID, String> logins = pokerPort.loginsOf(playerIds);
        Map<UUID, PlayerAppearance> appearances = wardrobePort.appearancesOf(playerIds);
        return PokerStateOut.builder()
                .type("pokerState")
                .id(session.getId().toString())
                .name(session.getName())
                .isAdmin(session.getAdminPlayerId().equals(viewerId))
                .readOnly(true)
                .remainingMs(0)
                .myVote(null)
                .participants(toParticipants(session, tasks, logins, appearances))
                .current(null)
                .tasks(toDoneTasks(tasks, logins, appearances))
                .build();
    }

    private PokerSession loadClosedSession(UUID roomId) {
        PokerSession session = pokerPort.findSession(roomId)
                .orElseThrow(PokerRoomNotFoundException::new);
        Instant now = Instant.now();
        if (session.isActive() && session.isExpired(now)) {
            pokerPort.closeSession(roomId);
            session = pokerPort.findSession(roomId).orElse(session);
        }
        if (session.isActive()) {
            throw new PokerRoomActiveException();
        }
        return session;
    }

    private Set<UUID> collectPlayerIds(PokerSession session, List<PokerTask> tasks) {
        Set<UUID> playerIds = new HashSet<>();
        playerIds.add(session.getAdminPlayerId());
        for (PokerTask task : tasks) {
            for (PokerVote vote : task.getVotes()) {
                playerIds.add(vote.getPlayerId());
            }
        }
        return playerIds;
    }

    private List<PokerStateOut.DoneTask> toDoneTasks(
            List<PokerTask> tasks,
            Map<UUID, String> logins,
            Map<UUID, PlayerAppearance> appearances
    ) {
        List<PokerStateOut.DoneTask> done = new ArrayList<>();
        for (PokerTask task : tasks) {
            List<PokerStateOut.Vote> taskVotes = new ArrayList<>();
            for (PokerVote vote : task.getVotes()) {
                taskVotes.add(toVoteView(vote.getPlayerId(), vote.getValue(), logins, appearances));
            }
            done.add(PokerStateOut.DoneTask.builder()
                    .title(task.getTitle())
                    .average(task.getAverage())
                    .recommended(task.getRecommended())
                    .votes(taskVotes)
                    .build());
        }
        return done;
    }

    private List<PokerStateOut.Participant> toParticipants(
            PokerSession session,
            List<PokerTask> tasks,
            Map<UUID, String> logins,
            Map<UUID, PlayerAppearance> appearances
    ) {
        List<PokerStateOut.Participant> participants = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        UUID adminId = session.getAdminPlayerId();
        participants.add(PokerStateOut.Participant.builder()
                .login(loginOf(adminId, logins))
                .appearance(appearanceOf(adminId, appearances))
                .admin(true)
                .voted(false)
                .build());
        seen.add(adminId);
        for (PokerTask task : tasks) {
            for (PokerVote vote : task.getVotes()) {
                UUID playerId = vote.getPlayerId();
                if (!seen.add(playerId)) {
                    continue;
                }
                participants.add(PokerStateOut.Participant.builder()
                        .login(loginOf(playerId, logins))
                        .appearance(appearanceOf(playerId, appearances))
                        .admin(false)
                        .voted(true)
                        .build());
            }
        }
        return participants;
    }

    private PokerStateOut.Vote toVoteView(
            UUID playerId,
            String value,
            Map<UUID, String> logins,
            Map<UUID, PlayerAppearance> appearances
    ) {
        return PokerStateOut.Vote.builder()
                .login(loginOf(playerId, logins))
                .appearance(appearanceOf(playerId, appearances))
                .value(value)
                .build();
    }

    private static String loginOf(UUID playerId, Map<UUID, String> logins) {
        return logins.getOrDefault(playerId, "?");
    }

    private static PlayerAppearance appearanceOf(UUID playerId, Map<UUID, PlayerAppearance> appearances) {
        return appearances.getOrDefault(playerId, PlayerAppearance.defaults());
    }
}
