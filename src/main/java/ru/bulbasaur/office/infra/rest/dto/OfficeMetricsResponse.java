package ru.bulbasaur.office.infra.rest.dto;

import java.util.List;

/** Временной ряд метрик для мониторов в комнате мониторинга. */
public record OfficeMetricsResponse(int bucketMinutes, List<OfficeMetricsPointResponse> points) {

    public record OfficeMetricsPointResponse(
            String t,
            int online,
            int tennisKicks,
            int volleyballKicks,
            int coffeeCups
    ) {
    }
}
