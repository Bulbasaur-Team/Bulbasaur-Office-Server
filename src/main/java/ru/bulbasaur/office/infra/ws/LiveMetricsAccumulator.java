package ru.bulbasaur.office.infra.ws;

import org.springframework.stereotype.Component;
import ru.bulbasaur.office.usecase.dto.LiveMetricsSnapshot;
import ru.bulbasaur.office.usecase.port.out.LiveMetricsPort;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory счётчики событий за текущее 5‑минутное окно.
 * Как и presence — на один процесс сервера; при рестарте окно просто начинается заново.
 */
@Component
public class LiveMetricsAccumulator implements LiveMetricsPort {

    private final AtomicInteger tennisKicks = new AtomicInteger();
    private final AtomicInteger volleyballKicks = new AtomicInteger();
    private final AtomicInteger coffeeCups = new AtomicInteger();

    @Override
    public void recordTennisKick() {
        tennisKicks.incrementAndGet();
    }

    @Override
    public void recordVolleyballKick() {
        volleyballKicks.incrementAndGet();
    }

    @Override
    public void recordCoffeeCup() {
        coffeeCups.incrementAndGet();
    }

    @Override
    public LiveMetricsSnapshot peek() {
        return new LiveMetricsSnapshot(tennisKicks.get(), volleyballKicks.get(), coffeeCups.get());
    }

    @Override
    public LiveMetricsSnapshot drain() {
        return new LiveMetricsSnapshot(
                tennisKicks.getAndSet(0),
                volleyballKicks.getAndSet(0),
                coffeeCups.getAndSet(0));
    }
}
