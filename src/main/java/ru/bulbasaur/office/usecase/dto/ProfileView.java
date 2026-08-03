package ru.bulbasaur.office.usecase.dto;

import ru.bulbasaur.office.domain.model.PlayerAppearance;

public record ProfileView(String login, long bulbaCoinBalance, PlayerAppearance appearance) {
}
