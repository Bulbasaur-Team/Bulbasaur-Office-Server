package ru.bulbasaur.office.usecase.dto;

import lombok.Builder;
import ru.bulbasaur.office.domain.model.GameId;

import java.util.List;
import java.util.UUID;

@Builder
public record SaveWotdProgressCommand(
        UUID playerId,
        GameId game,
        boolean solved,
        int attempts,
        List<String> guesses) {
}
