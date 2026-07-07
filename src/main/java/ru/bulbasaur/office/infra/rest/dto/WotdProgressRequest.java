package ru.bulbasaur.office.infra.rest.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record WotdProgressRequest(
        boolean solved,
        @PositiveOrZero int attempts,
        @NotNull List<String> guesses) {
}
