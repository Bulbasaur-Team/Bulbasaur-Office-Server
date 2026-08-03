package ru.bulbasaur.office.usecase.dto;

import ru.bulbasaur.office.domain.model.PlayerAppearance;

public record WardrobeSellView(long bulbaCoinBalance, long refund, PlayerAppearance appearance) {
}
