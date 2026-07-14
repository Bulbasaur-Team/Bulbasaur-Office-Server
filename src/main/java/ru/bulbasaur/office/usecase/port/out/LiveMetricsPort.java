package ru.bulbasaur.office.usecase.port.out;

import ru.bulbasaur.office.usecase.dto.LiveMetricsSnapshot;

/**
 * Эфемерные счётчики событий офиса за текущее окно: пинки мячей и налитый кофе.
 * Раз в 5 минут {@link #drain()} забирает накопленное для записи в БД.
 */
public interface LiveMetricsPort {

    void recordTennisKick();

    void recordVolleyballKick();

    void recordCoffeeCup();

    /** Текущие значения без сброса — для отображения незавершённого окна. */
    LiveMetricsSnapshot peek();

    /** Атомарно отдать накопленное и обнулить. */
    LiveMetricsSnapshot drain();
}
