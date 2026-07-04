package ru.bulbasaur.office.infra.rest.dto;

import jakarta.validation.constraints.NotNull;

public record SubmitScoreRequest(
        @NotNull Long value) {
}
