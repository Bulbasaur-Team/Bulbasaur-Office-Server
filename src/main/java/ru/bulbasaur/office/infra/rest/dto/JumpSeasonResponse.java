package ru.bulbasaur.office.infra.rest.dto;

import java.time.Instant;
import java.util.List;

/** Текущий сезон Bulba Jump для модалки при входе в игру. */
public record JumpSeasonResponse(
        int number,
        Instant startsAt,
        Instant endsAt,
        Instant serverNow,
        List<Long> prizes,
        PreviousWinner previousWinner,
        List<LeaderboardEntryResponse> entries,
        LeaderboardEntryResponse you
) {

    public record PreviousWinner(int season, String login, long value) {
    }
}
