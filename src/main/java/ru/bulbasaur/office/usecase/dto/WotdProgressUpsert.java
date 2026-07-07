package ru.bulbasaur.office.usecase.dto;

import lombok.Builder;
import ru.bulbasaur.office.domain.model.GameId;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Builder
public record WotdProgressUpsert(
        UUID playerId,
        GameId game,
        LocalDate day,
        boolean solved,
        int attempts,
        List<String> guesses) {
}
