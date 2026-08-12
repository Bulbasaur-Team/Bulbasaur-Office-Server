package ru.bulbasaur.office.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/** Сохранённая сессия planning poker (активная или закрытая). */
@Getter
@Builder
@AllArgsConstructor
public class PokerSession {

    private final UUID id;
    private final String name;
    private final UUID adminPlayerId;
    private final PokerSessionStatus status;
    private final Instant createdAt;
    private final Instant closesAt;
    private final Instant closedAt;

    public boolean isActive() {
        return status == PokerSessionStatus.ACTIVE;
    }

    public boolean isExpired(Instant now) {
        return !closesAt.isAfter(now);
    }

    public Instant closedAtOrClosesAt() {
        return closedAt != null ? closedAt : closesAt;
    }
}
