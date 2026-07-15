package ru.bulbasaur.office.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.bulbasaur.office.domain.model.Achievement;
import ru.bulbasaur.office.usecase.dto.LiveMetricsSnapshot;
import ru.bulbasaur.office.usecase.dto.OfficeMetricsPoint;
import ru.bulbasaur.office.usecase.port.out.LiveMetricsPort;
import ru.bulbasaur.office.usecase.port.out.OfficeMetricsPort;
import ru.bulbasaur.office.usecase.port.out.OnlinePlayersPort;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * История метрик за последние 48 часов плотной сеткой 5‑минутных бакетов.
 * Незавершённое текущее окно дописывается из live-счётчиков и онлайна.
 */
@Service
@RequiredArgsConstructor
public class GetOfficeMetricsUsecase {

    private static final Duration WINDOW = RecordOfficeMetricsTickUsecase.RETENTION;
    private static final int BUCKET_SECONDS = RecordOfficeMetricsTickUsecase.BUCKET_SECONDS;

    private final OfficeMetricsPort metrics;
    private final LiveMetricsPort liveMetrics;
    private final OnlinePlayersPort onlinePlayers;
    private final AchievementService achievements;

    public List<OfficeMetricsPoint> execute(UUID playerId) {
        achievements.grant(playerId, Achievement.TRADER);
        return loadMetrics();
    }

    private List<OfficeMetricsPoint> loadMetrics() {
        Instant now = Instant.now();
        Instant currentBucket = RecordOfficeMetricsTickUsecase.floorToBucket(now);
        Instant from = currentBucket.minus(WINDOW);

        Map<Instant, OfficeMetricsPoint> byBucket = new HashMap<>();
        for (OfficeMetricsPoint p : metrics.since(from)) {
            byBucket.put(p.bucketStart(), p);
        }

        LiveMetricsSnapshot live = liveMetrics.peek();
        byBucket.put(currentBucket, new OfficeMetricsPoint(
                currentBucket,
                onlinePlayers.onlineLogins().size(),
                live.tennisKicks(),
                live.volleyballKicks(),
                live.coffeeCups()));

        List<OfficeMetricsPoint> dense = new ArrayList<>();
        for (Instant t = from; !t.isAfter(currentBucket); t = t.plusSeconds(BUCKET_SECONDS)) {
            OfficeMetricsPoint point = byBucket.get(t);
            if (point != null) {
                dense.add(point);
            } else {
                dense.add(new OfficeMetricsPoint(t, 0, 0, 0, 0));
            }
        }
        return dense;
    }
}
