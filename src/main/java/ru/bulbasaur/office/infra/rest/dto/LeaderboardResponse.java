package ru.bulbasaur.office.infra.rest.dto;

import java.util.List;

/** Топ игры и отдельно строка текущего игрока ({@code you}, может быть null). */
public record LeaderboardResponse(List<LeaderboardEntryResponse> entries, LeaderboardEntryResponse you) {
}
