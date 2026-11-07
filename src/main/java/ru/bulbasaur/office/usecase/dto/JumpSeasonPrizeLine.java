package ru.bulbasaur.office.usecase.dto;

/** Одно призовое место закрытого сезона Bulba Jump. */
public record JumpSeasonPrizeLine(int place, String login, long score, long coins) {
}
