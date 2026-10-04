package ru.bulbasaur.office.infra.ws;

import org.springframework.web.socket.WebSocketSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Активная ретро-комната в памяти: сессии участников и метаданные.
 * Контент (стикеры, мемы, настроение) хранится в БД.
 */
public class RetroRoom {

    public static final long TTL_MS = 2 * 60 * 60 * 1000L;
    public static final int MAX_PARTICIPANTS = 30;

    public record Participant(UUID playerId, String login, String role, WebSocketSession session) {
    }

    private final String id;
    private final String name;
    private final UUID adminPlayerId;
    private final String adminLogin;
    private final long closesAtMillis;
    /** 0 — таймер ведущего не запущен. */
    private long timerEndsAtMillis;

    private final Map<UUID, Participant> participants = new LinkedHashMap<>();

    public RetroRoom(String id, String name, UUID adminPlayerId, String adminLogin, long closesAtMillis) {
        this.id = id;
        this.name = name;
        this.adminPlayerId = adminPlayerId;
        this.adminLogin = adminLogin;
        this.closesAtMillis = closesAtMillis;
    }

    public String id() {
        return id;
    }

    public UUID idUuid() {
        return UUID.fromString(id);
    }

    public String name() {
        return name;
    }

    public UUID adminPlayerId() {
        return adminPlayerId;
    }

    public String adminLogin() {
        return adminLogin;
    }

    public long closesAtMillis() {
        return closesAtMillis;
    }

    public boolean isExpired(long nowMillis) {
        return nowMillis >= closesAtMillis;
    }

    public boolean isAdmin(UUID playerId) {
        return adminPlayerId.equals(playerId);
    }

    public synchronized int participantCount() {
        return participants.size();
    }

    public synchronized boolean hasParticipant(UUID playerId) {
        return participants.containsKey(playerId);
    }

    public synchronized boolean join(UUID playerId, String login, String role, WebSocketSession session) {
        if (!participants.containsKey(playerId) && participants.size() >= MAX_PARTICIPANTS) {
            return false;
        }
        participants.put(playerId, new Participant(playerId, login, role, session));
        return true;
    }

    public synchronized boolean leave(UUID playerId) {
        return participants.remove(playerId) != null;
    }

    public synchronized List<Participant> participantsSnapshot() {
        return List.copyOf(participants.values());
    }

    public long remainingMs(long nowMillis) {
        return Math.max(0, closesAtMillis - nowMillis);
    }

    public synchronized void startTimer(long durationMs, long nowMillis) {
        timerEndsAtMillis = nowMillis + durationMs;
    }

    public synchronized void stopTimer() {
        timerEndsAtMillis = 0;
    }

    public synchronized long timerRemainingMs(long nowMillis) {
        if (timerEndsAtMillis == 0) {
            return 0;
        }
        long left = timerEndsAtMillis - nowMillis;
        if (left <= 0) {
            timerEndsAtMillis = 0;
            return 0;
        }
        return left;
    }
}
