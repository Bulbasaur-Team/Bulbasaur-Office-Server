package ru.bulbasaur.office.infra.rest.dto;

import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.usecase.dto.WardrobeItemView;

public record WardrobeBuyResponse(long balance, WardrobeItemView item) {
}
