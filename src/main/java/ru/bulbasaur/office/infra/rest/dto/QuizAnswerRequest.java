package ru.bulbasaur.office.infra.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record QuizAnswerRequest(
        @NotNull UUID attemptId,
        @Min(-1) @Max(3) int optionIndex
) {
}
