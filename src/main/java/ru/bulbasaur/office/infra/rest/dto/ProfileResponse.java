package ru.bulbasaur.office.infra.rest.dto;

import ru.bulbasaur.office.domain.model.PlayerAppearance;

/** Профиль текущего игрока: баланс BC и надетая одежда. */
public record ProfileResponse(String login, long bulbaCoinBalance, PlayerAppearance appearance) {
}
