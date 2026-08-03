package ru.bulbasaur.office.infra.rest.dto;

import ru.bulbasaur.office.domain.model.PlayerAppearance;

public record WardrobeSellResponse(long balance, long refund, PlayerAppearance appearance) {
}
