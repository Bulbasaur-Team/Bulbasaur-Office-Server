package ru.bulbasaur.office.infra.persistence.connector;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.bulbasaur.office.infra.persistence.entity.OfficeMetricsEntity;
import ru.bulbasaur.office.infra.persistence.repository.OfficeMetricsJpaRepository;
import ru.bulbasaur.office.usecase.dto.OfficeMetricsPoint;
import ru.bulbasaur.office.usecase.port.out.OfficeMetricsPort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OfficeMetricsConnector implements OfficeMetricsPort {

    private final OfficeMetricsJpaRepository repository;

    @Override
    @Transactional
    public void upsert(OfficeMetricsPoint point) {
        OfficeMetricsEntity entity = repository.findById(point.bucketStart()).orElseGet(OfficeMetricsEntity::new);
        entity.setBucketStart(point.bucketStart());
        entity.setOnlineCount(point.onlineCount());
        entity.setTennisKicks(point.tennisKicks());
        entity.setVolleyballKicks(point.volleyballKicks());
        entity.setCoffeeCups(point.coffeeCups());
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OfficeMetricsPoint> since(Instant fromInclusive) {
        List<OfficeMetricsEntity> rows =
                repository.findAllByBucketStartGreaterThanEqualOrderByBucketStartAsc(fromInclusive);
        List<OfficeMetricsPoint> points = new ArrayList<>(rows.size());
        for (OfficeMetricsEntity row : rows) {
            points.add(new OfficeMetricsPoint(
                    row.getBucketStart(),
                    row.getOnlineCount(),
                    row.getTennisKicks(),
                    row.getVolleyballKicks(),
                    row.getCoffeeCups()));
        }
        return points;
    }

    @Override
    @Transactional
    public void deleteBefore(Instant cutoff) {
        repository.deleteOlderThan(cutoff);
    }
}
