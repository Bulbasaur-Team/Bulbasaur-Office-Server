package ru.bulbasaur.office.usecase.dto;

import java.util.List;

/**
 * Представление лидерборда: топ-N и отдельно строка текущего игрока ({@code me}),
 * даже если он не попал в топ. {@code me} равно null, если у игрока ещё нет результата.
 */
public record LeaderboardView(List<LeaderboardRowView> top, LeaderboardRowView me) {
}
