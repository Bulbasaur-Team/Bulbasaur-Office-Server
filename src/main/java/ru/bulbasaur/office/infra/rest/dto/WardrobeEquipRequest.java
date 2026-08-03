package ru.bulbasaur.office.infra.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record WardrobeEquipRequest(@NotBlank String category, String itemCode) {
}
