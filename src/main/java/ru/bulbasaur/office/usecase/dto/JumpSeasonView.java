package ru.bulbasaur.office.usecase.dto;

import java.time.Instant;
import java.util.List;

/** Текущий сезон Bulba Jump: сроки, призы, прошлый победитель и таблица. */
public record JumpSeasonView(
        int number,
        Instant startsAt,
        Instant endsAt,
        Instant serverNow,
        List<Long> prizes,
        PreviousWinner previousWinner,
        List<LeaderboardRowView> top,
        LeaderboardRowView me
) {

    public record PreviousWinner(int season, String login, long value) {
    }
}
