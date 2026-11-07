package ru.bulbasaur.office.domain.model;

import java.util.UUID;

/** Победитель уже закрытого сезона Bulba Jump. */
public record SeasonChampion(int seasonNumber, String login, UUID playerId, long value) {
}
