package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.domain.model.PokerSession;
import ru.bulbasaur.office.domain.model.PokerSessionStatus;
import ru.bulbasaur.office.domain.model.PokerTask;
import ru.bulbasaur.office.domain.model.PokerVote;
import ru.bulbasaur.office.infra.persistence.entity.PlayerEntity;
import ru.bulbasaur.office.infra.persistence.entity.PokerRoomEntity;
import ru.bulbasaur.office.infra.persistence.entity.PokerTaskEntity;
import ru.bulbasaur.office.infra.persistence.entity.PokerVoteEntity;
import ru.bulbasaur.office.infra.persistence.repository.PlayerJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.PokerRoomJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.PokerTaskJpaRepository;
import ru.bulbasaur.office.infra.persistence.repository.PokerVoteJpaRepository;
import ru.bulbasaur.office.usecase.dto.PokerVoteRecord;
import ru.bulbasaur.office.usecase.dto.PokerVotingUpsert;
import ru.bulbasaur.office.usecase.port.out.PokerRepositoryPort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PokerResultConnector implements PokerRepositoryPort {

    private final PokerRoomJpaRepository rooms;
    private final PokerTaskJpaRepository tasks;
    private final PokerVoteJpaRepository votes;
    private final PlayerJpaRepository players;

    @Override
    @Transactional
    public void save(PokerVotingUpsert upsert) {
        PokerTaskEntity task = new PokerTaskEntity();
        task.setId(UUID.randomUUID());
        task.setRoomId(upsert.roomId());
        task.setRoomName(upsert.roomName());
        task.setTitle(upsert.taskTitle());
        task.setAverage(upsert.average());
        task.setRecommended(upsert.recommended());
        task.setCreatedAt(Instant.now());
        tasks.save(task);

        for (PokerVoteRecord record : upsert.votes()) {
            PokerVoteEntity vote = new PokerVoteEntity();
            vote.setId(UUID.randomUUID());
            vote.setTask(task);
            PlayerEntity playerRef = new PlayerEntity();
            playerRef.setId(record.playerId());
            vote.setPlayer(playerRef);
            vote.setValue(record.value());
            votes.save(vote);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PokerSession> findSession(UUID roomId) {
        return rooms.findById(roomId).map(this::toSession);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PokerSession> findClosedSessions() {
        return rooms.findByStatusOrderByCreatedAtDesc(PokerRoomEntity.STATUS_CLOSED).stream()
                .map(this::toSession)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveSessions() {
        return rooms.countByStatus(PokerRoomEntity.STATUS_ACTIVE);
    }

    @Override
    @Transactional
    public PokerSession createSession(String name, UUID adminPlayerId, Instant closesAt) {
        Instant now = Instant.now();
        PokerRoomEntity entity = new PokerRoomEntity();
        entity.setId(UUID.randomUUID());
        entity.setName(name);
        entity.setAdminPlayerId(adminPlayerId);
        entity.setStatus(PokerRoomEntity.STATUS_ACTIVE);
        entity.setCreatedAt(now);
        entity.setClosesAt(closesAt);
        return toSession(rooms.save(entity));
    }

    @Override
    @Transactional
    public void closeSession(UUID roomId) {
        rooms.findById(roomId).ifPresent(this::close);
    }

    @Override
    @Transactional
    public void closeStaleSessions(Set<UUID> liveIds, Instant now) {
        for (PokerRoomEntity room : rooms.findByStatusOrderByCreatedAtDesc(PokerRoomEntity.STATUS_ACTIVE)) {
            boolean live = liveIds.contains(room.getId());
            boolean expired = !room.getClosesAt().isAfter(now);
            if (!live || expired) {
                close(room);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PokerTask> findSessionTasks(UUID roomId) {
        List<PokerTaskEntity> taskRows = tasks.findByRoomIdOrderByCreatedAtAsc(roomId);
        List<UUID> taskIds = taskRows.stream().map(PokerTaskEntity::getId).toList();
        List<PokerVoteEntity> voteRows = taskIds.isEmpty()
                ? List.of()
                : votes.findWithPlayerByTaskIdIn(taskIds);
        Map<UUID, List<PokerVoteEntity>> votesByTask = voteRows.stream()
                .collect(Collectors.groupingBy(v -> v.getTask().getId(), LinkedHashMap::new, Collectors.toList()));

        List<PokerTask> result = new ArrayList<>();
        for (PokerTaskEntity task : taskRows) {
            List<PokerVote> taskVotes = new ArrayList<>();
            for (PokerVoteEntity vote : votesByTask.getOrDefault(task.getId(), List.of())) {
                taskVotes.add(PokerVote.builder()
                        .playerId(vote.getPlayer().getId())
                        .value(vote.getValue())
                        .build());
            }
            result.add(PokerTask.builder()
                    .id(task.getId())
                    .title(task.getTitle())
                    .average(task.getAverage())
                    .recommended(task.getRecommended())
                    .votes(taskVotes)
                    .build());
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> loginsOf(Set<UUID> playerIds) {
        if (playerIds.isEmpty()) {
            return Map.of();
        }
        return players.findAllById(playerIds).stream()
                .collect(Collectors.toMap(PlayerEntity::getId, PlayerEntity::getLogin));
    }

    private void close(PokerRoomEntity room) {
        if (PokerRoomEntity.STATUS_CLOSED.equals(room.getStatus())) {
            return;
        }
        room.setStatus(PokerRoomEntity.STATUS_CLOSED);
        room.setClosedAt(Instant.now());
        rooms.save(room);
    }

    private PokerSession toSession(PokerRoomEntity entity) {
        return PokerSession.builder()
                .id(entity.getId())
                .name(entity.getName())
                .adminPlayerId(entity.getAdminPlayerId())
                .status(PokerSessionStatus.valueOf(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .closesAt(entity.getClosesAt())
                .closedAt(entity.getClosedAt())
                .build();
    }
}
