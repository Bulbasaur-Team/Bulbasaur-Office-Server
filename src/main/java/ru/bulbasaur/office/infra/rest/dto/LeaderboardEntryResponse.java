package ru.bulbasaur.office.infra.rest.dto;

/** Строка лидерборда для клиента. {@code you} — подсветить как результат текущего игрока. */
public record LeaderboardEntryResponse(int rank, String login, long value, boolean you) {
}
