package ru.bulbasaur.office.infra.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record QuizBoosterRequest(
        @NotNull UUID attemptId,
        @NotBlank String type
) {
}
