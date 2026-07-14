package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.usecase.dto.OfficeMetricsPoint;

import java.time.Instant;
import java.util.List;

/** История метрик офиса по 5‑минутным бакетам (окно ~48 часов). */
public interface OfficeMetricsPort {

    void upsert(OfficeMetricsPoint point);

    List<OfficeMetricsPoint> since(Instant fromInclusive);

    void deleteBefore(Instant cutoff);
}
