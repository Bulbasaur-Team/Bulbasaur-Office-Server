package ru.bulbasaur.office.infra.ws;

import org.springframework.web.socket.WebSocketSession;
import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.usecase.dto.PokerVoteRecord;
import ru.bulbasaur.office.infra.ws.dto.PokerStateOut;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Комната planning poker. Живёт в памяти до закрытия админом или истечения TTL.
 * Участник идентифицируется playerId (постоянный id аккаунта): при реконнекте он
 * возвращается в комнату тем же участником, а создатель — снова админом.
 * Все изменения — под монитором объекта: голосуют и входят конкурентно.
 */
public class PokerRoom {

    public static final long TTL_MS = 2 * 60 * 60 * 1000L;

    private static final Set<String> CARD_VALUES = Set.of("0", "1", "2", "3", "5", "8", "13", "?", "coffee");
    private static final int MAX_PARTICIPANTS = 30;

    /** Участник: сессия обновляется при повторном входе (реконнект). */
    public record Participant(UUID playerId, String login, PlayerAppearance appearance, WebSocketSession session) {
    }

    /** Итог завершённого голосования — для сохранения в БД. */
    public record FinishedVoting(String title, List<PokerVoteRecord> votes) {
    }

    private final String id;
    private final String name;
    private final UUID adminPlayerId;
    private final String adminLogin;
    private final long closesAtMillis;

    private final Map<UUID, Participant> participants = new LinkedHashMap<>();
    private final List<PokerStateOut.DoneTask> done = new ArrayList<>();

    private String currentTitle;
    private final Map<UUID, String> votes = new LinkedHashMap<>();
    private boolean revealed;
    private List<PokerStateOut.Vote> revealedVotes = List.of();
    private Double average;
    private Integer recommended;

    public PokerRoom(String id, String name, UUID adminPlayerId, String adminLogin, long closesAtMillis) {
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

    public String adminLogin() {
        return adminLogin;
    }

    public boolean isExpired(long nowMillis) {
        return nowMillis >= closesAtMillis;
    }

    public synchronized int participantCount() {
        return participants.size();
    }

    public synchronized boolean hasParticipant(UUID playerId) {
        return participants.containsKey(playerId);
    }

    public synchronized boolean isAdmin(UUID playerId) {
        return adminPlayerId.equals(playerId);
    }

    public synchronized boolean join(UUID playerId, String login, PlayerAppearance appearance, WebSocketSession session) {
        if (!participants.containsKey(playerId) && participants.size() >= MAX_PARTICIPANTS) {
            return false;
        }
        PlayerAppearance look = appearance != null ? appearance : PlayerAppearance.defaults();
        participants.put(playerId, new Participant(playerId, login, look, session));
        return true;
    }

    /** Выход участника; его голос в незавершённом голосовании снимается. */
    public synchronized boolean leave(UUID playerId) {
        if (participants.remove(playerId) == null) {
            return false;
        }
        if (!revealed) {
            votes.remove(playerId);
        }
        return true;
    }

    public synchronized boolean vote(UUID playerId, String value) {
        if (currentTitle == null || revealed || !participants.containsKey(playerId)
                || !CARD_VALUES.contains(value)) {
            return false;
        }
        votes.put(playerId, value);
        return true;
    }

    /** Новая задача (только админ, предыдущее голосование должно быть вскрыто). */
    public synchronized boolean addTask(UUID playerId, String title) {
        if (!adminPlayerId.equals(playerId) || (currentTitle != null && !revealed)) {
            return false;
        }
        if (currentTitle != null) {
            done.add(PokerStateOut.DoneTask.builder()
                    .title(currentTitle)
                    .average(average)
                    .recommended(recommended)
                    .votes(List.of())
                    .build());
        }
        currentTitle = title;
        votes.clear();
        revealed = false;
        revealedVotes = List.of();
        average = null;
        recommended = null;
        return true;
    }

    /**
     * Переголосование по текущей задаче (только админ, карты уже вскрыты).
     * Итог сбрасывается, голоса очищаются, задача остаётся той же.
     */
    public synchronized boolean revote(UUID playerId) {
        if (!adminPlayerId.equals(playerId) || currentTitle == null || !revealed) {
            return false;
        }
        votes.clear();
        revealed = false;
        revealedVotes = List.of();
        average = null;
        recommended = null;
        return true;
    }

    /**
     * Вскрытие карт (только админ). Голоса замораживаются и материализуются со
     * снимком логина/внешности — участник, вышедший позже, не сотрёт свой вскрытый голос.
     */
    public synchronized FinishedVoting finish(UUID playerId) {
        if (!adminPlayerId.equals(playerId) || currentTitle == null || revealed) {
            return null;
        }
        revealed = true;
        List<PokerStateOut.Vote> views = new ArrayList<>();
        List<PokerVoteRecord> records = new ArrayList<>();
        for (Map.Entry<UUID, String> vote : votes.entrySet()) {
            Participant participant = participants.get(vote.getKey());
            if (participant != null) {
                views.add(PokerStateOut.Vote.builder()
                        .login(participant.login())
                        .appearance(participant.appearance())
                        .value(vote.getValue())
                        .build());
            }
            records.add(new PokerVoteRecord(vote.getKey(), vote.getValue()));
        }
        revealedVotes = views;
        return new FinishedVoting(currentTitle, records);
    }

    /** Итог от usecase после сохранения — показывается всем в комнате. */
    public synchronized void setResult(Double average, Integer recommended) {
        this.average = average;
        this.recommended = recommended;
    }

    public synchronized List<Participant> participantsSnapshot() {
        return List.copyOf(participants.values());
    }

    public synchronized PokerStateOut stateFor(UUID playerId, long nowMillis) {
        List<PokerStateOut.Participant> list = new ArrayList<>();
        for (Participant p : participants.values()) {
            list.add(PokerStateOut.Participant.builder()
                    .login(p.login())
                    .appearance(p.appearance())
                    .admin(adminPlayerId.equals(p.playerId()))
                    .voted(votes.containsKey(p.playerId()))
                    .build());
        }
        PokerStateOut.Current current = currentTitle == null ? null
                : PokerStateOut.Current.builder()
                        .title(currentTitle)
                        .revealed(revealed)
                        .average(average)
                        .recommended(recommended)
                        .votes(revealedVotes)
                        .build();
        return PokerStateOut.builder()
                .type("pokerState")
                .id(id)
                .name(name)
                .isAdmin(adminPlayerId.equals(playerId))
                .readOnly(false)
                .remainingMs(Math.max(0, closesAtMillis - nowMillis))
                .myVote(votes.get(playerId))
                .participants(list)
                .current(current)
                .tasks(List.copyOf(done))
                .build();
    }
}
