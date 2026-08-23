package ru.bulbasaur.office.infra.rest.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PresentationDeckRequest(@NotNull List<String> slideIds) {
}
