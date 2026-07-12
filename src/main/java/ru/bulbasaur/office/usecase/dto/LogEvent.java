package ru.bulbasaur.office.usecase.dto;

import java.time.Instant;

/** Одна строка журнала событий Бульба Офиса (для «логов» на принтере дата-центра). */
public record LogEvent(Instant time, String level, String logger, String message) {
}
