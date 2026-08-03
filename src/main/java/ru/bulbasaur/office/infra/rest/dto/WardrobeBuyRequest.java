package ru.bulbasaur.office.infra.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record WardrobeBuyRequest(@NotBlank String itemCode) {
}
