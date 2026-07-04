package ru.bulbasaur.office.domain.model;

/**
 * Направление «лучшего» результата в лидерборде.
 * HIGHER_BETTER — больше значение лучше (очки, количество угаданных слов).
 * LOWER_BETTER  — меньше значение лучше (время, число попыток).
 */
public enum Direction {
    HIGHER_BETTER,
    LOWER_BETTER
}
