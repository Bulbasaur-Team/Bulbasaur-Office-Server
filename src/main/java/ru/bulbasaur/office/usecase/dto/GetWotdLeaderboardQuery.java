package ru.bulbasaur.office.usecase.dto;

import lombok.Builder;
import ru.bulbasaur.office.domain.model.GameId;

import java.util.UUID;

@Builder
public record GetWotdLeaderboardQuery(
        GameId game,
        UUID playerId,
        String login,
        int limit) {
}
