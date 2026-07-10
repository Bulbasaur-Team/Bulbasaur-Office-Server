package ru.bulbasaur.office.infra.rest.dto;

import java.util.List;

/** Список всех ачивок игрока плюс счётчик полученных для надписи «Получено X/Y». */
public record AchievementsResponse(List<AchievementResponse> achievements, int owned, int total) {
}
