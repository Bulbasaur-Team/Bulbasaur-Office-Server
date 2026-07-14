package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.usecase.dto.LiveMetricsSnapshot;
import ru.bulbasaur.office.usecase.dto.OfficeMetricsPoint;
import ru.bulbasaur.office.usecase.port.out.LiveMetricsPort;
import ru.bulbasaur.office.usecase.port.out.OfficeMetricsPort;
import ru.bulbasaur.office.usecase.port.out.OnlinePlayersPort;

import java.time.Duration;
import java.time.Instant;

/**
 * Раз в 5 минут: снять онлайн и накопленные счётчики, записать бакет в БД,
 * подчистить историю старше 48 часов.
 */
@Service
@RequiredArgsConstructor
public class RecordOfficeMetricsTickUsecase {

    public static final Duration RETENTION = Duration.ofHours(48);
    public static final int BUCKET_SECONDS = 300;

    private final OnlinePlayersPort onlinePlayers;
    private final LiveMetricsPort liveMetrics;
    private final OfficeMetricsPort metrics;

    public void execute() {
        // Копим стрим в live → на границе окна пишем в только что завершённый бакет.
        Instant current = floorToBucket(Instant.now());
        Instant completed = current.minusSeconds(BUCKET_SECONDS);
        LiveMetricsSnapshot live = liveMetrics.drain();
        metrics.upsert(new OfficeMetricsPoint(
                completed,
                onlinePlayers.onlineLogins().size(),
                live.tennisKicks(),
                live.volleyballKicks(),
                live.coffeeCups()));
        metrics.deleteBefore(current.minus(RETENTION));
    }

    static Instant floorToBucket(Instant instant) {
        long epoch = instant.getEpochSecond();
        return Instant.ofEpochSecond(epoch - (epoch % BUCKET_SECONDS));
    }
}
