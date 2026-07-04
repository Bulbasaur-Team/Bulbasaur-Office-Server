package ru.bulbasaur.office.usecase.dto;

/** Строка лидерборда с рангом и признаком «это текущий игрок». */
public record LeaderboardRowView(int rank, String login, long value, boolean currentPlayer) {
}
