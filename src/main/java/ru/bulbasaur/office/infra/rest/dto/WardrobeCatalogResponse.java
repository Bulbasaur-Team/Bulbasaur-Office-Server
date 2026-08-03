package ru.bulbasaur.office.infra.rest.dto;

import ru.bulbasaur.office.domain.model.PlayerAppearance;
import ru.bulbasaur.office.usecase.dto.WardrobeItemView;

import java.util.List;

public record WardrobeCatalogResponse(
        long balance,
        PlayerAppearance appearance,
        List<WardrobeItemView> items
) {
}
