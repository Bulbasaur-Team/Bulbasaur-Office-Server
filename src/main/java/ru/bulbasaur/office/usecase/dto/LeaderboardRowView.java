package ru.bulbasaur.office.usecase.dto;

import lombok.Builder;

/** Строка лидерборда с рангом и признаком «это текущий игрок». */
@Builder
public record LeaderboardRowView(int rank, String login, long value, boolean currentPlayer) {
}
