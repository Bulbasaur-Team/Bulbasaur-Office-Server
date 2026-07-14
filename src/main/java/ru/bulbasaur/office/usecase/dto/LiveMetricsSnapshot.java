package ru.bulbasaur.office.usecase.dto;

/** Накопленные счётчики за текущее 5‑минутное окно (ещё не сброшенные в БД). */
public record LiveMetricsSnapshot(int tennisKicks, int volleyballKicks, int coffeeCups) {
}
