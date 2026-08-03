package ru.bulbasaur.office.usecase.dto;

import ru.bulbasaur.office.domain.model.WardrobeCategory;

import java.time.Instant;

public record WardrobeItemView(
        String code,
        WardrobeCategory category,
        String name,
        long price,
        boolean sellable,
        boolean owned,
        boolean equipped,
        Instant purchasedAt
) {
}
