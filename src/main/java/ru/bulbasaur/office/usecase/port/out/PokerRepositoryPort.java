package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.domain.model.PokerSession;
import ru.bulbasaur.office.domain.model.PokerTask;
import ru.bulbasaur.office.usecase.dto.PokerVotingUpsert;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Персистентность planning poker: сессии, задачи, голоса. */
public interface PokerRepositoryPort {

    void save(PokerVotingUpsert upsert);

    Optional<PokerSession> findSession(UUID roomId);

    List<PokerSession> findClosedSessions();

    long countActiveSessions();

    PokerSession createSession(String name, UUID adminPlayerId, Instant closesAt);

    void closeSession(UUID roomId);

    /** Закрыть ACTIVE-сессии вне liveIds или с истёкшим TTL. */
    void closeStaleSessions(Set<UUID> liveIds, Instant now);

    List<PokerTask> findSessionTasks(UUID roomId);

    Map<UUID, String> loginsOf(Set<UUID> playerIds);
}
