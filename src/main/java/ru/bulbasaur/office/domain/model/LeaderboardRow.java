package ru.bulbasaur.office.domain.model;

import java.util.UUID;

/** Строка лидерборда: логин игрока, его id (для подсветки своей строки) и результат. */
public record LeaderboardRow(String login, UUID playerId, long value) {
}
