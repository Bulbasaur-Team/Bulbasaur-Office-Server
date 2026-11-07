package ru.bulbasaur.office.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Один сезон Bulba Jump. {@code awardedAt} заполнен, когда призы уже выданы. */
public record JumpSeason(
        UUID id,
        int seasonNumber,
        Instant startsAt,
        Instant endsAt,
        Instant awardedAt
) {

    public boolean contains(Instant now) {
        return !now.isBefore(startsAt) && now.isBefore(endsAt);
    }

    public boolean finishedAt(Instant now) {
        return !endsAt.isAfter(now);
    }

    public JumpSeason awarded(Instant at) {
        return new JumpSeason(id, seasonNumber, startsAt, endsAt, at);
    }
}
