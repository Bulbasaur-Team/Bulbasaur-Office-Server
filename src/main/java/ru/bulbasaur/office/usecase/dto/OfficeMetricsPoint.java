package ru.bulbasaur.office.usecase.dto;

import java.time.Instant;

/** Одна 5‑минутная точка метрик офиса. */
public record OfficeMetricsPoint(
        Instant bucketStart,
        int onlineCount,
        int tennisKicks,
        int volleyballKicks,
        int coffeeCups
) {
}
