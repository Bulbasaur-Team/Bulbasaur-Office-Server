package ru.bulbasaur.office.usecase.dto;

import ru.bulbasaur.office.domain.model.PlayerAppearance;

import java.util.List;

public record WardrobeCatalogView(
        long bulbaCoinBalance,
        PlayerAppearance appearance,
        List<WardrobeItemView> items
) {
}
